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
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"strings"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/proc"
	"jpom.io/go-agent/internal/project"
	"jpom.io/go-agent/internal/sysinfo"
)

// registerProject 注册 /manage/* 项目生命周期、状态与日志端点。
func registerProject(r *httpx.Router, a *app.App) {
	runtime := project.NewRuntime(a.BaseDir, a.Config, a.Log())
	// 项目后台任务：autoStart 项目启动
	runtime.StartBackgroundTasks()
	// 控制台日志自动备份注册到 sysinfo 统一调度器（/system/cache.taskList 与 Java 一致）
	if cron := runtime.Options.ProjectLog.AutoBackupConsoleCron; strings.TrimSpace(cron) != "" {
		if err := sysinfo.RegisterCronTask("auto_back_log", cron, runtime.AutoBackupConsoleLogs); err != nil {
			a.Log().Warn("注册控制台日志自动备份任务失败", "error", err)
		}
	}

	r.Handle(http.MethodPost, "/manage/getProjectItem", func(c *httpx.Context) { projectGetItem(c, runtime) })
	r.Handle(http.MethodPost, "/manage/getProjectInfo", func(c *httpx.Context) { projectGetInfo(c, runtime) })
	r.Handle(http.MethodPost, "/manage/saveProject", func(c *httpx.Context) { projectSave(c, runtime) })
	r.Handle(http.MethodPost, "/manage/deleteProject", func(c *httpx.Context) { projectDelete(c, runtime) })
	r.Handle(http.MethodPost, "/manage/getProjectStatus", func(c *httpx.Context) { projectStatus(c, runtime) })
	r.Handle(http.MethodPost, "/manage/getProjectPort", func(c *httpx.Context) { projectPort(c, runtime) })
	r.Handle(http.MethodPost, "/manage/operate", func(c *httpx.Context) { projectOperate(c, runtime) })
	r.Handle(http.MethodPost, "/manage/releaseOutGiving", func(c *httpx.Context) { projectReleaseOutGiving(c, runtime) })
	r.Handle(http.MethodPost, "/manage/change-workspace-id", func(c *httpx.Context) { projectChangeWorkspaceID(c, runtime) })

	r.Handle(http.MethodPost, "/manage/log/logSize", func(c *httpx.Context) { projectLogSize(c, runtime) })
	r.Handle(http.MethodPost, "/manage/log/resetLog", func(c *httpx.Context) { projectResetLog(c, runtime) })
	r.Handle(http.MethodPost, "/manage/log/logBack", func(c *httpx.Context) { projectLogBack(c, runtime) })
	r.Handle(http.MethodPost, "/manage/log/logBack_delete", func(c *httpx.Context) { projectLogBackDelete(c, runtime) })
	r.Handle(http.MethodGet, "/manage/log/logBack_download", func(c *httpx.Context) { projectLogBackDownload(c, runtime) })
	r.Handle(http.MethodGet, "/manage/log/export", func(c *httpx.Context) { projectLogExport(c, runtime) })
}

// projectForm 统一读取 form-urlencoded / JSON 请求参数。
//
// 服务端 NodeForward 默认以 form-urlencoded 转发（与 Spring MVC 参数绑定一致），
// 这里同时兼容 JSON body，便于 curl 自测。
type projectForm struct {
	values  map[string]any
	present map[string]bool
}

func readProjectForm(c *httpx.Context) *projectForm {
	form := &projectForm{values: map[string]any{}, present: map[string]bool{}}
	contentType := strings.ToLower(c.R.Header.Get("Content-Type"))
	if strings.Contains(contentType, "application/json") {
		var body map[string]any
		if err := c.BindJSON(&body); err == nil && body != nil {
			for key, value := range body {
				form.values[key] = value
				form.present[key] = true
			}
		}
		return form
	}
	_ = c.R.ParseForm()
	for key, values := range c.R.Form {
		if len(values) == 0 {
			continue
		}
		form.values[key] = values[len(values)-1]
		form.present[key] = true
	}
	return form
}

