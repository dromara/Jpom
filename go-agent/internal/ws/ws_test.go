/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package ws

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/config"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/script"
	"github.com/dromara/Jpom/go-agent/internal/sysinfo"
)

const testAuthorize = "test-authorize-digest"

func testApp(t *testing.T) *app.App {
	t.Helper()
	base := t.TempDir()
	dataDir := filepath.Join(base, "data")
	instance := &app.App{
		BaseDir:           base,
		DataDir:           dataDir,
		LogsDir:           filepath.Join(base, "logs"),
		TmpDir:            filepath.Join(base, "tmp"),
		ProjectLogDir:     filepath.Join(base, "project-log"),
		ScriptDir:         filepath.Join(dataDir, "script"),
		GlobalScriptDir:   filepath.Join(dataDir, "global-script"),
		ScriptRunCacheDir: filepath.Join(dataDir, "script_run_cache"),
		TempDir:           filepath.Join(dataDir, "temp"),
		Config:            config.Default(),
		Auth:              app.Auth{Name: "test", Authorize: testAuthorize},
	}
	for _, dir := range []string{instance.DataDir, instance.LogsDir, instance.TmpDir, instance.ProjectLogDir,
		instance.ScriptDir, instance.GlobalScriptDir, instance.ScriptRunCacheDir, instance.TempDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	return instance
}

func testManager(t *testing.T, instance *app.App) *Manager {
	t.Helper()
	scripts := script.NewManager(instance)
	return NewManager(instance, scripts, script.NewExecutor(scripts))
}

// startServer serves one websocket handler with an httpx Context.
func startServer(t *testing.T, instance *app.App, handler func(*httpx.Context)) *httptest.Server {
	t.Helper()
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		handler(&httpx.Context{W: w, R: r, App: instance})
	}))
	t.Cleanup(server.Close)
	return server
}

func dial(t *testing.T, server *httptest.Server, path, extraQuery string) *websocket.Conn {
	t.Helper()
	url := "ws" + strings.TrimPrefix(server.URL, "http") + path + "?Jpom-Agent-Authorize=" + testAuthorize + extraQuery
	conn, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		t.Fatalf("dial %s: %v", path, err)
	}
	t.Cleanup(func() { _ = conn.Close() })
	return conn
}

// readText reads one text frame.
func readText(t *testing.T, conn *websocket.Conn, timeout time.Duration) string {
	t.Helper()
	_ = conn.SetReadDeadline(time.Now().Add(timeout))
	messageType, data, err := conn.ReadMessage()
	if err != nil {
		t.Fatalf("read frame: %v", err)
	}
	if messageType != websocket.TextMessage {
		t.Fatalf("expected text frame, got type %d", messageType)
	}
	return string(data)
}

// readUntil reads frames until match returns true (or fails on timeout).
func readUntil(t *testing.T, conn *websocket.Conn, timeout time.Duration, match func(string) bool) []string {
	t.Helper()
	deadline := time.Now().Add(timeout)
	frames := make([]string, 0, 8)
	for time.Now().Before(deadline) {
		frame := readText(t, conn, time.Until(deadline))
		frames = append(frames, frame)
		if match(frame) {
			return frames
		}
	}
	t.Fatalf("no matching frame, received: %v", frames)
	return frames
}

func sendJSON(t *testing.T, conn *websocket.Conn, value any) {
	t.Helper()
	encoded, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	if err := conn.WriteMessage(websocket.TextMessage, encoded); err != nil {
		t.Fatalf("write frame: %v", err)
	}
}

func TestUnauthorizedClosesWith1003(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	server := startServer(t, instance, manager.HandleSystemLog)
	url := "ws" + strings.TrimPrefix(server.URL, "http") + "/system_log?Jpom-Agent-Authorize=wrong"
	conn, _, err := websocket.DefaultDialer.Dial(url, nil)
	if err != nil {
		t.Fatalf("dial: %v", err)
	}
	defer func() { _ = conn.Close() }()
	_ = conn.SetReadDeadline(time.Now().Add(3 * time.Second))
	_, _, err = conn.ReadMessage()
	closeError, ok := err.(*websocket.CloseError)
	if !ok {
		t.Fatalf("expected close error, got %v", err)
	}
	if closeError.Code != closeCannotAccept {
		t.Fatalf("expected close code %d, got %d", closeCannotAccept, closeError.Code)
	}
	if closeError.Text != "授权信息错误" {
		t.Fatalf("unexpected close reason: %s", closeError.Text)
	}
}

