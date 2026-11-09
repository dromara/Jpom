/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package api

import (
	"errors"
	"net/http"
	"strconv"
	"strings"
	"sync"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/sysinfo"
)

var startOnce sync.Once

// registerSystem owns /info, monitoring, whitelist, cache, workspace env,
// system config, system logs and upgrade endpoints (NodeUrl 中系统域的全部接口)。
func registerSystem(r *httpx.Router, a *app.App) {
	// 启动任务：remote_version 缓存刷新、cron（控制台日志备份/缓存刷新）、tmp 清理。
	startOnce.Do(func() { sysinfo.Start(a) })
	setUpgradeHandler(a)

	r.Handle(http.MethodPost, "/info", func(c *httpx.Context) { handleInfo(c, a) })
	r.Handle(http.MethodPost, "/get-stat-info", func(c *httpx.Context) { handleStatInfo(c, a) })
	r.Handle(http.MethodPost, "/processList", func(c *httpx.Context) { handleProcessList(c, a) })
	r.Handle(http.MethodPost, "/kill.json", func(c *httpx.Context) { handleKill(c, a) })
	r.Handle(http.MethodPost, "/disk-info", func(c *httpx.Context) {
		list, err := sysinfo.FileStoresJSON()
		if err != nil {
			c.FailCode(405, "文件系统监控异常："+err.Error())
			return
		}
		c.OK(list)
	})
	r.Handle(http.MethodPost, "/hw-disk--info", func(c *httpx.Context) {
		list, err := sysinfo.DiskStoresJSON()
		if err != nil {
			c.FailCode(405, "硬盘资源监控异常："+err.Error())
			return
		}
		c.OK(list)
	})
	r.Handle(http.MethodPost, "/network-interfaces", func(c *httpx.Context) {
		list, err := sysinfo.NetworkInterfacesJSON()
		if err != nil {
			c.FailCode(405, "网卡资源监控异常："+err.Error())
			return
		}
		c.OK(list)
	})

	// 系统配置
	r.Handle(http.MethodPost, "/system/getConfig.json", func(c *httpx.Context) {
		data, err := sysinfo.SystemConfigContent(a)
		if err != nil {
			c.Error(err)
			return
		}
		c.OK(data)
	})
	r.Handle(http.MethodPost, "/system/save_config.json", func(c *httpx.Context) {
		msg, err := sysinfo.SaveSystemConfig(a, c.Form("content"), c.Form("restart"))
		if err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg(msg, nil)
	})

	// 白名单
	r.Handle(http.MethodPost, "/system/whitelistDirectory_data", func(c *httpx.Context) {
		c.OK(sysinfo.WhitelistData(a))
	})
	r.Handle(http.MethodPost, "/system/whitelistDirectory_submit", func(c *httpx.Context) {
		if err := sysinfo.SaveWhitelist(a, c.Form("project"), c.Form("allowEditSuffix")); err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg("保存成功", nil)
	})

	// 工作空间环境变量
	r.Handle(http.MethodPost, "/system/workspace_env/update", func(c *httpx.Context) {
		var privacy *int
		if value := strings.TrimSpace(c.Form("privacy")); value != "" {
			parsed, err := strconv.Atoi(value)
			if err != nil {
				c.FailCode(405, "参数验证失败")
				return
			}
			privacy = &parsed
		}
		err := sysinfo.UpdateWorkspaceEnv(a, c.Workspace, c.Form("name"), c.Form("value"), c.Form("description"), privacy)
		if err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg("更新成功", nil)
	})
	r.Handle(http.MethodPost, "/system/workspace_env/delete", func(c *httpx.Context) {
		name := c.Form("name")
		if strings.TrimSpace(name) == "" {
			c.FailCode(405, "参数验证失败")
			return
		}
		if err := sysinfo.DeleteWorkspaceEnv(a, c.Workspace, name); err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg("删除成功", nil)
	})

	// 缓存
	r.Handle(http.MethodPost, "/system/cache", func(c *httpx.Context) {
		c.OK(sysinfo.CacheJSON(a, c.Workspace))
	})
	r.Handle(http.MethodPost, "/system/clearCache", func(c *httpx.Context) {
		cacheType := strings.TrimSpace(c.Form("type"))
		if cacheType == "" {
			c.FailCode(405, "类型错误")
			return
		}
		if err := sysinfo.ClearCache(a, cacheType); err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg("清空成功", nil)
	})

	// 系统日志
	r.Handle(http.MethodPost, "/system/log_data.json", func(c *httpx.Context) {
		data, err := sysinfo.LogTreeData(a)
		if err != nil {
			c.Error(err)
			return
		}
		c.OK(data)
	})
	r.Handle(http.MethodPost, "/system/log_del.json", func(c *httpx.Context) {
		if err := sysinfo.DeleteLog(a, c.Form("path")); err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg("删除成功", nil)
	})
	r.Handle(http.MethodGet, "/system/log_download", func(c *httpx.Context) {
		path, err := sysinfo.LogDownloadPath(a, c.Form("path"))
		if err != nil {
			// 与 Java 一致：文件不存在时 HTTP 200 且 body 为空。
			return
		}
		if err := c.ServeFile(path, ""); err != nil {
			a.Log().Warn("下载系统日志失败", "path", path, "error", err)
		}
	})

	// 版本与在线升级
	r.Handle(http.MethodPost, "/system/check_version.json", func(c *httpx.Context) {
		remote, err := sysinfo.LoadRemoteInfo(a)
		if err != nil {
			a.Log().Warn("获取远程版本信息失败", "error", err)
			remote = sysinfo.CacheInfo(a)
		}
		if remote == nil {
			c.OK(nil)
			return
		}
		c.OK(normalizeRemoteVersion(remote))
	})
	r.Handle(http.MethodPost, "/system/change_log", func(c *httpx.Context) {
		c.OK(sysinfo.ChangeLog(a, parseBoolValue(c.Form("beta"))))
	})
	r.Handle(http.MethodPost, "/system/remote_upgrade.json", func(c *httpx.Context) {
		msg, err := sysinfo.RemoteUpgrade(a)
		if err != nil {
			writeError(c, err)
			return
		}
		c.OKMsg(msg, nil)
	})
	r.Handle(http.MethodPost, "/system/upload-jar-sharding", func(c *httpx.Context) {
		handleUploadJarSharding(c, a)
	})
	r.Handle(http.MethodPost, "/system/upload-jar-sharding-merge", func(c *httpx.Context) {
		totalSlice, err := strconv.Atoi(strings.TrimSpace(c.Form("totalSlice")))
		if err != nil {
			c.FailCode(405, "上传信息不完整：totalSlice")
			return
		}
		msg, mergeErr := sysinfo.MergeJarSharding(a, c.Form("sliceId"), totalSlice, c.Form("fileSumMd5"))
		if mergeErr != nil {
			writeError(c, mergeErr)
			return
		}
		c.OKMsg(msg, nil)
	})
}

