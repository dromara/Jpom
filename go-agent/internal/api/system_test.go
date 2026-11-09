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
	"bytes"
	"encoding/json"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
	"time"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/sysinfo"
)

// apiMultipartBody 构造 multipart 请求体（用于上传/升级分片入口测试）。
func apiMultipartBody(t *testing.T, fields map[string]string, fileName string, content []byte) (string, *bytes.Buffer) {
	t.Helper()
	var buffer bytes.Buffer
	writer := multipart.NewWriter(&buffer)
	for key, value := range fields {
		if err := writer.WriteField(key, value); err != nil {
			t.Fatal(err)
		}
	}
	if fileName != "" {
		part, err := writer.CreateFormFile("file", fileName)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := part.Write(content); err != nil {
			t.Fatal(err)
		}
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	return writer.FormDataContentType(), &buffer
}

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

// authedRouter 构造带鉴权的 Router（复用 registerSystem 注册全部系统端点）。
func authedRouter(t *testing.T, a *app.App) *httpx.Router {
	t.Helper()
	a.Auth = app.Auth{Name: "jpomAgent", Authorize: "authorized"}
	r := httpx.New(a)
	registerSystem(r, a)
	return r
}

// doSystemRequest 发送带鉴权的表单请求并返回解析后的响应。
func doSystemRequest(t *testing.T, r *httpx.Router, method, target, contentType, body string, headers map[string]string) map[string]any {
	t.Helper()
	var reader *strings.Reader
	if body == "" {
		reader = strings.NewReader("")
	} else {
		reader = strings.NewReader(body)
	}
	request := httptest.NewRequest(method, target, reader)
	if contentType != "" {
		request.Header.Set("Content-Type", contentType)
	}
	request.Header.Set("Jpom-Agent-Authorize", "authorized")
	for key, value := range headers {
		request.Header.Set(key, value)
	}
	recorder := httptest.NewRecorder()
	r.ServeHTTP(recorder, request)
	var envelope map[string]any
	if err := json.Unmarshal(recorder.Body.Bytes(), &envelope); err != nil {
		t.Fatalf("解析响应失败: %v body=%s", err, recorder.Body.String())
	}
	return envelope
}

// TestSaveConfigParamCodes N1：缺参 -> 500 ...:null；空串 -> 400 内容不能为空。
func TestSaveConfigParamCodes(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/save_config.json", "application/x-www-form-urlencoded", "restart=false", nil)
	if envelope["code"].(float64) != 400 || !strings.Contains(envelope["msg"].(string), "内容不能为空") {
		t.Errorf("missing content = %v %v", envelope["code"], envelope["msg"])
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/save_config.json", "application/x-www-form-urlencoded", "content=&restart=false", nil)
	if envelope["code"].(float64) != 400 || !strings.Contains(envelope["msg"].(string), "内容不能为空") {
		t.Errorf("empty content = %v %v", envelope["code"], envelope["msg"])
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/save_config.json", "application/x-www-form-urlencoded", "content=a%3A+%5B&restart=false", map[string]string{"Accept-Language": "en-US"})
	if envelope["code"].(float64) != 500 || !strings.Contains(envelope["msg"].(string), "Content formatting error") {
		t.Errorf("invalid yaml en = %v %v", envelope["code"], envelope["msg"])
	}
}

// TestClearCacheParamCodes N1：缺参 -> 500 NPE 文案；空串 -> 400 类型错误。
func TestClearCacheParamCodes(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/clearCache", "application/x-www-form-urlencoded", "", nil)
	if envelope["code"].(float64) != 400 || envelope["msg"] != "类型错误" {
		t.Errorf("missing type = %v %v", envelope["code"], envelope["msg"])
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/clearCache", "application/x-www-form-urlencoded", "type=", nil)
	if envelope["code"].(float64) != 400 || envelope["msg"] != "类型错误" {
		t.Errorf("blank type = %v %v", envelope["code"], envelope["msg"])
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/clearCache", "application/x-www-form-urlencoded", "type=pidPort", nil)
	if envelope["code"].(float64) != 200 {
		t.Errorf("clear pidPort = %v %v", envelope["code"], envelope["msg"])
	}
}

// TestWorkspaceEnvValidatorCodes N1：@ValidatorItem -> 400 自定义文案/缺省文案。
func TestWorkspaceEnvValidatorCodes(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/workspace_env/update", "application/x-www-form-urlencoded", "value=v&description=d", nil)
	if envelope["code"].(float64) != 400 || envelope["msg"] != "请填写名称" {
		t.Errorf("missing name = %v %v", envelope["code"], envelope["msg"])
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/workspace_env/update", "application/x-www-form-urlencoded", "name=n&value=v&description=", map[string]string{"Accept-Language": "en-US"})
	if envelope["code"].(float64) != 400 || !strings.Contains(envelope["msg"].(string), "description") {
		t.Errorf("missing description en = %v %v", envelope["code"], envelope["msg"])
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/workspace_env/delete", "application/x-www-form-urlencoded", "", nil)
	if envelope["code"].(float64) != 400 || envelope["msg"] != "参数验证失败" {
		t.Errorf("delete missing name = %v %v", envelope["code"], envelope["msg"])
	}
}

// TestWhitelistPathMessageN2 Java 拼接语义：文案 + 具体路径（4 语言都带值）。
func TestWhitelistPathMessageN2(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	for lang, wantPrefix := range map[string]string{
		"zh-CN": "需要配置绝对路径：",
		"en-US": "You need to configure the absolute path:",
	} {
		envelope := doSystemRequest(t, r, http.MethodPost, "/system/whitelistDirectory_submit",
			"application/x-www-form-urlencoded", "project=relative/path", map[string]string{"Accept-Language": lang})
		if envelope["code"].(float64) != 405 {
			t.Errorf("%s code = %v", lang, envelope["code"])
		}
		msg, _ := envelope["msg"].(string)
		if !strings.Contains(msg, wantPrefix) || !strings.Contains(msg, "relative/path") {
			t.Errorf("%s msg = %q, want prefix %q + value", lang, msg, wantPrefix)
		}
	}
}

// TestLogDownloadBlankPath N1：path 缺失 -> code 400 参数错误path错误（不再空 body）。
func TestLogDownloadBlankPath(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodGet, "/system/log_download", "", "", nil)
	if envelope["code"].(float64) != 400 || envelope["msg"] != "参数错误path错误" {
		t.Errorf("blank path = %v %v", envelope["code"], envelope["msg"])
	}
}

// TestUploadJarShardingNoFile N1 #26：无 file 时先按业务 Assert 顺序返回
// 405「没有文件签名信息」（与 Java 一致，不是「没有上传文件」）。
func TestUploadJarShardingNoFile(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/upload-jar-sharding", "application/x-www-form-urlencoded", "", nil)
	if envelope["code"].(float64) != 405 || envelope["msg"] != "没有文件签名信息" {
		t.Errorf("no file = %v %v", envelope["code"], envelope["msg"])
	}
}

// TestKillNonExistentPid E2E：目标进程不存在时按 Java 语义返回 code=200 + shell 输出文本。
func TestKillNonExistentPid(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodPost, "/kill.json", "application/x-www-form-urlencoded", "pid=999999", nil)
	if envelope["code"].(float64) != 200 {
		t.Fatalf("code = %v msg=%v", envelope["code"], envelope["msg"])
	}
	msg, _ := envelope["msg"].(string)
	if msg == "" {
		t.Error("kill 不存在的进程应返回 shell 输出文本")
	}
}

// TestClearCacheInvalidTypeMessage i18n 拼接：没有对应类型：<type>
func TestClearCacheInvalidTypeMessage(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/clearCache", "application/x-www-form-urlencoded", "type=unknownType", nil)
	if envelope["code"].(float64) != 405 {
		t.Fatalf("code = %v", envelope["code"])
	}
	msg, _ := envelope["msg"].(string)
	if !strings.Contains(msg, "没有对应类型：") || !strings.Contains(msg, "unknownType") {
		t.Errorf("msg = %q, 应包含类型值", msg)
	}
}

// TestUploadJarShardingNonMultipartWithParams 回归 D1：非 multipart 请求但带齐
// 分片参数时必须返回 405「没有上传文件」，不能 nil deref 被 recover 成 code=200。
func TestUploadJarShardingNonMultipartWithParams(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	body := "fileSumMd5=abc&sliceId=s1&totalSlice=1&nowSlice=1"
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/upload-jar-sharding", "application/x-www-form-urlencoded", body, nil)
	code := envelope["code"].(float64)
	if code != 405 {
		t.Fatalf("non multipart with params code = %v msg=%v", code, envelope["msg"])
	}
	if envelope["msg"] != "没有上传文件" {
		t.Errorf("non multipart msg = %v, want 没有上传文件", envelope["msg"])
	}
}

// TestUploadJarShardingMultipartLimits 校验升级分片入口的 multipart 限制：
// 超过 max-file-size / max-request-size 返回 code=406 与 Java 文案。
func TestUploadJarShardingMultipartLimits(t *testing.T) {
	a := newAPITestApp(t)
	a.Config.Spring.Servlet.Multipart.MaxFileSize = "10B"
	a.Config.Spring.Servlet.Multipart.MaxRequestSize = "1KB"
	r := authedRouter(t, a)
	fields := map[string]string{"fileSumMd5": "abc", "sliceId": "s1", "totalSlice": "1", "nowSlice": "1"}

	do := func(contentType string, body *bytes.Buffer) map[string]any {
		request := httptest.NewRequest(http.MethodPost, "/system/upload-jar-sharding", body)
		request.Header.Set("Content-Type", contentType)
		request.Header.Set("Jpom-Agent-Authorize", "authorized")
		recorder := httptest.NewRecorder()
		r.ServeHTTP(recorder, request)
		return decodeEnvelope(t, recorder)
	}

	contentType, body := apiMultipartBody(t, fields, "upgrade-package-agent-go-2.11.13-upgrade-linux-amd64.jar", []byte("12345678901234567890"))
	envelope := do(contentType, body)
	if envelope["code"].(float64) != 406 {
		t.Fatalf("file too large code = %v msg=%v", envelope["code"], envelope["msg"])
	}
	if msg, _ := envelope["msg"].(string); !strings.Contains(msg, "上传文件太大") {
		t.Errorf("file too large msg = %q", msg)
	}

	contentType, body = apiMultipartBody(t, map[string]string{"fileSumMd5": strings.Repeat("x", 2048)}, "", nil)
	envelope = do(contentType, body)
	if envelope["code"].(float64) != 406 {
		t.Fatalf("request too large code = %v msg=%v", envelope["code"], envelope["msg"])
	}

	// 限制内时正常返回 200（分片文件名为 <name>.<nowSlice>，最后一段是序号）。
	contentType, body = apiMultipartBody(t, fields, "upgrade-package-agent-go-2.11.13-upgrade-linux-amd64.jar.1", []byte("12345"))
	envelope = do(contentType, body)
	if envelope["code"].(float64) != 200 {
		t.Fatalf("within limits code = %v msg=%v", envelope["code"], envelope["msg"])
	}
}

// TestSystemCacheTaskListAndTagMap 校验 /system/cache：
//   - taskList 只包含脚本定时任务（排除 system_cache/auto_back_log 内部任务）；
//   - scriptLibraryTagMap 无数据时输出 {}（不省略）。
func TestSystemCacheTaskListAndTagMap(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	// 注册一个脚本任务 + 一个内部任务，验证过滤逻辑。
	if err := sysinfo.RegisterCronTask("script-task-test", "0 0/10 * * * ?", func() {}); err != nil {
		t.Fatalf("register script cron: %v", err)
	}
	if err := sysinfo.RegisterCronTask("auto_back_log", "0 0/10 * * * ?", func() {}); err != nil {
		t.Fatalf("register internal cron: %v", err)
	}
	t.Cleanup(func() {
		_ = sysinfo.RemoveCronTask("script-task-test")
		_ = sysinfo.RemoveCronTask("auto_back_log")
	})
	// 空脚本库 -> {}
	envelope := doSystemRequest(t, r, http.MethodPost, "/system/cache", "application/x-www-form-urlencoded", "", nil)
	data := envelope["data"].(map[string]any)
	tags, ok := data["scriptLibraryTagMap"].(map[string]any)
	if !ok || len(tags) != 0 {
		t.Fatalf("scriptLibraryTagMap must be {} when empty, got %#v", data["scriptLibraryTagMap"])
	}
	// 对齐 Java CronUtils.list：只有首次触发过的任务才出现在 taskList；
	// 仅注册未执行时应为空（内部任务也不能被列出）。
	seen := map[string]bool{}
	list, _ := data["taskList"].([]any)
	for _, item := range list {
		entry, _ := item.(map[string]any)
		if id, _ := entry["taskId"].(string); id != "" {
			seen[id] = true
		}
	}
	if len(seen) != 0 {
		t.Errorf("taskList must be empty before any execution (Java semantics), got %v", seen)
	}
	// 有脚本库数据时输出 tag -> version。
	if err := os.MkdirAll(a.GlobalScriptDir, 0o755); err != nil {
		t.Fatal(err)
	}
	library := map[string]any{"id": "tag1", "tag": "tag1", "version": "1.2.3", "script": "echo hi", "description": "d"}
	encoded, _ := json.Marshal(library)
	if err := os.WriteFile(filepath.Join(a.GlobalScriptDir, "tag1.json"), encoded, 0o644); err != nil {
		t.Fatal(err)
	}
	envelope = doSystemRequest(t, r, http.MethodPost, "/system/cache", "application/x-www-form-urlencoded", "", nil)
	data = envelope["data"].(map[string]any)
	tags, ok = data["scriptLibraryTagMap"].(map[string]any)
	if !ok || tags["tag1"] != "1.2.3" {
		t.Fatalf("scriptLibraryTagMap = %#v", data["scriptLibraryTagMap"])
	}
}

// TestSystemLogDownloadChineseName 校验 /system/log_download 的中文文件名下载头
// （5 个下载端点统一走 httpx.ContentDisposition）。
func TestSystemLogDownloadChineseName(t *testing.T) {
	a := newAPITestApp(t)
	r := authedRouter(t, a)
	if err := os.MkdirAll(a.LogsDir, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(a.LogsDir, "中文日志.log"), []byte("hello"), 0o644); err != nil {
		t.Fatal(err)
	}
	request := httptest.NewRequest(http.MethodGet, "/system/log_download?path="+url.QueryEscape("中文日志.log"), nil)
	request.Header.Set("Jpom-Agent-Authorize", "authorized")
	recorder := httptest.NewRecorder()
	r.ServeHTTP(recorder, request)
	want := httpx.ContentDisposition("中文日志.log")
	if got := recorder.Header().Get("Content-Disposition"); got != want {
		t.Errorf("download disposition = %q, want %q", got, want)
	}
	if recorder.Body.String() != "hello" {
		t.Errorf("download body = %q", recorder.Body.String())
	}
}