func TestScriptRunStreamsExecution(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	item := map[string]any{"id": "script-1", "name": "测试脚本", "context": "echo hello-script", "workspaceId": "DEFAULT"}
	if err := manager.Scripts.Add(item); err != nil {
		t.Fatal(err)
	}
	server := startServer(t, instance, manager.HandleScriptRun)
	conn := dial(t, server, "/script_run", "&id=script-1&workspaceId=DEFAULT&optUser=devadmin&lang=zh-CN")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：测试脚本" {
		t.Fatalf("unexpected hello frame: %s", hello)
	}
	sendJSON(t, conn, map[string]any{"scriptId": "script-1", "op": "start", "executeId": "execute-1", "args": "[]"})
	control := readText(t, conn, 3*time.Second)
	if !strings.Contains(control, "\"code\":200") || !strings.Contains(control, "执行成功") {
		t.Fatalf("unexpected control frame: %s", control)
	}
	frames := readUntil(t, conn, 5*time.Second, func(frame string) bool {
		return strings.Contains(frame, "JPOM_MSG") && strings.Contains(frame, "stop")
	})
	joined := strings.Join(frames, "\n")
	if !strings.Contains(joined, "hello-script") {
		t.Fatalf("execution output missing: %s", joined)
	}
	if !strings.Contains(joined, "执行完毕:0") {
		t.Fatalf("completion frame missing: %s", joined)
	}
	stored, _, _ := manager.Scripts.Get("script-1")
	if bodyString(stored, "lastRunUser") != "devadmin" {
		t.Fatalf("lastRunUser not updated: %v", stored["lastRunUser"])
	}
}