func (f *projectForm) string(key string) string {
	value, ok := f.values[key]
	if !ok || value == nil {
		return ""
	}
	switch data := value.(type) {
	case string:
		return data
	case bool:
		return strconv.FormatBool(data)
	case json.Number:
		return data.String()
	case float64:
		return strconv.FormatFloat(data, 'f', -1, 64)
	default:
		return fmt.Sprint(data)
	}
}

func (f *projectForm) has(key string) bool {
	_, ok := f.values[key]
	return ok
}

// boolValue 按 Java Boolean.parseBoolean 语义解析（忽略大小写，只有 true 为真）。
func (f *projectForm) boolValue(key string) bool {
	return strings.EqualFold(strings.TrimSpace(f.string(key)), "true")
}

// convertBoolValue 按 hutool Convert.toBool 语义解析（true/1/yes/y/on 为真）。
func (f *projectForm) convertBoolValue(key string) bool {
	switch strings.ToLower(strings.TrimSpace(f.string(key))) {
	case "true", "1", "yes", "y", "on", "是", "真":
		return true
	default:
		return false
	}
}

// projectWorkspace 返回 workspaceId 请求头原值（Java 端不做 trim/默认值）。
func projectWorkspace(c *httpx.Context) string {
	return c.R.Header.Get("workspaceId")
}

// projectUser 解析服务端操作人：URL 解码，空值返回 "-"（等价 BaseAgentController.getUserName）。
func projectUser(c *httpx.Context) string {
	raw := c.R.Header.Get("Jpom-Server-UserName")
	if raw == "" {
		return "-"
	}
	if decoded, err := url.PathUnescape(raw); err == nil {
		return decoded
	}
	return raw
}

// respondOK 输出成功响应；data 为 nil 时不输出 data 字段（Jackson NON_NULL 语义）。
func respondOK(c *httpx.Context, msg string, data any) {
	respondCode(c, 200, msg, data)
}

// respondCode 输出 JsonMessage；data 为 nil 时省略 data 字段。
func respondCode(c *httpx.Context, code int, msg string, data any) {
	c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
	c.W.WriteHeader(http.StatusOK)
	payload := map[string]any{"code": code, "msg": msg}
	if data != nil {
		payload["data"] = data
	}
	encoder := json.NewEncoder(c.W)
	encoder.SetEscapeHTML(false)
	_ = encoder.Encode(payload)
}

// fail405 输出参数/状态异常（Java IllegalArgumentException/IllegalStateException/ValidateException）。
func fail405(c *httpx.Context, msg string) {
	respondCode(c, 405, msg, nil)
}

// fail500 输出运行期异常（Java RuntimeException，msg 前缀为 服务异常：）。
func fail500(c *httpx.Context, msg string) {
	respondCode(c, 500, "服务异常："+msg, nil)
}

// lookupProject 读取项目，不存在时返回 nil。
func lookupProject(runtime *project.Runtime, id string) map[string]any {
	info, ok, err := runtime.Manager.Get(id)
	if err != nil || !ok {
		return nil
	}
	return info
}

// projectGetItem 获取单个项目信息（等价 ProjectListController.getProjectItem）。
func projectGetItem(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := lookupProject(runtime, form.string("id"))
	if info == nil {
		respondOK(c, "", nil)
		return
	}
	result := project.Clone(info)
	runMode := project.GetString(info, "runMode")
	if runMode != project.RunModeDsl && runMode != project.RunModeFile {
		if command := runtime.BuildRunCommand(info); command != "" {
			result["runCommand"] = command
		}
	}
	if runMode == project.RunModeDsl {
		if canReload, list, err := runtime.DslProcessInfo(info); err == nil {
			result["canReload"] = canReload
			result["dslProcessInfo"] = list
		}
	}
	respondOK(c, "", result)
}

// projectGetInfo 获取全部项目信息（等价 ProjectListController.getProjectInfo）。
func projectGetInfo(c *httpx.Context, runtime *project.Runtime) {
	list, err := runtime.Manager.List()
	if err != nil {
		fail405(c, err.Error())
		return
	}
	respondOK(c, "", list)
}