// setUpgradeHandler 把平台升级域的实现注册给 /node_update（script-ws 的 ws.Manager）。
// registerSystem 在 registerWS 之前执行，runtimeFor 会缓存同一个 ws.Manager 实例。
func setUpgradeHandler(a *app.App) {
	runtimeFor(a).ws.Upgrade = upgradeHandler{app: a}
}

// upgradeHandler 实现 internal/ws 的 UpgradeHandler：安装 /node_update 上传的
// 升级包并延迟重启（Linux 使用 exec 原地重启，Windows 拉起新进程后退出）。
type upgradeHandler struct{ app *app.App }

// Restart 安装升级包并返回给服务端的提示语。
func (h upgradeHandler) Restart(uploadedFile string) (string, error) {
	return sysinfo.InstallUploadedPackage(h.app, uploadedFile)
}

// handleInfo 对应 POST /info。
func handleInfo(c *httpx.Context, a *app.App) {
	data := map[string]any{
		"manifest":        a.Manifest(),
		"pluginSize":      0,
		"joinBetaRelease": sysinfo.JoinBetaRelease(),
	}
	if remote := sysinfo.CacheInfo(a); remote != nil {
		data["remoteVersion"] = normalizeRemoteVersion(remote)
	}
	if monitor := a.Config.Jpom.Monitor; monitor != nil {
		network := map[string]any{}
		if value := monitor.Network.StatExcludeNames.String(); value != "" {
			network["statExcludeNames"] = value
		}
		if value := monitor.Network.StatContainsOnlyNames.String(); value != "" {
			network["statContainsOnlyNames"] = value
		}
		data["monitor"] = map[string]any{"network": network}
	}
	c.OK(data)
}

// normalizeRemoteVersion 把缓存文件中的 Long 字段转换成字符串（Jackson 语义），
// 并复用 sysinfo.CanonicalRemoteVersion 裁剪为 Java RemoteVersion 的字段集合
// （缓存写入与 HTTP 输出共用同一份字段白名单）。
func normalizeRemoteVersion(value map[string]any) map[string]any {
	result := sysinfo.CanonicalRemoteVersion(value)
	if lastTime, ok := result["lastTime"]; ok {
		switch typed := lastTime.(type) {
		case float64:
			result["lastTime"] = strconv.FormatInt(int64(typed), 10)
		case int64:
			result["lastTime"] = strconv.FormatInt(typed, 10)
		case int:
			result["lastTime"] = strconv.Itoa(typed)
		case string:
			result["lastTime"] = typed
		}
	}
	return result
}