func TestSystemLogTailsFile(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	logFile := filepath.Join(instance.LogsDir, "agent.log")
	if err := os.WriteFile(logFile, []byte("line1\nline2\nline3\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	server := startServer(t, instance, manager.HandleSystemLog)
	conn := dial(t, server, "/system_log", "&optUser=devadmin&lang=zh-CN")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：插件端日志" {
		t.Fatalf("unexpected hello frame: %s", hello)
	}
	sendJSON(t, conn, map[string]any{"op": "showlog", "fileName": "agent.log"})
	banner := readText(t, conn, 3*time.Second)
	if !strings.Contains(banner, "监听agent.log日志成功") {
		t.Fatalf("unexpected banner: %s", banner)
	}
	if line := readText(t, conn, 3*time.Second); line != "line1" {
		t.Fatalf("unexpected first line: %s", line)
	}
	_ = readText(t, conn, 3*time.Second)
	_ = readText(t, conn, 3*time.Second)
	appendFile(t, logFile, "line4\n")
	if line := readText(t, conn, 5*time.Second); line != "line4" {
		t.Fatalf("tail did not deliver appended line: %s", line)
	}
	sendJSON(t, conn, map[string]any{"op": "showlog", "fileName": "../../etc/passwd"})
	if frame := readText(t, conn, 3*time.Second); frame != "文件不存在" {
		t.Fatalf("traversal must be rejected: %s", frame)
	}
}

func appendFile(t *testing.T, path, text string) {
	t.Helper()
	file, err := os.OpenFile(path, os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = file.Close() }()
	if _, err := file.WriteString(text); err != nil {
		t.Fatal(err)
	}
}

// fakeConsoleBackend drives the /console frames without touching real projects.
type fakeConsoleBackend struct {
	project map[string]any
	success bool
	message string
}

func (f *fakeConsoleBackend) Project(string) (map[string]any, bool) { return f.project, true }

func (f *fakeConsoleBackend) Execute(op string, _ map[string]any) (bool, string, any) {
	if op == "status" {
		return f.success, "", map[string]any{"success": f.success, "msgs": []string{}}
	}
	if f.success {
		return true, "", map[string]any{"success": true, "msgs": []string{}}
	}
	return false, f.message, nil
}

func (f *fakeConsoleBackend) UpdateModifyUser(string, string) {}

func (f *fakeConsoleBackend) CanReload(map[string]any) bool { return false }

func TestConsoleProtocol(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	libDir := filepath.Join(instance.BaseDir, "lib")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	projectInfo := map[string]any{
		"id": "p1", "name": "demo", "workspaceId": "DEFAULT", "runMode": "Java",
		"whitelistDirectory": instance.BaseDir, "lib": "lib",
	}
	logFile := filepath.Join(instance.ProjectLogDir, "p1", "p1.log")
	if err := os.MkdirAll(filepath.Dir(logFile), 0o755); err != nil {
		t.Fatal(err)
	}
	fixture, err := json.Marshal(map[string]any{"p1": projectInfo})
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(instance.DataDir, "project.json"), fixture, 0o644); err != nil {
		t.Fatal(err)
	}
	backend := &fakeConsoleBackend{project: projectInfo, success: false, message: "未运行"}
	manager.Console = backend
	server := startServer(t, instance, manager.HandleConsole)
	conn := dial(t, server, "/console", "&projectId=p1&optUser=devadmin&lang=zh-CN")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：demo" {
		t.Fatalf("unexpected hello: %s", hello)
	}
	// status 未运行
	sendJSON(t, conn, map[string]any{"op": "status", "projectId": "p1"})
	frame := readText(t, conn, 3*time.Second)
	var status map[string]any
	if err := json.Unmarshal([]byte(frame), &status); err != nil {
		t.Fatalf("status frame is not json: %s", frame)
	}
	if status["code"].(float64) != 404 || status["msg"] != "未运行" || status[socketMsgTag] != "JPOM_MSG" {
		t.Fatalf("unexpected status frame: %s", frame)
	}
	// start 成功
	backend.success = true
	sendJSON(t, conn, map[string]any{"op": "start", "projectId": "p1"})
	frame = readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, "\"code\":200") || !strings.Contains(frame, "操作成功") {
		t.Fatalf("unexpected start frame: %s", frame)
	}
	// 未知 op
	sendJSON(t, conn, map[string]any{"op": "unknown-op", "projectId": "p1"})
	frame = readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, "不支持的方式：unknown-op") {
		t.Fatalf("unexpected unknown op frame: %s", frame)
	}
	// showlog 默认日志文件
	if err := os.WriteFile(logFile, []byte("console-line-1\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	sendJSON(t, conn, map[string]any{"op": "showlog", "projectId": "p1"})
	banner := readText(t, conn, 3*time.Second)
	if !strings.Contains(banner, "监听p1.log日志成功") {
		t.Fatalf("unexpected console banner: %s", banner)
	}
	if line := readText(t, conn, 3*time.Second); line != "console-line-1" {
		t.Fatalf("unexpected console log line: %s", line)
	}
}

func TestFreeScriptRun(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	server := startServer(t, instance, manager.HandleFreeScriptRun)
	conn := dial(t, server, "/free-script-run", "&optUser=devadmin")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功" {
		t.Fatalf("unexpected hello: %s", hello)
	}
	sendJSON(t, conn, map[string]any{
		"content": "echo free-hello", "path": t.TempDir(), "tag": "tag-1", "environment": map[string]any{},
	})
	frames := readUntil(t, conn, 5*time.Second, func(frame string) bool {
		return frame == "JPOM_SYSTEM_TAG:tag-1"
	})
	joined := strings.Join(frames, "\n")
	if !strings.Contains(joined, "free-hello") {
		t.Fatalf("free script output missing: %s", joined)
	}
	if !strings.Contains(joined, "执行结束:0") {
		t.Fatalf("free script completion missing: %s", joined)
	}
	conn2 := dial(t, server, "/free-script-run", "")
	_ = readText(t, conn2, 3*time.Second)
	sendJSON(t, conn2, map[string]any{"content": "echo hi", "path": "", "tag": "t", "environment": map[string]any{}})
	frame := readText(t, conn2, 3*time.Second)
	if !strings.Contains(frame, "参数存在不正确") {
		t.Fatalf("unexpected validation frame: %s", frame)
	}
}

