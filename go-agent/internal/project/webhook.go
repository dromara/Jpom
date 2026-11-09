/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package project

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"
)

// webhookClient 复用连接执行项目 WebHooks 通知（等价 DefaultWebhookPluginImpl）。
var webhookClient = &http.Client{Timeout: 30 * time.Second}

// WebHooks 同步调用项目 WebHooks（token 为 http(s) 地址，参数以 query 形式拼接）。
//
// 等价 AbstractProjectCommander.webHooks：返回响应 body；token 为空返回空字符串。
func (r *Runtime) WebHooks(info map[string]any, eventType string, params map[string]any) string {
	token := tokenValue(info)
	if token == "" {
		return ""
	}
	query := url.Values{}
	query.Set("projectId", GetString(info, "id"))
	query.Set("projectName", GetString(info, "name"))
	query.Set("type", eventType)
	for key, value := range params {
		query.Set(key, webhookValue(value))
	}
	target, err := url.Parse(token)
	if err != nil {
		return "WebHooks error:" + err.Error()
	}
	target.RawQuery = query.Encode()
	response, err := webhookClient.Get(target.String())
	if err != nil {
		if r.Log != nil {
			r.Log.Warn("调用项目 WebHooks 失败", "id", GetString(info, "id"), "type", eventType, "error", err)
		}
		return "WebHooks error:" + err.Error()
	}
	defer func() { _ = response.Body.Close() }()
	body, _ := io.ReadAll(io.LimitReader(response.Body, 64*1024))
	result := strings.TrimSpace(string(body))
	if r.Log != nil {
		r.Log.Info("项目 WebHooks 通知完成", "id", GetString(info, "id"), "type", eventType, "body", result)
	}
	return result
}

// AsyncWebHooks 异步调用项目 WebHooks（等价 AbstractProjectCommander.asyncWebHooks）。
func (r *Runtime) AsyncWebHooks(info map[string]any, eventType string, params map[string]any) {
	if tokenValue(info) == "" {
		return
	}
	go func() {
		defer func() { _ = recover() }()
		r.WebHooks(info, eventType, params)
	}()
}

// WebhookFileChange 文件变动通知，并按 DSL 配置触发 reload（等价 fileChange 分支）。
//
// file-manage 域在文件上传/删除/编辑/重命名后调用。
func (r *Runtime) WebhookFileChange(info map[string]any, params map[string]any) {
	if params == nil {
		params = map[string]any{}
	}
	r.AsyncWebHooks(info, "fileChange", params)
	original, err := r.Manager.ResolveModel(info)
	if err != nil || GetString(original, "runMode") != RunModeDsl {
		return
	}
	config, err := parseDslConfig(original)
	if err != nil || !dslHasProcess(config, OpReload) || !dslFileChangeReload(config) {
		return
	}
	go func() {
		defer func() { _ = recover() }()
		if result, err := r.reload(info, original); err != nil {
			if r.Log != nil {
				r.Log.Warn("文件变动触发 DSL reload 失败", "id", GetString(info, "id"), "error", err)
			}
		} else if r.Log != nil {
			r.Log.Info("文件变动触发 DSL reload", "id", GetString(info, "id"), "result", result.MsgStr())
		}
	}()
}

// webhookValue 把参数值转成字符串（对象按 JSON 输出，等价 Java Convert.toStr）。
func webhookValue(value any) string {
	switch data := value.(type) {
	case string:
		return data
	case nil:
		return ""
	case []byte:
		return string(data)
	default:
		content, err := json.Marshal(value)
		if err != nil {
			return fmt.Sprint(value)
		}
		return string(content)
	}
}

// ResolveFileBackupConfig 解析项目文件备份配置（等价 ProjectFileBackupService.resolveBackupCount/Suffix/Path）。
//
// dsl 的 file.backupCount/backupSuffix/backupPath 优先，否则取代理配置 jpom.project.file-backup-*。
func (r *Runtime) ResolveFileBackupConfig(info map[string]any) (count int, suffix []string, backupPath string) {
	count = r.Options.FileBackupCount
	suffix = r.Options.FileBackupSuffix
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return count, suffix, ""
	}
	config, err := parseDslConfig(original)
	if err != nil {
		return count, suffix, ""
	}
	fileConfig := yamlMap(config["file"])
	if fileConfig == nil {
		return count, suffix, ""
	}
	if value, ok := fileConfig["backupCount"]; ok {
		switch data := value.(type) {
		case int:
			count = data
		case float64:
			count = int(data)
		case string:
			if number, err := strconv.Atoi(data); err == nil {
				count = number
			}
		}
	}
	if value, ok := fileConfig["backupSuffix"]; ok {
		if list, ok := value.([]any); ok {
			suffix = make([]string, 0, len(list))
			for _, item := range list {
				suffix = append(suffix, fmt.Sprint(item))
			}
		}
	}
	backupPath = GetString(fileConfig, "backupPath")
	return count, suffix, backupPath
}

// ProjectFileBackupDir 返回项目文件备份根目录（等价 ProjectFileBackupService.pathProject）。
//
// 默认 <data>/project_file_backup/<projectId>，dsl file.backupPath 指定时使用
// <backupPath>/<projectId>。
func (r *Runtime) ProjectFileBackupDir(info map[string]any) (string, error) {
	original, err := r.Manager.ResolveModel(info)
	if err != nil {
		return "", err
	}
	_, _, backupPath := r.ResolveFileBackupConfig(info)
	id := GetString(original, "id")
	if backupPath == "" {
		return filepath.Join(r.Options.BaseDir, "data", "project_file_backup", id), nil
	}
	return filepath.Join(backupPath, id), nil
}

// SortedKeys 返回 map 的稳定排序 key（供其他域复用）。
func SortedKeys(values map[string]any) []string {
	keys := make([]string, 0, len(values))
	for key := range values {
		keys = append(keys, key)
	}
	sort.Strings(keys)
	return keys
}
