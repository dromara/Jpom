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
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/script"
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
	if !strings.Contains(frame, "\"command\":\"updateNode\"") || !strings.Contains(frame, "\"completeSize\":4") {
		t.Fatalf("unexpected progress frame: %s", frame)
	}
	// Java UploadFileModel: size/completeSize are fastjson2 long -> JSON numbers,
	// savePath is agentConfig.getTempPath() (<data>/temp/<yyyy-MM-dd>) and the
	// getFilePath() getter is serialized as the filePath property.
	var progress map[string]any
	if err := json.Unmarshal([]byte(frame), &progress); err != nil {
		t.Fatalf("progress frame is not json: %s", frame)
	}
	data, _ := progress["data"].(map[string]any)
	if data == nil {
		t.Fatalf("progress data missing: %s", frame)
	}
	if _, ok := data["size"].(float64); !ok {
		t.Fatalf("size must be a JSON number: %v", data["size"])
	}
	if _, ok := data["completeSize"].(float64); !ok {
		t.Fatalf("completeSize must be a JSON number: %v", data["completeSize"])
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