func TestNodeUpdateProtocol(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	server := startServer(t, instance, manager.HandleNodeUpdate)
	conn := dial(t, server, "/node_update", "")
	sendJSON(t, conn, map[string]any{"command": "getVersion", "nodeId": "N1", "data": ""})
	frame := readText(t, conn, 3*time.Second)
	var model map[string]any
	if err := json.Unmarshal([]byte(frame), &model); err != nil {
		t.Fatalf("getVersion frame is not json: %s", frame)
	}
	manifestText, ok := model["data"].(string)
	if !ok || !strings.Contains(manifestText, "version") {
		t.Fatalf("getVersion data must be a manifest string: %s", frame)
	}
	sendJSON(t, conn, map[string]any{
		"command": "upload", "nodeId": "N1",
		"params": map[string]any{"name": "agent-test.zip", "size": 4, "version": "test-version"},
		"data":   "",
	})
	frame = readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, "\"command\":\"upload\"") {
		t.Fatalf("unexpected upload frame: %s", frame)
	}
	if err := conn.WriteMessage(websocket.BinaryMessage, []byte("abcd")); err != nil {
		t.Fatal(err)
	}
	frame = readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, "\"command\":\"updateNode\"") || !strings.Contains(frame, "\"completeSize\":\"4\"") {
		t.Fatalf("unexpected progress frame: %s", frame)
	}
	// Java UploadFileModel: size/completeSize are serialized as JSON strings (live
	// capture), savePath is agentConfig.getTempPath() (<data>/temp/<yyyy-MM-dd>)
	// and the getFilePath() getter is serialized as the filePath property.
	var progress map[string]any
	if err := json.Unmarshal([]byte(frame), &progress); err != nil {
		t.Fatalf("progress frame is not json: %s", frame)
	}
	data, _ := progress["data"].(map[string]any)
	if data == nil {
		t.Fatalf("progress data missing: %s", frame)
	}
	// Live Java 2.11.13 capture: fastjson2 serializes long as JSON strings here
	if data["size"] != "4" {
		t.Fatalf("size must be the JSON string \"4\" (Java live capture), got %v", data["size"])
	}
	if data["completeSize"] != "4" {
		t.Fatalf("completeSize must be the JSON string \"4\", got %v", data["completeSize"])
	}
	if data["savePath"] != instance.TempPath() {
		t.Fatalf("savePath must be the day scoped temp dir %q, got %v", instance.TempPath(), data["savePath"])
	}
	if data["filePath"] != instance.TempPath()+"/agent-test.zip" {
		t.Fatalf("filePath mismatch: %v", data["filePath"])
	}
	uploaded := filepath.Join(instance.TempPath(), "agent-test.zip")
	if content, err := os.ReadFile(uploaded); err != nil || string(content) != "abcd" {
		t.Fatalf("uploaded file content wrong: %q %v", content, err)
	}
	sendJSON(t, conn, map[string]any{"command": "restart", "nodeId": "N1", "data": ""})
	frame = readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, "重启失败") {
		t.Fatalf("unexpected restart frame: %s", frame)
	}
	sendJSON(t, conn, map[string]any{"command": "heart", "nodeId": "N1", "data": ""})
	frame = readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, "\"command\":\"heart\"") || !strings.Contains(frame, "\"data\":\"\"") {
		t.Fatalf("unexpected heart frame: %s", frame)
	}
}

func TestSystemLogGBK(t *testing.T) {
	instance := testApp(t)
	instance.Config.Jpom.System.LogCharset = "GBK"
	manager := testManager(t, instance)
	gbkContent, err := script.EncodeText("中文日志"+"\n", "GBK")
	if err != nil {
		t.Fatal(err)
	}
	logFile := filepath.Join(instance.LogsDir, "agent.log")
	if err := os.WriteFile(logFile, gbkContent, 0o644); err != nil {
		t.Fatal(err)
	}
	server := startServer(t, instance, manager.HandleSystemLog)
	conn := dial(t, server, "/system_log", "")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：插件端日志" {
		t.Fatalf("unexpected hello: %s", hello)
	}
	sendJSON(t, conn, map[string]any{"op": "showlog", "fileName": "agent.log"})
	banner := readText(t, conn, 3*time.Second)
	if !strings.Contains(banner, "监听agent.log日志成功") {
		t.Fatalf("unexpected banner: %s", banner)
	}
	if line := readText(t, conn, 3*time.Second); line != "中文日志" {
		t.Fatalf("GBK system log must be decoded, got %q", line)
	}
}