// projectSave 保存项目（等价 ManageEditProjectController.saveProject）。
func projectSave(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	msg, err := runtime.SaveProject(project.SaveParams{
		Values:      form.values,
		Preview:     form.convertBoolValue("previewData"),
		OutGiving:   form.boolValue("outGivingProject"),
		WorkspaceID: projectWorkspace(c),
		UserName:    projectUser(c),
	})
	if err != nil {
		fail405(c, err.Error())
		return
	}
	respondOK(c, msg, nil)
}

// projectDelete 删除项目（等价 ManageEditProjectController.deleteProject）。
func projectDelete(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	id := form.string("id")
	if strings.TrimSpace(id) == "" {
		fail405(c, "项目id 不正确")
		return
	}
	msg, err := runtime.DeleteProject(id, form.string("thorough"))
	if err != nil {
		fail405(c, err.Error())
		return
	}
	respondOK(c, msg, nil)
}

// projectStatus 获取项目进程状态（等价 ProjectStatusController.getProjectStatus）。
func projectStatus(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	id := form.string("id")
	if strings.TrimSpace(id) == "" {
		fail405(c, "项目id 不正确")
		return
	}
	info := lookupProject(runtime, id)
	if info == nil {
		fail405(c, "项目id不存在")
		return
	}
	result, err := runtime.Operate(project.OpStatus, info)
	if err != nil {
		fail405(c, err.Error())
		return
	}
	data := map[string]any{}
	if result.Pid != nil {
		data["pId"] = *result.Pid
	}
	if len(result.Pids) > 0 {
		data["pIds"] = result.Pids
	}
	if result.StatusMsg != nil {
		data["statusMsg"] = *result.StatusMsg
	}
	respondOK(c, "", data)
}

// projectPort 获取项目运行端口（等价 ProjectStatusController.getProjectPort）。
func projectPort(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	idsText := form.string("ids")
	if strings.TrimSpace(idsText) == "" {
		fail405(c, "没有要获取的信息")
		return
	}
	var ids []string
	if err := json.Unmarshal([]byte(idsText), &ids); err != nil {
		fail405(c, "没有要获取的信息")
		return
	}
	result := map[string]any{}
	for _, id := range ids {
		item := map[string]any{}
		info := lookupProject(runtime, id)
		if info == nil {
			item["error"] = "项目不存在:" + id
			result[id] = item
			continue
		}
		item["name"] = project.GetString(info, "name")
		status, err := runtime.Operate(project.OpStatus, info)
		if err != nil {
			item["error"] = err.Error()
			result[id] = item
			continue
		}
		pid := 0
		if status.Pid != nil {
			item["pid"] = *status.Pid
			pid = *status.Pid
		}
		if len(status.Pids) > 0 {
			item["pids"] = status.Pids
		}
		if status.StatusMsg != nil {
			item["statusMsg"] = *status.StatusMsg
		}
		if status.Ports != nil && *status.Ports != "" {
			item["port"] = *status.Ports
		} else {
			item["port"] = proc.MainPort(pid)
		}
		result[id] = item
	}
	respondOK(c, "", result)
}

// projectOperate 执行项目命令（等价 ProjectStatusController.operate）。
func projectOperate(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	id := form.string("id")
	if strings.TrimSpace(id) == "" {
		fail405(c, "项目id 不正确")
		return
	}
	op := strings.TrimSpace(form.string("opt"))
	if op == "" {
		fail405(c, "操作参数缺失")
		return
	}
	info := lookupProject(runtime, id)
	if info == nil {
		fail405(c, "没有找到对应的项目")
		return
	}
	if !project.CanOpt(op) {
		fail405(c, "不支持当前操作："+op)
		return
	}
	result, err := runtime.Operate(op, info)
	if err != nil {
		fail405(c, err.Error())
		return
	}
	code := 200
	msg := "操作成功"
	if !result.Success {
		code = 201
		msg = "操作失败：" + result.MsgStr()
	}
	respondCode(c, code, msg, result.JSON())
}

// projectReleaseOutGiving 释放节点分发（等价 /manage/releaseOutGiving）。
func projectReleaseOutGiving(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	msg, err := runtime.ReleaseOutGiving(form.string("id"), projectUser(c))
	if err != nil {
		fail405(c, err.Error())
		return
	}
	respondOK(c, msg, nil)
}