// handleStatInfo 对应 POST /get-stat-info：oshi 采集异常时返回 oshiError 而不是 5xx。
func handleStatInfo(c *httpx.Context, a *app.App) {
	data := map[string]any{}
	collectMonitor(a, data)
	data["jpomInfo"] = sysinfo.JpomInfo(a)
	c.OK(data)
}

func collectMonitor(a *app.App, data map[string]any) {
	defer func() {
		if recovered := recover(); recovered != nil {
			data["oshiError"] = errString(recovered)
		}
	}()
	status, err := sysinfo.SimpleStatus(a.Config.Jpom.Monitor)
	if err != nil {
		data["oshiError"] = err.Error()
		return
	}
	data["simpleStatus"] = status
	data["systemSleep"] = sysinfo.SystemSleep
	systemInfo, err := sysinfo.SystemInfo()
	if err != nil {
		data["oshiError"] = err.Error()
		return
	}
	data["systemInfo"] = systemInfo
}

func errString(value any) string {
	if err, ok := value.(error); ok {
		return err.Error()
	}
	if text, ok := value.(string); ok {
		return text
	}
	return "系统监控异常"
}

// handleProcessList 对应 POST /processList。
func handleProcessList(c *httpx.Context, a *app.App) {
	processName := c.FormDefault("processName", "java")
	count := c.FormInt("count", 20)
	list, err := sysinfo.ProcessListJSON(processName, count)
	if err != nil {
		c.FailCode(405, "系统进程监控异常："+err.Error())
		return
	}
	c.OK(list)
}

// handleKill 对应 POST /kill.json。
func handleKill(c *httpx.Context, a *app.App) {
	pid := c.FormInt("pid", 0)
	if pid <= 0 {
		// 防止 kill 0/负数 误伤进程组（Java 端未做该保护，这里按参数错误处理）。
		c.FailCode(405, "参数验证失败")
		return
	}
	if pid == a.Pid {
		c.FailCode(405, "不支持在线关闭 Agent 进程")
		return
	}
	result, err := sysinfo.KillProcess(pid)
	if err != nil {
		c.Fail("kill 进程失败：" + err.Error())
		return
	}
	if strings.TrimSpace(result) == "" {
		result = "成功kill"
	}
	c.OKMsg(result, nil)
}

// handleUploadJarSharding 对应 POST /system/upload-jar-sharding（multipart）。
func handleUploadJarSharding(c *httpx.Context, a *app.App) {
	if c.R.MultipartForm == nil {
		if err := c.R.ParseMultipartForm(64 << 20); err != nil {
			c.FailCode(405, "没有上传文件")
			return
		}
	}
	file, header, err := c.R.FormFile("file")
	if err != nil {
		// 兼容字段名不是 file 的情况：取第一个上传文件。
		for _, headers := range c.R.MultipartForm.File {
			if len(headers) > 0 {
				header = headers[0]
				break
			}
		}
		if header == nil {
			c.FailCode(405, "没有上传文件")
			return
		}
		file, err = header.Open()
		if err != nil {
			c.FailCode(405, "没有上传文件")
			return
		}
	}
	defer func() { _ = file.Close() }()
	totalSlice, err := strconv.Atoi(strings.TrimSpace(c.Form("totalSlice")))
	if err != nil {
		c.FailCode(405, "上传信息不完整：totalSlice")
		return
	}
	nowSlice, err := strconv.Atoi(strings.TrimSpace(c.Form("nowSlice")))
	if err != nil {
		c.FailCode(405, "上传信息不完整：nowSlice")
		return
	}
	err = sysinfo.SaveJarSharding(a, header.Filename, file, c.Form("sliceId"), totalSlice, nowSlice, c.Form("fileSumMd5"))
	if err != nil {
		writeError(c, err)
		return
	}
	c.OKMsg("上传成功", nil)
}

// writeError 把 ParamError 转成对应响应码，其它错误按 500 处理。
func writeError(c *httpx.Context, err error) {
	var paramError *sysinfo.ParamError
	if errors.As(err, &paramError) {
		c.FailCode(paramError.Code, paramError.Msg)
		return
	}
	c.Fail(err.Error())
}

func parseBoolValue(value string) bool {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "true", "1", "yes", "y", "on":
		return true
	default:
		return false
	}
}