func TestConsoleLogCharsetAndNoRelisten(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	libDir := filepath.Join(instance.BaseDir, "lib")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	projectInfo := map[string]any{
		"id": "p1", "name": "demo", "workspaceId": "DEFAULT", "runMode": "Java",
		"whitelistDirectory": instance.BaseDir, "lib": "lib", "logCharset": "GBK",
	}
	manager.Console = &fakeConsoleBackend{project: projectInfo, success: true}
	logFile := filepath.Join(instance.ProjectLogDir, "p1", "p1.log")
	if err := os.MkdirAll(filepath.Dir(logFile), 0o755); err != nil {
		t.Fatal(err)
	}
	firstLog, err := script.EncodeText("控制台中文"+"\n", "GBK")
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(logFile, firstLog, 0o644); err != nil {
		t.Fatal(err)
	}
	secondLog, err := script.EncodeText("第二个日志"+"\n", "GBK")
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(libDir, "other.log"), secondLog, 0o644); err != nil {
		t.Fatal(err)
	}
	server := startServer(t, instance, manager.HandleConsole)
	conn := dial(t, server, "/console", "&projectId=p1")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：demo" {
		t.Fatalf("unexpected hello: %s", hello)
	}
	// 默认控制台日志（GBK 解码）
	sendJSON(t, conn, map[string]any{"op": "showlog", "projectId": "p1"})
	_ = readText(t, conn, 3*time.Second) // banner
	if line := readText(t, conn, 3*time.Second); line != "控制台中文" {
		t.Fatalf("project log must be decoded with the project charset, got %q", line)
	}
	// 切换日志文件：只发新文件 banner，不发 Relisten（仅 reWatcher 才发）
	sendJSON(t, conn, map[string]any{"op": "showlog", "projectId": "p1", "fileName": "other.log"})
	frame := readText(t, conn, 3*time.Second)
	if strings.Contains(frame, "Relisten") {
		t.Fatalf("file switch must not send Relisten: %s", frame)
	}
	if !strings.Contains(frame, "监听other.log日志成功") {
		t.Fatalf("unexpected switch banner: %s", frame)
	}
	if line := readText(t, conn, 3*time.Second); line != "第二个日志" {
		t.Fatalf("switched log must be decoded, got %q", line)
	}
}

func TestScriptRunOpenHintsWithoutClose(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	server := startServer(t, instance, manager.HandleScriptRun)
	conn := dial(t, server, "/script_run", "")
	if hint := readText(t, conn, 3*time.Second); hint != "脚本模板或者工作空间未知" {
		t.Fatalf("unexpected hint: %s", hint)
	}
	// Java 只发提示、不主动关闭：连接应保持打开（读取超时而不是 close 帧）
	_ = conn.SetReadDeadline(time.Now().Add(500 * time.Millisecond))
	_, _, err := conn.ReadMessage()
	if err == nil {
		t.Fatal("expected read timeout (session must stay open)")
	}
	if closeError, ok := err.(*websocket.CloseError); ok {
		t.Fatalf("session must not be closed after open hint, got close %d %q", closeError.Code, closeError.Text)
	}
	if netError, ok := err.(net.Error); !ok || !netError.Timeout() {
		t.Fatalf("expected read timeout, got %v", err)
	}
}

