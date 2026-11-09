/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package sysinfo

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"jpom.io/go-agent/internal/app"
)

// Java 端 AbstractProjectCommander.PID_PORT 缓存：进程 id -> 监听端口。
var pidPortCache sync.Map

// pidPortCacheSize 暴露给 /system/cache 的 pidPort 字段。
func pidPortCacheSize() int {
	count := 0
	pidPortCache.Range(func(_, _ any) bool {
		count++
		return true
	})
	return count
}

// PortOfProcess 返回进程监听端口（带缓存，对应 Java 的 PID_PORT）。
func PortOfProcess(pid int) string {
	if cached, ok := pidPortCache.Load(pid); ok {
		if value, ok := cached.(string); ok {
			return value
		}
	}
	value := listeningPorts(pid)
	pidPortCache.Store(pid, value)
	return value
}

// ClearPidPortCache 清空进程端口缓存（/system/clearCache type=pidPort）。
func ClearPidPortCache() { pidPortCache = sync.Map{} }

// fileTailLineCount 对应 AgentFileTailWatcher.getOneLineCount()，由 WebSocket
// 日志读取模块维护，默认 0。
var fileTailLineCount int64

// SetFileTailLineCount 由日志 tail 模块更新当前缓存行数。
func SetFileTailLineCount(value int64) { atomic.StoreInt64(&fileTailLineCount, value) }

// plainFileSizeCache 记录 data/temp 与 oldJars 的缓存大小（Java 端每 10 分钟刷新）。
type sizeCache struct {
	temp    int64
	oldJars int64
	data    int64
}

var cacheSizes atomic.Value

func refreshCacheSizes(a *app.App) sizeCache {
	sizes := sizeCache{
		temp:    app.DirSize(a.TempRoot()),
		oldJars: app.DirSize(a.OldJarsPath()),
		data:    a.DataSize(),
	}
	cacheSizes.Store(sizes)
	return sizes
}

func currentCacheSizes(a *app.App) sizeCache {
	if value := cacheSizes.Load(); value != nil {
		if sizes, ok := value.(sizeCache); ok {
			return sizes
		}
	}
	return refreshCacheSizes(a)
}

// AgentPluginSize 是 /info 与 /system/cache 的 pluginSize 值。
// 与 Java 插件端保持一致（Java PluginFactory.size() 实测为 1：内置 webhook 插件；
// Server 仅展示该字段，不做逻辑判断）。
const AgentPluginSize = 1

// CacheJSON 对应 /system/cache 的响应 data。
func CacheJSON(a *app.App, workspaceID string) map[string]any {
	sizes := currentCacheSizes(a)
	result := map[string]any{
		"fileSize":            strconv.FormatInt(sizes.temp, 10),
		"dataSize":            strconv.FormatInt(sizes.data, 10),
		"oldJarsSize":         strconv.FormatInt(sizes.oldJars, 10),
		"pidPort":             pidPortCacheSize(),
		"readFileOnLineCount": int(atomic.LoadInt64(&fileTailLineCount)),
		"taskList":            CronList(),
		"pluginSize":          AgentPluginSize,
		"dateTime":            time.Now().Format("2006-01-02 15:04:05"),
		"timeZoneId":          timeZoneID(),
		"scriptExecLogSize":   scriptExecLogSize(a),
		"timerMatchSecond":    a.Config.Jpom.System.TimerMatchSecond,
	}
	if keys := WorkspaceEnvKeys(a, workspaceID); len(keys) > 0 {
		result["envVarKeys"] = keys
	}
	if tags := scriptLibraryTagMap(a); len(tags) > 0 {
		result["scriptLibraryTagMap"] = tags
	}
	return result
}

// ClearCache 对应 /system/clearCache。
func ClearCache(a *app.App, cacheType string) error {
	switch cacheType {
	case "pidPort":
		ClearPidPortCache()
	case "oldJarsSize":
		if err := os.RemoveAll(a.OldJarsPath()); err != nil {
			return paramErr(405, "i18n.clear_old_version_package_failed.021c")
		}
	case "fileSize":
		// Java 仅清空 data/temp 下的内容，保留目录本身（E2E 实测）。
		if err := clearDirectoryContents(a.TempRoot()); err != nil {
			return paramErr(405, "i18n.clear_file_cache_failed.5cd1")
		}
	default:
		return paramErrAppend(405, "i18n.no_type_specified.8c65", cacheType)
	}
	refreshCacheSizes(a)
	return nil
}

// clearDirectoryContents 删除目录下的所有内容但保留目录本身。
func clearDirectoryContents(path string) error {
	if err := os.MkdirAll(path, 0o755); err != nil {
		return err
	}
	entries, err := os.ReadDir(path)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		if err := os.RemoveAll(filepath.Join(path, entry.Name())); err != nil {
			return err
		}
	}
	return nil
}

// scriptLibraryTagMap 读取 data/global-script/*.json，返回 脚本库 tag -> version。
func scriptLibraryTagMap(a *app.App) map[string]string {
	entries, err := os.ReadDir(a.GlobalScriptDir)
	if err != nil {
		return nil
	}
	result := map[string]string{}
	for _, entry := range entries {
		if entry.IsDir() || filepath.Ext(entry.Name()) != ".json" {
			continue
		}
		content, err := os.ReadFile(filepath.Join(a.GlobalScriptDir, entry.Name()))
		if err != nil {
			continue
		}
		var value map[string]any
		if err := json.Unmarshal(content, &value); err != nil {
			continue
		}
		tag := filepath.Base(entry.Name()[:len(entry.Name())-len(".json")])
		version := ""
		if raw, ok := value["version"]; ok && raw != nil {
			version = toString(raw)
		}
		result[tag] = version
	}
	return result
}

// scriptExecLogSize 对应 NodeScriptExecLogServer.size()（data/script_log.json 条数）。
func scriptExecLogSize(a *app.App) int {
	content, err := os.ReadFile(filepath.Join(a.DataDir, "script_log.json"))
	if err != nil {
		return 0
	}
	var values map[string]json.RawMessage
	if err := json.Unmarshal(content, &values); err != nil {
		return 0
	}
	return len(values)
}

// timeZoneID 近似 Java TimeZone.getDefault().getID()：优先 TZ 环境变量，其次
// /etc/localtime 指向的 IANA 名称、/etc/timezone，最后回退本地时区名。
func timeZoneID() string {
	if value := os.Getenv("TZ"); value != "" {
		return value
	}
	if target, err := os.Readlink("/etc/localtime"); err == nil {
		if index := strings.Index(target, "zoneinfo/"); index >= 0 {
			return target[index+len("zoneinfo/"):]
		}
	}
	if content, err := os.ReadFile("/etc/timezone"); err == nil {
		if value := trimSpace(string(content)); value != "" {
			return value
		}
	}
	if name := time.Now().Location().String(); name != "" && name != "Local" {
		return name
	}
	return "Local"
}
