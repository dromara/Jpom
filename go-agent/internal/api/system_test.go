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
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/httpx"
)

// newAPITestApp 构造 api 层单测使用的 App（临时目录 + 默认配置）。
func newAPITestApp(t *testing.T) *app.App {
	t.Helper()
	base := t.TempDir()
	a := &app.App{
		Version:           "2.11.13",
		BaseDir:           base,
		ConfPath:          filepath.Join(base, "conf", "application.yml"),
		Config:            config.Default(),
		DataDir:           filepath.Join(base, "data"),
		LogsDir:           filepath.Join(base, "logs"),
		TmpDir:            filepath.Join(base, "tmp"),
		TempDir:           filepath.Join(base, "data", "temp"),
		RunDir:            filepath.Join(base, "bin"),
		Executable:        filepath.Join(base, "bin", "agent"),
		GlobalScriptDir:   filepath.Join(base, "data", "global-script"),
		ScriptRunCacheDir: filepath.Join(base, "data", "script_run_cache"),
		Pid:               4242,
		RandomID:          "random",
		InstallID:         "install",
		StartAt:           time.Now(),
	}
	for _, dir := range []string{a.DataDir, a.LogsDir, a.TmpDir, a.TempDir, a.GlobalScriptDir, a.RunDir, filepath.Dir(a.ConfPath)} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	if err := os.WriteFile(a.ConfPath, []byte("server:\n  port: 2123\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	return a
}

func newContext(a *app.App, method, target string) (*httpx.Context, *httptest.ResponseRecorder) {
	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(method, target, nil)
	request.Header.Set("workspaceId", "DEFAULT")
	return &httpx.Context{W: recorder, R: request, App: a, Workspace: "DEFAULT"}, recorder
}

func decodeEnvelope(t *testing.T, recorder *httptest.ResponseRecorder) map[string]any {
	t.Helper()
	var value map[string]any
	if err := json.Unmarshal(recorder.Body.Bytes(), &value); err != nil {
		t.Fatalf("解析响应失败: %v body=%s", err, recorder.Body.String())
	}
	return value
}

func TestHandleInfoTrimsRemoteVersion(t *testing.T) {
	a := newAPITestApp(t)
	cache := map[string]any{
		"tagName":        "v2.11.13",
		"agentUrl":       "https://example.com/agent.zip",
		"serverUrl":      "https://example.com/server.zip",
		"changelogUrl":   "https://example.com/CHANGELOG.md",
		"changelog":      "# log",
		"lastTime":       time.Now().UnixMilli(),
		"upgrade":        false,
		"downloadSource": "cloudflare",
		"tag_name":       "v2.11.13-extra",
		"downloadUrl":    "https://example.com/extra",
		"url":            "https://example.com/transit",
	}
	content, _ := json.Marshal(cache)
	if err := os.WriteFile(filepath.Join(a.DataDir, "remote_version.json"), content, 0o644); err != nil {
		t.Fatal(err)
	}
	c, recorder := newContext(a, http.MethodPost, "/info")
	handleInfo(c, a)
	envelope := decodeEnvelope(t, recorder)
	if envelope["code"].(float64) != 200 {
		t.Fatalf("code = %v", envelope["code"])
	}
	data := envelope["data"].(map[string]any)
	if _, ok := data["monitor"]; ok {
		t.Error("jpom.monitor 未配置时不应输出 monitor 字段")
	}
	remote, ok := data["remoteVersion"].(map[string]any)
	if !ok {
		t.Fatalf("remoteVersion = %T", data["remoteVersion"])
	}
	for _, unexpected := range []string{"tag_name", "downloadUrl", "url"} {
		if _, exists := remote[unexpected]; exists {
			t.Errorf("remoteVersion 不应包含 %s", unexpected)
		}
	}
	if _, ok := remote["lastTime"].(string); !ok {
		t.Errorf("lastTime 应为字符串, got %T", remote["lastTime"])
	}
	manifest := data["manifest"].(map[string]any)
	if _, ok := manifest["pid"].(string); !ok {
		t.Errorf("manifest.pid 应为字符串, got %T", manifest["pid"])
	}
	if _, ok := data["joinBetaRelease"].(bool); !ok {
		t.Errorf("joinBetaRelease 应为 bool, got %T", data["joinBetaRelease"])
	}
}

func TestHandleInfoMonitorWhenConfigured(t *testing.T) {
	a := newAPITestApp(t)
	a.Config.Jpom.Monitor = &config.Monitor{Network: config.Network{
		StatExcludeNames:      "lo*,docker0",
		StatContainsOnlyNames: "ens*",
	}}
	c, recorder := newContext(a, http.MethodPost, "/info")
	handleInfo(c, a)
	data := decodeEnvelope(t, recorder)["data"].(map[string]any)
	monitor, ok := data["monitor"].(map[string]any)
	if !ok {
		t.Fatalf("monitor = %T", data["monitor"])
	}
	network := monitor["network"].(map[string]any)
	if network["statExcludeNames"] != "lo*,docker0" || network["statContainsOnlyNames"] != "ens*" {
		t.Errorf("network = %v", network)
	}
}

func TestHandleKillRejectsInvalidPid(t *testing.T) {
	a := newAPITestApp(t)
	for _, target := range []string{"/kill.json?pid=0", "/kill.json?pid=" + strconv.Itoa(a.Pid)} {
		c, recorder := newContext(a, http.MethodPost, target)
		handleKill(c, a)
		envelope := decodeEnvelope(t, recorder)
		if envelope["code"].(float64) != 405 {
			t.Errorf("%s code = %v", target, envelope["code"])
		}
		if recorder.Code != http.StatusOK {
			t.Errorf("%s HTTP 状态必须为 200, got %d", target, recorder.Code)
		}
	}
}

func TestHandleLogDownloadMissingReturnsEmptyBody(t *testing.T) {
	a := newAPITestApp(t)
	r := httpx.New(a)
	registerSystem(r, a)
	recorder := httptest.NewRecorder()
	request := httptest.NewRequest(http.MethodGet, "/system/log_download?path=not-exists.log", nil)
	request.Header.Set("Jpom-Agent-Authorize", a.Auth.Authorize)
	// 鉴权在 Router 层，这里直接调用路由（Auth 为空会被拒绝），因此构造一个通过鉴权的 App。
	a.Auth = app.Auth{Name: "jpomAgent", Authorize: "authorized"}
	r = httpx.New(a)
	registerSystem(r, a)
	request.Header.Set("Jpom-Agent-Authorize", "authorized")
	r.ServeHTTP(recorder, request)
	if recorder.Code != http.StatusOK {
		t.Errorf("HTTP 状态 = %d", recorder.Code)
	}
	if body := strings.TrimSpace(recorder.Body.String()); body != "" {
		t.Errorf("文件不存在时 body 应为空, got %q", body)
	}
}

func TestUpgradeHandlerRegistered(t *testing.T) {
	a := newAPITestApp(t)
	a.Auth = app.Auth{Name: "jpomAgent", Authorize: "authorized"}
	r := httpx.New(a)
	registerSystem(r, a)
	manager := runtimeFor(a)
	if manager == nil || manager.ws == nil {
		t.Fatal("ws manager 未初始化")
	}
	if manager.ws.Upgrade == nil {
		t.Fatal("UpgradeHandler 未注册到 ws.Manager")
	}
	// 非升级包应返回错误且不触发重启。
	target := filepath.Join(t.TempDir(), "not-a-package.txt")
	if err := os.WriteFile(target, []byte("x"), 0o644); err != nil {
		t.Fatal(err)
	}
	message, err := manager.ws.Upgrade.Restart(target)
	if err == nil {
		t.Fatalf("非升级包应返回错误, got msg=%q", message)
	}
	if !strings.Contains(err.Error(), "安装包") && !strings.Contains(err.Error(), "升级包") {
		t.Errorf("unexpected error: %v", err)
	}
}