func TestSystemLogUpdatesFileTailCount(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	logFile := filepath.Join(instance.LogsDir, "agent.log")
	if err := os.WriteFile(logFile, []byte("tail-count-line\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	server := startServer(t, instance, manager.HandleSystemLog)
	conn := dial(t, server, "/system_log", "")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：插件端日志" {
		t.Fatalf("unexpected hello: %s", hello)
	}
	sendJSON(t, conn, map[string]any{"op": "showlog", "fileName": "agent.log"})
	if banner := readText(t, conn, 3*time.Second); !strings.Contains(banner, "监听agent.log日志成功") {
		t.Fatalf("unexpected banner: %s", banner)
	}
	// /system/cache.readFileOnLineCount 必须反映当前 tail 的文件数
	cache := sysinfo.CacheJSON(instance, "DEFAULT")
	if count, ok := cache["readFileOnLineCount"].(int); !ok || count < 1 {
		t.Fatalf("readFileOnLineCount must be > 0 while tailing, got %v", cache["readFileOnLineCount"])
	}
	// 关闭会话后计数归 0
	if err := conn.Close(); err != nil {
		t.Fatal(err)
	}
	deadline := time.Now().Add(5 * time.Second)
	for time.Now().Before(deadline) {
		cache = sysinfo.CacheJSON(instance, "DEFAULT")
		if count, ok := cache["readFileOnLineCount"].(int); ok && count == 0 {
			return
		}
		time.Sleep(50 * time.Millisecond)
	}
	t.Fatalf("readFileOnLineCount must return to 0 after close, got %v", cache["readFileOnLineCount"])
}

// fakeUpgradeHandler 记录 Restart 调用并返回约定 message（模拟 sysinfo 返回 i18n key）。
type fakeUpgradeHandler struct {
	message string
	err     error
	called  bool
}

func (h *fakeUpgradeHandler) Restart(string) (string, error) {
	h.called = true
	return h.message, h.err
}

// TestNodeUpdateReadLimitFromConfig 校验二进制帧上限取
// min(spring.servlet.multipart.max-request-size, 10MB)。
func TestNodeUpdateReadLimitFromConfig(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	if got := manager.binaryReadLimit(); got != binaryMessageCap {
		t.Fatalf("default binary read limit = %d, want %d", got, binaryMessageCap)
	}
	instance.Config.Spring.Servlet.Multipart.MaxRequestSize = "2MB"
	if got := manager.binaryReadLimit(); got != 2<<20 {
		t.Fatalf("configured binary read limit = %d, want %d", got, 2<<20)
	}
	instance.Config.Spring.Servlet.Multipart.MaxRequestSize = "50MB"
	if got := manager.binaryReadLimit(); got != binaryMessageCap {
		t.Fatalf("capped binary read limit = %d, want %d", got, binaryMessageCap)
	}
}

// TestNodeUpdateRestartLocalized 校验 restart 响应 data 按会话语言渲染，
// 不返回原始 i18n key（P2-8）。
func TestNodeUpdateRestartLocalized(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	server := startServer(t, instance, manager.HandleNodeUpdate)
	conn := dial(t, server, "/node_update", "&lang=en-US")
	sendJSON(t, conn, map[string]any{
		"command": "upload", "nodeId": "N1",
		"params": map[string]any{"name": "agent-test.zip", "size": 4, "version": "test-version"},
	})
	_ = readText(t, conn, 3*time.Second)
	if err := conn.WriteMessage(websocket.BinaryMessage, []byte("abcd")); err != nil {
		t.Fatal(err)
	}
	_ = readText(t, conn, 3*time.Second)
	// Upgrade 未装配时回退「重启失败」，必须为英文且不含原始 key。
	sendJSON(t, conn, map[string]any{"command": "restart", "nodeId": "N1"})
	frame := readText(t, conn, 3*time.Second)
	data := frameData(t, frame)
	if strings.Contains(data, "i18n.") {
		t.Fatalf("restart data must be localized, got %q", data)
	}
	if !strings.Contains(data, "Restart failed") {
		t.Fatalf("restart data should be English, got %q", data)
	}
}

// TestNodeUpdateRestartUpgradeMessageLocalized 校验升级成功提示（sysinfo 返回
// i18n.upgrade_duration_message.bab4）在 WS 帧中已本地化。
func TestNodeUpdateRestartUpgradeMessageLocalized(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	upgrade := &fakeUpgradeHandler{message: "i18n.upgrade_duration_message.bab4"}
	manager.Upgrade = upgrade
	server := startServer(t, instance, manager.HandleNodeUpdate)
	conn := dial(t, server, "/node_update", "&lang=en-US")
	sendJSON(t, conn, map[string]any{
		"command": "upload", "nodeId": "N1",
		"params": map[string]any{"name": "agent-test.zip", "size": 4, "version": "test-version"},
	})
	_ = readText(t, conn, 3*time.Second)
	if err := conn.WriteMessage(websocket.BinaryMessage, []byte("abcd")); err != nil {
		t.Fatal(err)
	}
	_ = readText(t, conn, 3*time.Second)
	sendJSON(t, conn, map[string]any{"command": "restart", "nodeId": "N1"})
	frame := readText(t, conn, 3*time.Second)
	data := frameData(t, frame)
	if !upgrade.called {
		t.Fatal("upgrade handler should be called")
	}
	if strings.Contains(data, "i18n.") {
		t.Fatalf("upgrade message must be localized, got %q", data)
	}
	if !strings.Contains(data, "It takes about 30 seconds") {
		t.Fatalf("upgrade message should be English, got %q", data)
	}
}

// frameData extracts the string data field of a websocket frame.
func frameData(t *testing.T, frame string) string {
	t.Helper()
	var model map[string]any
	if err := json.Unmarshal([]byte(frame), &model); err != nil {
		t.Fatalf("frame is not json: %s", frame)
	}
	data, _ := model["data"].(string)
	return data
}

// TestConsoleSearchStreamsRangeAndCounts 校验 /console 日志搜索：
// 只读取区间行，summary 第一个数字=区间行数（Java FileSearchUtil.searchList）。
func TestConsoleSearchRangeAndCounts(t *testing.T) {
	instance := testApp(t)
	manager := testManager(t, instance)
	libDir := filepath.Join(instance.BaseDir, "lib")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	projectInfo := map[string]any{
		"id": "p1", "name": "demo", "workspaceId": "DEFAULT", "runMode": "Java",
		"whitelistDirectory": instance.BaseDir, "lib": "lib",
	}
	fixture, err := json.Marshal(map[string]any{"p1": projectInfo})
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(instance.DataDir, "project.json"), fixture, 0o644); err != nil {
		t.Fatal(err)
	}
	var logBuilder strings.Builder
	for line := 1; line <= 20; line++ {
		suffix := ""
		if line == 3 || line == 15 {
			suffix = " hit"
		}
		fmt.Fprintf(&logBuilder, "line-%02d%s\n", line, suffix)
	}
	if err := os.WriteFile(filepath.Join(libDir, "search.log"), []byte(logBuilder.String()), 0o644); err != nil {
		t.Fatal(err)
	}
	backend := &fakeConsoleBackend{project: projectInfo, success: false, message: "未运行"}
	manager.Console = backend
	server := startServer(t, instance, manager.HandleConsole)
	conn := dial(t, server, "/console", "&projectId=p1&lang=zh-CN")
	if hello := readText(t, conn, 3*time.Second); hello != "连接成功：demo" {
		t.Fatalf("unexpected hello: %s", hello)
	}

	// head=10,tail=10,first=false -> 区间 [max(10-10,1),10] = 1..10，仅第 3 行命中。
	sendJSON(t, conn, map[string]any{
		"op": "showlog", "projectId": "p1", "search": true, "logFile": "search.log",
		"keyword": "hit", "head": 10, "tail": 10, "first": false, "beforeCount": 0, "afterCount": 0,
	})
	frames := readUntil(t, conn, 5*time.Second, func(frame string) bool {
		return strings.Contains(frame, "搜索到并显示")
	})
	joined := strings.Join(frames, "\n")
	if !strings.Contains(joined, "line-03 hit") {
		t.Fatalf("matched line missing: %s", joined)
	}
	if strings.Contains(joined, "line-15 hit") {
		t.Fatalf("search must only read the selected range: %s", joined)
	}
	if !strings.Contains(joined, "在 10 行中搜索到并显示 1 行") {
		t.Fatalf("unexpected search summary: %s", joined)
	}

	// head=0,tail=5,first=false -> 最后 5 行（16..20），无命中。
	sendJSON(t, conn, map[string]any{
		"op": "showlog", "projectId": "p1", "search": true, "logFile": "search.log",
		"keyword": "no-such-keyword", "head": 0, "tail": 5, "first": false, "beforeCount": 0, "afterCount": 0,
	})
	frames = readUntil(t, conn, 5*time.Second, func(frame string) bool {
		return strings.Contains(frame, "搜索到并显示")
	})
	joined = strings.Join(frames, "\n")
	if !strings.Contains(joined, "在 5 行中搜索到并显示 0 行") {
		t.Fatalf("unexpected last-lines search summary: %s", joined)
	}
}