// projectChangeWorkspaceID 修改项目工作空间（等价 /manage/change-workspace-id）。
func projectChangeWorkspaceID(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	msg, err := runtime.ChangeWorkspaceID(form.string("id"), form.string("newWorkspaceId"), form.string("newNodeId"), projectUser(c))
	if err != nil {
		fail405(c, err.Error())
		return
	}
	respondOK(c, msg, nil)
}

// requireProject 读取项目，不存在时写错误响应。
func requireProject(c *httpx.Context, runtime *project.Runtime, id string) map[string]any {
	info := lookupProject(runtime, id)
	if info == nil {
		fail405(c, "获取项目信息失败:"+id)
		return nil
	}
	return info
}

// projectLogSize 查看控制台日志大小（等价 ProjectLogBackController.logSize）。
func projectLogSize(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := requireProject(c, runtime, form.string("id"))
	if info == nil {
		return
	}
	logBack, size := runtime.LogSize(info)
	data := map[string]any{"logBack": logBack}
	if size != "" {
		data["logSize"] = size
	}
	respondOK(c, "", data)
}

// projectResetLog 重置控制台日志（等价 ProjectLogBackController.resetLog）。
func projectResetLog(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := requireProject(c, runtime, form.string("id"))
	if info == nil {
		return
	}
	msg, code := runtime.ResetLog(info)
	respondCode(c, code, msg, nil)
}

// projectLogBack 查看控制台日志备份列表（等价 ProjectLogBackController.console）。
func projectLogBack(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := requireProject(c, runtime, form.string("id"))
	if info == nil {
		return
	}
	respondOK(c, "", runtime.LogBackList(info))
}

// projectLogBackDelete 删除日志备份（等价 ProjectLogBackController.clear）。
func projectLogBackDelete(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := requireProject(c, runtime, form.string("id"))
	if info == nil {
		return
	}
	name := form.string("name")
	if strings.TrimSpace(name) == "" {
		fail405(c, "没有对应到文件")
		return
	}
	if err := runtime.LogBackDelete(info, name); err != nil {
		fail500(c, err.Error())
		return
	}
	respondOK(c, "删除成功", nil)
}

// projectLogBackDownload 下载日志备份（等价 ProjectLogBackController.download）。
func projectLogBackDownload(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := requireProject(c, runtime, form.string("id"))
	if info == nil {
		return
	}
	file, err := runtime.LogBackDownloadFile(info, form.string("key"))
	if err != nil {
		writeRawJSON(c, 400, err.Error())
		return
	}
	serveFile(c, file)
}

// projectLogExport 导出控制台日志（等价 ProjectLogBackController.export）。
func projectLogExport(c *httpx.Context, runtime *project.Runtime) {
	form := readProjectForm(c)
	info := requireProject(c, runtime, form.string("id"))
	if info == nil {
		return
	}
	file, err := runtime.ExportLogFile(info)
	if err != nil {
		writeRawJSON(c, 400, err.Error())
		return
	}
	serveFile(c, file)
}

// writeRawJSON 输出裸 JSON（下载失败时的 Java ServletUtil.write 行为）。
func writeRawJSON(c *httpx.Context, code int, msg string) {
	c.W.Header().Set("Content-Type", "application/json;charset=UTF-8")
	c.W.WriteHeader(http.StatusOK)
	encoder := json.NewEncoder(c.W)
	encoder.SetEscapeHTML(false)
	_ = encoder.Encode(map[string]any{"code": code, "msg": msg})
}

// serveFile 以附件形式输出文件。
func serveFile(c *httpx.Context, path string) {
	file, err := os.Open(path)
	if err != nil {
		writeRawJSON(c, 400, "下载失败。请刷新页面后重试"+err.Error())
		return
	}
	defer func() { _ = file.Close() }()
	stat, err := file.Stat()
	if err != nil || stat.IsDir() {
		writeRawJSON(c, 400, "下载失败。请刷新页面后重试文件不存在")
		return
	}
	c.W.Header().Set("Content-Type", "application/octet-stream")
	c.W.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%q", stat.Name()))
	http.ServeContent(c.W, c.R, stat.Name(), stat.ModTime(), file)
}
