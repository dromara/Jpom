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
	"errors"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

func asParamError(t *testing.T, err error) *ParamError {
	t.Helper()
	var paramError *ParamError
	if !errors.As(err, &paramError) {
		t.Fatalf("expected ParamError, got %T (%v)", err, err)
	}
	return paramError
}

func TestWhitelistSubmitAndData(t *testing.T) {
	a := newTestApp(t)
	projectDir := filepath.Join(t.TempDir(), "deploy")
	projectDir2 := filepath.Join(t.TempDir(), "logs")
	if err := os.MkdirAll(projectDir, 0o755); err != nil {
		t.Fatal(err)
	}
	// 相对路径 -> 405 需要配置绝对路径
	if err := SaveWhitelist(a, "relative/path", ""); err == nil {
		t.Fatal("relative path should be rejected")
	} else if paramError := asParamError(t, err); paramError.Code != 405 {
		t.Errorf("code = %d", paramError.Code)
	}
	// 根路径 -> 405
	if err := SaveWhitelist(a, "/", ""); err == nil {
		t.Fatal("root path should be rejected")
	}
	// 正常保存
	value := projectDir + "\n" + projectDir2
	if err := SaveWhitelist(a, value, "txt\nlog@UTF-8"); err != nil {
		t.Fatalf("SaveWhitelist: %v", err)
	}
	data := WhitelistData(a)
	projects, _ := data["project"].([]string)
	if len(projects) != 2 {
		t.Errorf("project = %v", data["project"])
	}
	suffixes, _ := data["allowEditSuffix"].([]string)
	if len(suffixes) != 2 || suffixes[1] != "log@UTF-8" {
		t.Errorf("allowEditSuffix = %v", data["allowEditSuffix"])
	}
	// 包含关系 -> 405 授权目录中不能存在包含关系
	containment := projectDir + "\n" + filepath.Join(projectDir, "sub")
	if err := SaveWhitelist(a, containment, ""); err == nil {
		t.Fatal("nested directories should be rejected")
	} else if !strings.Contains(err.Error(), "包含关系") {
		t.Errorf("unexpected error: %v", err)
	}
	// 非法编码名 -> 405
	if err := SaveWhitelist(a, projectDir, "log@GBK;bad"); err == nil {
		t.Fatal("illegal charset should be rejected")
	}
}

func TestWorkspaceEnvCRUD(t *testing.T) {
	a := newTestApp(t)
	privacy := 1
	if err := UpdateWorkspaceEnv(a, "DEFAULT", "TEST_ENV", "value1", "描述", &privacy); err != nil {
		t.Fatalf("UpdateWorkspaceEnv: %v", err)
	}
	if err := UpdateWorkspaceEnv(a, "DEFAULT", "TEST_ENV2", "value2", "描述2", nil); err != nil {
		t.Fatalf("UpdateWorkspaceEnv: %v", err)
	}
	keys := WorkspaceEnvKeys(a, "DEFAULT")
	if len(keys) != 2 || keys[0] != "TEST_ENV" || keys[1] != "TEST_ENV2" {
		t.Errorf("keys = %v", keys)
	}
	env := WorkspaceEnv(a, "DEFAULT")
	if env["TEST_ENV"] != "value1" {
		t.Errorf("env = %v", env)
	}
	content, err := os.ReadFile(filepath.Join(a.DataDir, "workspace_env_var.json"))
	if err != nil {
		t.Fatal(err)
	}
	text := string(content)
	if !strings.Contains(text, "\"id\":\"DEFAULT\"") || !strings.Contains(text, "\"varData\"") {
		t.Errorf("unexpected file content: %s", text)
	}
	if err := DeleteWorkspaceEnv(a, "DEFAULT", "TEST_ENV"); err != nil {
		t.Fatalf("DeleteWorkspaceEnv: %v", err)
	}
	if keys := WorkspaceEnvKeys(a, "DEFAULT"); len(keys) != 1 {
		t.Errorf("keys after delete = %v", keys)
	}
	// 参数校验
	if err := UpdateWorkspaceEnv(a, "DEFAULT", "", "v", "d", nil); err == nil {
		t.Error("empty name should be rejected")
	}
}

func TestSystemConfigReadWrite(t *testing.T) {
	a := newTestApp(t)
	if err := os.MkdirAll(filepath.Dir(a.ConfPath), 0o755); err != nil {
		t.Fatal(err)
	}
	content := "server:\n  port: 2123\njpom:\n  authorize:\n    agent-name: jpomAgent\n"
	if err := os.WriteFile(a.ConfPath, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
	data, err := SystemConfigContent(a)
	if err != nil {
		t.Fatalf("SystemConfigContent: %v", err)
	}
	if data["content"] != content {
		t.Errorf("content mismatch")
	}
	if data["file"] != a.ConfPath {
		t.Errorf("file = %v", data["file"])
	}
	// @ValidatorItem(内容不能为空)：缺参/空串均 code=400（Java 实测）
	if _, err := SaveSystemConfig(a, "  ", ""); err == nil {
		t.Error("empty content should be rejected")
	} else if paramError := asParamError(t, err); paramError.Code != 400 {
		t.Errorf("code = %d", paramError.Code)
	}
	// 非法 YAML -> 500
	if _, err := SaveSystemConfig(a, "a: [", ""); err == nil {
		t.Error("invalid yaml should be rejected")
	} else if paramError := asParamError(t, err); paramError.Code != 500 {
		t.Errorf("code = %d", paramError.Code)
	}
	// tab 缩进会被转换为两个空格后校验，原文写回
	saved := "server:\n\tport: 2124\n"
	if msg, err := SaveSystemConfig(a, saved, ""); err != nil {
		t.Fatalf("SaveSystemConfig: %v", err)
	} else if msg != "i18n.modify_success.69be" {
		t.Errorf("msg = %q", msg)
	} else if localized := i18n.T(i18n.LangEnUs, msg); localized != "Modified successfully" {
		t.Logf("en 文案: %q", localized)
	}
	after, err := os.ReadFile(a.ConfPath)
	if err != nil {
		t.Fatal(err)
	}
	if string(after) != saved {
		t.Errorf("config content not written as-is")
	}
}

func TestLogTreeAndDelete(t *testing.T) {
	a := newTestApp(t)
	old := filepath.Join(a.LogsDir, "2020-01-01")
	if err := os.MkdirAll(old, 0o755); err != nil {
		t.Fatal(err)
	}
	oldFile := filepath.Join(old, "agent-2020-01-01-0.log")
	if err := os.WriteFile(oldFile, []byte("old"), 0o644); err != nil {
		t.Fatal(err)
	}
	oldTime := time.Now().Add(-48 * time.Hour)
	if err := os.Chtimes(oldFile, oldTime, oldTime); err != nil {
		t.Fatal(err)
	}
	recent := filepath.Join(a.LogsDir, "agent.log")
	if err := os.WriteFile(recent, []byte("recent"), 0o644); err != nil {
		t.Fatal(err)
	}
	tree, err := LogTreeData(a)
	if err != nil {
		t.Fatalf("LogTreeData: %v", err)
	}
	if len(tree) != 2 {
		t.Fatalf("tree = %v", tree)
	}
	// 最近修改的文件不能删除 -> 405
	err = DeleteLog(a, "agent.log")
	paramError := asParamError(t, err)
	if paramError.Code != 405 || !strings.Contains(paramError.Msg, "不能删除近一天") {
		t.Errorf("unexpected error: %+v", paramError)
	}
	// 路径穿越 -> 405
	if err := DeleteLog(a, "../conf/application.yml"); err == nil {
		t.Error("path traversal should be rejected")
	}
	// 旧日志可以删除，并清理空父目录
	if err := DeleteLog(a, "2020-01-01/agent-2020-01-01-0.log"); err != nil {
		t.Fatalf("DeleteLog: %v", err)
	}
	if _, err := os.Stat(old); !os.IsNotExist(err) {
		t.Error("empty parent directory should be removed")
	}
	if _, err := LogDownloadPath(a, "agent.log"); err != nil {
		t.Errorf("LogDownloadPath: %v", err)
	}
	if _, err := LogDownloadPath(a, "not-exists.log"); err == nil {
		t.Error("missing log should return error")
	}
}

func TestCacheJSONFields(t *testing.T) {
	a := newTestApp(t)
	RefreshCache(a)
	if err := UpdateWorkspaceEnv(a, "DEFAULT", "TEST_ENV", "value", "desc", nil); err != nil {
		t.Fatal(err)
	}
	// script library
	if err := os.MkdirAll(a.GlobalScriptDir, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(a.GlobalScriptDir, "test.json"), []byte("{\"version\":\"1\"}"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(a.DataDir, "script_log.json"), []byte("{\"a\":{}}"), 0o644); err != nil {
		t.Fatal(err)
	}
	taskList := []map[string]any{}
	_ = taskList
	cache := CacheJSON(a, "DEFAULT")
	for _, key := range []string{"fileSize", "dataSize", "oldJarsSize"} {
		if _, ok := cache[key].(string); !ok {
			t.Errorf("%s should be string, got %T", key, cache[key])
		}
	}
	for _, key := range []string{"pidPort", "readFileOnLineCount", "pluginSize", "scriptExecLogSize"} {
		if _, ok := cache[key].(int); !ok {
			t.Errorf("%s should be int, got %T", key, cache[key])
		}
	}
	if _, ok := cache["taskList"].([]map[string]any); !ok {
		t.Errorf("taskList = %T", cache["taskList"])
	}
	if _, ok := cache["timerMatchSecond"].(bool); !ok {
		t.Errorf("timerMatchSecond = %T", cache["timerMatchSecond"])
	}
	dateTime, _ := cache["dateTime"].(string)
	if _, err := time.Parse("2006-01-02 15:04:05", dateTime); err != nil {
		t.Errorf("dateTime = %q", dateTime)
	}
	if cache["timeZoneId"] == "" {
		t.Error("timeZoneId should not be empty")
	}
	keys, _ := cache["envVarKeys"].([]string)
	if len(keys) != 1 || keys[0] != "TEST_ENV" {
		t.Errorf("envVarKeys = %v", cache["envVarKeys"])
	}
	tagMap, _ := cache["scriptLibraryTagMap"].(map[string]string)
	if tagMap["test"] != "1" {
		t.Errorf("scriptLibraryTagMap = %v", cache["scriptLibraryTagMap"])
	}
	if cache["scriptExecLogSize"].(int) != 1 {
		t.Errorf("scriptExecLogSize = %v", cache["scriptExecLogSize"])
	}
	// clearCache
	if err := ClearCache(a, "pidPort"); err != nil {
		t.Errorf("ClearCache pidPort: %v", err)
	}
	err := ClearCache(a, "unknown")
	if paramError := asParamError(t, err); paramError.Code != 405 || !strings.Contains(paramError.Msg, "没有对应类型") {
		t.Errorf("unexpected error: %+v", paramError)
	}
}

func TestRemoteVersionCache(t *testing.T) {
	a := newTestApp(t)
	file := RemoteVersionFile(a)
	if CacheInfo(a) != nil {
		t.Error("missing cache file should return nil")
	}
	fresh := "{\"tagName\":\"v2.11.13\",\"lastTime\":\"" + strconv.FormatInt(time.Now().Add(-time.Hour).UnixMilli(), 10) + "\",\"upgrade\":false}"
	if err := os.WriteFile(file, []byte(fresh), 0o644); err != nil {
		t.Fatal(err)
	}
	cached := CacheInfo(a)
	if cached == nil || cached["tagName"] != "v2.11.13" {
		t.Fatalf("cacheInfo = %v", cached)
	}
	stale := "{\"tagName\":\"v2.11.13\",\"lastTime\":\"" + strconv.FormatInt(time.Now().Add(-25*time.Hour).UnixMilli(), 10) + "\"}"
	if err := os.WriteFile(file, []byte(stale), 0o644); err != nil {
		t.Fatal(err)
	}
	if CacheInfo(a) != nil {
		t.Error("stale cache should return nil")
	}
	if err := os.WriteFile(file, []byte("not-json"), 0o644); err != nil {
		t.Fatal(err)
	}
	if CacheInfo(a) != nil {
		t.Error("broken cache should return nil")
	}
}

func TestUpgradeShardingValidation(t *testing.T) {
	a := newTestApp(t)
	if err := SaveJarSharding(a, "agent.zip.0", strings.NewReader("x"), "sliceid", 1, 0, ""); err == nil {
		t.Error("missing fileSumMd5 should be rejected")
	}
	err := SaveJarSharding(a, "agent.exe.0", strings.NewReader("x"), "sliceid", 1, 0, "md5")
	if paramError := asParamError(t, err); paramError.Code != 405 || !strings.Contains(paramError.Msg, "不支持的文件类型") {
		t.Errorf("unexpected error: %+v", paramError)
	}
	if err := SaveJarSharding(a, "agent.zip.0", strings.NewReader("part"), "sliceid", 1, 0, "md5"); err != nil {
		t.Fatalf("SaveJarSharding: %v", err)
	}
	// 分片数量不足 -> 405
	_, err = MergeJarSharding(a, "sliceid", 2, "md5")
	if paramError := asParamError(t, err); paramError.Code != 405 || !strings.Contains(paramError.Msg, "分片丢失") {
		t.Errorf("unexpected error: %+v", paramError)
	}
	// 不存在的 sliceId
	_, err = MergeJarSharding(a, "missing", 1, "md5")
	if paramError := asParamError(t, err); paramError.Code != 405 {
		t.Errorf("unexpected error: %+v", paramError)
	}
}

func TestCacheLoadTimeWritesCanonicalFields(t *testing.T) {
	a := newTestApp(t)
	value := map[string]any{
		"tagName":        "v2.11.14",
		"agentUrl":       "https://example.com/agent.zip",
		"serverUrl":      "https://example.com/server.zip",
		"changelogUrl":   "",
		"downloadSource": "cloudflare",
		"downloadUrl":    "https://example.com/download",
		"tag_name":       "v2.11.14-extra",
		"url":            "https://example.com/transit",
		"auth": map[string]any{
			"downloadSource": "cn-cdn",
			"agentUrl":       "https://example.com/auth-agent.zip",
			"serverUrl":      "https://example.com/auth-server.zip",
			"token":          "should-be-dropped",
		},
	}
	cacheLoadTime(a, value)
	content, err := os.ReadFile(RemoteVersionFile(a))
	if err != nil {
		t.Fatalf("读取缓存失败: %v", err)
	}
	cached := map[string]any{}
	if err := jsonUnmarshal(content, &cached); err != nil {
		t.Fatalf("解析缓存失败: %v", err)
	}
	for _, unexpected := range []string{"downloadUrl", "tag_name", "url"} {
		if _, exists := cached[unexpected]; exists {
			t.Errorf("缓存不应包含别名/额外字段 %s: %s", unexpected, string(content))
		}
	}
	if _, ok := cached["lastTime"]; !ok {
		t.Error("缓存应包含 lastTime")
	}
	if _, ok := cached["upgrade"].(bool); !ok {
		t.Errorf("缓存 upgrade 应为 bool, got %T", cached["upgrade"])
	}
	auth, ok := cached["auth"].(map[string]any)
	if !ok {
		t.Fatalf("缓存 auth = %T", cached["auth"])
	}
	if _, exists := auth["token"]; exists {
		t.Errorf("auth 只应保留 Java RemoteVersionAuth 字段: %v", auth)
	}
	if len(auth) != 3 {
		t.Errorf("auth = %v", auth)
	}
}

// TestClearCacheFileSizeKeepsDirectory E2E：fileSize 只清空 data/temp 内容，保留目录。
func TestClearCacheFileSizeKeepsDirectory(t *testing.T) {
	a := newTestApp(t)
	if err := os.MkdirAll(a.TempRoot(), 0o755); err != nil {
		t.Fatal(err)
	}
	file := filepath.Join(a.TempRoot(), "cache.txt")
	if err := os.WriteFile(file, []byte("x"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Join(a.TempRoot(), "slice", "id"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := ClearCache(a, "fileSize"); err != nil {
		t.Fatalf("ClearCache: %v", err)
	}
	if _, err := os.Stat(a.TempRoot()); err != nil {
		t.Errorf("data/temp 目录应保留: %v", err)
	}
	entries, err := os.ReadDir(a.TempRoot())
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 0 {
		t.Errorf("data/temp 应被清空, 仍有 %d 项", len(entries))
	}
}
