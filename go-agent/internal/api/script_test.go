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
	"net/http/httptest"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/config"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/sysinfo"
)

const apiTestAuthorize = "api-test-digest"

func apiTestApp(t *testing.T) *app.App {
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
		Auth:              app.Auth{Name: "test", Authorize: apiTestAuthorize},
	}
	for _, dir := range []string{instance.DataDir, instance.LogsDir, instance.TmpDir, instance.ProjectLogDir,
		instance.ScriptDir, instance.GlobalScriptDir, instance.ScriptRunCacheDir, instance.TempDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	return instance
}

// scriptTestServer builds a router with the script endpoints only.
func scriptTestServer(t *testing.T) (*httptest.Server, *app.App) {
	t.Helper()
	instance := apiTestApp(t)
	router := httpx.New(instance)
	registerScript(router, instance)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)
	return server, instance
}

type apiEnvelope struct {
	Code int             `json:"code"`
	Msg  string          `json:"msg"`
	Data json.RawMessage `json:"data"`
}

// postForm posts a form and decodes the envelope.
func postForm(t *testing.T, server *httptest.Server, path string, form url.Values) apiEnvelope {
	t.Helper()
	request, err := http.NewRequest(http.MethodPost, server.URL+path, strings.NewReader(form.Encode()))
	if err != nil {
		t.Fatal(err)
	}
	request.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	request.Header.Set("Jpom-Agent-Authorize", apiTestAuthorize)
	request.Header.Set("workspaceId", "DEFAULT")
	request.Header.Set("Jpom-Server-UserName", "devadmin")
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode != http.StatusOK {
		t.Fatalf("%s: unexpected http status %d", path, response.StatusCode)
	}
	envelope := apiEnvelope{}
	if err := json.NewDecoder(response.Body).Decode(&envelope); err != nil {
		t.Fatalf("%s: decode response: %v", path, err)
	}
	return envelope
}

// postJSON posts a JSON body and decodes the envelope.
func postJSON(t *testing.T, server *httptest.Server, path string, body any) apiEnvelope {
	t.Helper()
	encoded, err := json.Marshal(body)
	if err != nil {
		t.Fatal(err)
	}
	request, err := http.NewRequest(http.MethodPost, server.URL+path, strings.NewReader(string(encoded)))
	if err != nil {
		t.Fatal(err)
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Jpom-Agent-Authorize", apiTestAuthorize)
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = response.Body.Close() }()
	envelope := apiEnvelope{}
	if err := json.NewDecoder(response.Body).Decode(&envelope); err != nil {
		t.Fatalf("%s: decode response: %v", path, err)
	}
	return envelope
}

func TestScriptEndpointsUnauthorized(t *testing.T) {
	instance := apiTestApp(t)
	router := httpx.New(instance)
	registerScript(router, instance)
	server := httptest.NewServer(router)
	defer server.Close()
	request, err := http.NewRequest(http.MethodPost, server.URL+"/script/list.json", strings.NewReader(""))
	if err != nil {
		t.Fatal(err)
	}
	request.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = response.Body.Close() }()
	envelope := apiEnvelope{}
	if err := json.NewDecoder(response.Body).Decode(&envelope); err != nil {
		t.Fatal(err)
	}
	if envelope.Code != 900 || !strings.Contains(envelope.Msg, "授权信息错误") {
		t.Fatalf("unexpected unauthorized envelope: %+v", envelope)
	}
}

func TestScriptSaveListItemDel(t *testing.T) {
	server, instance := scriptTestServer(t)
	// save validation
	envelope := postForm(t, server, "/script/save.json", url.Values{"type": {"add"}, "context": {""}})
	if envelope.Code != 405 || envelope.Msg != "内容为空" {
		t.Fatalf("unexpected save validation: %+v", envelope)
	}
	// add
	form := url.Values{
		"type":         {"add"},
		"name":         {"测试脚本"},
		"context":      {"echo hello-api"},
		"autoExecCron": {""},
		"defArgs":      {"a b c"},
		"description":  {"demo"},
		"global":       {"false"},
		"nodeId":       {"NODE01"},
	}
	envelope = postForm(t, server, "/script/save.json", form)
	if envelope.Code != 200 || envelope.Msg != "添加成功" {
		t.Fatalf("unexpected add envelope: %+v", envelope)
	}
	// list
	envelope = postForm(t, server, "/script/list.json", url.Values{})
	var list []map[string]any
	if err := json.Unmarshal(envelope.Data, &list); err != nil {
		t.Fatalf("decode list: %v (%s)", err, envelope.Data)
	}
	if len(list) != 1 {
		t.Fatalf("expected one script, got %v", list)
	}
	id, _ := list[0]["id"].(string)
	if list[0]["lastRunUser"] != "-" {
		t.Fatalf("lastRunUser getter default must be -: %v", list[0]["lastRunUser"])
	}
	if id == "" || list[0]["name"] != "测试脚本" || list[0]["workspaceId"] != "DEFAULT" {
		t.Fatalf("unexpected script record: %v", list[0])
	}
	// defArgs normalized to JSON array
	defArgs, _ := list[0]["defArgs"].(string)
	if !strings.Contains(defArgs, "参数1") {
		t.Fatalf("defArgs not normalized: %s", defArgs)
	}
	// 落盘数据必须与 Java add 一致：不写 lastRunUser / scriptType（读取时 getter 回退 "-"）。
	rawData, err := os.ReadFile(filepath.Join(instance.DataDir, "script.json"))
	if err != nil {
		t.Fatalf("read script.json: %v", err)
	}
	var scriptRecords map[string]map[string]any
	if err := json.Unmarshal(rawData, &scriptRecords); err != nil {
		t.Fatalf("decode script.json: %v", err)
	}
	record, ok := scriptRecords[id]
	if !ok {
		t.Fatalf("script %s not persisted: %v", id, scriptRecords)
	}
	if _, exists := record["lastRunUser"]; exists {
		t.Errorf("add must not persist lastRunUser, got %v", record["lastRunUser"])
	}
	if scriptType, exists := record["scriptType"]; exists && scriptType != "" {
		t.Errorf("add must not persist scriptType, got %v", scriptType)
	}
	// item
	envelope = postForm(t, server, "/script/item.json", url.Values{"id": {id}})
	var item map[string]any
	if err := json.Unmarshal(envelope.Data, &item); err != nil {
		t.Fatalf("decode item: %v", err)
	}
	if item["context"] != "echo hello-api" {
		t.Fatalf("unexpected item: %v", item)
	}
	// change workspace
	envelope = postForm(t, server, "/script/change-workspace-id", url.Values{
		"id": {id}, "newWorkspaceId": {"WS2"}, "newNodeId": {"NODE02"},
	})
	if envelope.Code != 200 || envelope.Msg != "修改成功" {
		t.Fatalf("unexpected change workspace: %+v", envelope)
	}
	// execute
	envelope = postForm(t, server, "/script/exec", url.Values{"id": {id}})
	if envelope.Code != 200 || envelope.Msg != "开始执行" {
		t.Fatalf("unexpected exec envelope: %+v", envelope)
	}
	var executeID string
	if err := json.Unmarshal(envelope.Data, &executeID); err != nil || executeID == "" {
		t.Fatalf("exec must return execute id: %v (%s)", err, envelope.Data)
	}
	// poll the incremental log endpoint until the execution finished
	deadline := time.Now().Add(10 * time.Second)
	var logData map[string]any
	for time.Now().Before(deadline) {
		envelope = postForm(t, server, "/script/log", url.Values{"id": {id}, "executeId": {executeID}, "line": {"1"}})
		if envelope.Code != 200 {
			t.Fatalf("unexpected log envelope: %+v", envelope)
		}
		if err := json.Unmarshal(envelope.Data, &logData); err != nil {
			t.Fatalf("decode log: %v", err)
		}
		run, _ := logData["run"].(bool)
		lines, _ := logData["dataLines"].([]any)
		if !run && len(lines) > 0 {
			break
		}
		time.Sleep(20 * time.Millisecond)
	}
	// line=0 也必须可读（Java 实测允许；getLine 回显 0）
	logZero := postForm(t, server, "/script/log", url.Values{"id": {id}, "executeId": {executeID}, "line": {"0"}})
	if logZero.Code != 200 {
		t.Fatalf("line=0 must be accepted: %+v", logZero)
	}
	var zeroData map[string]any
	if err := json.Unmarshal(logZero.Data, &zeroData); err != nil {
		t.Fatal(err)
	}
	if zeroData["getLine"].(float64) != 0 {
		t.Fatalf("getLine must echo the requested line 0, got %v", zeroData["getLine"])
	}
	if zeroLines, _ := zeroData["dataLines"].([]any); len(zeroLines) == 0 {
		t.Fatalf("line=0 must return the log lines")
	}
	lines, _ := logData["dataLines"].([]any)
	text := ""
	for _, line := range lines {
		text += line.(string) + string(rune(10))
	}
	if !strings.Contains(text, "hello-api") || !strings.Contains(text, "[SYSTEM-INFO] 执行结束:0") {
		t.Fatalf("unexpected execution log: %s", text)
	}
	if _, err := os.Stat(filepath.Join(instance.ScriptDir, id, "log", executeID+".log")); err != nil {
		t.Fatalf("execution log file missing: %v", err)
	}
	// pull trigger execution log and delete it
	envelope = postForm(t, server, "/script/pull_exec_log", url.Values{"pullCount": {"100"}})
	var records []map[string]any
	if err := json.Unmarshal(envelope.Data, &records); err != nil {
		t.Fatalf("decode exec logs: %v", err)
	}
	if len(records) != 1 || records[0]["scriptId"] != id || records[0]["triggerExecType"].(float64) != 2 {
		t.Fatalf("unexpected exec log records: %v", records)
	}
	if _, ok := records[0]["createTimeMillis"].(string); !ok {
		t.Fatalf("createTimeMillis must be serialized as string (J1): %v", records[0]["createTimeMillis"])
	}
	recordID, _ := records[0]["id"].(string)
	envelope = postJSON(t, server, "/script/del_exec_log", map[string]any{"ids": []string{recordID}})
	if envelope.Code != 200 {
		t.Fatalf("unexpected del exec log: %+v", envelope)
	}
	// del_log
	envelope = postForm(t, server, "/script/del_log", url.Values{"id": {id}, "executeId": {executeID}})
	if envelope.Code != 200 || envelope.Msg != "删除成功" {
		t.Fatalf("unexpected del_log: %+v", envelope)
	}
	if _, err := os.Stat(filepath.Join(instance.ScriptDir, id, "log", executeID+".log")); !os.IsNotExist(err) {
		t.Fatalf("log file must be deleted")
	}
	// del script
	envelope = postForm(t, server, "/script/del.json", url.Values{"id": {id}})
	if envelope.Code != 200 || envelope.Msg != "删除成功" {
		t.Fatalf("unexpected del: %+v", envelope)
	}
	envelope = postForm(t, server, "/script/list.json", url.Values{})
	if err := json.Unmarshal(envelope.Data, &list); err != nil {
		t.Fatal(err)
	}
	if len(list) != 0 {
		t.Fatalf("script must be deleted: %v", list)
	}
}

func TestScriptLibraryEndpoints(t *testing.T) {
	server, instance := scriptTestServer(t)
	envelope := postForm(t, server, "/script-library/save", url.Values{
		"id": {"test"}, "script": {"echo library-content"}, "description": {"demo"}, "version": {"1#abc"},
	})
	if envelope.Code != 200 || envelope.Msg != "保存成功" {
		t.Fatalf("unexpected library save: %+v", envelope)
	}
	if _, err := os.Stat(filepath.Join(instance.GlobalScriptDir, "test.json")); err != nil {
		t.Fatalf("library file missing: %v", err)
	}
	envelope = postForm(t, server, "/script-library/list", url.Values{})
	var list []map[string]any
	if err := json.Unmarshal(envelope.Data, &list); err != nil {
		t.Fatalf("decode library list: %v", err)
	}
	if len(list) != 1 || list[0]["tag"] != "test" || list[0]["version"] != "1#abc" {
		t.Fatalf("unexpected library list: %v", list)
	}
	if _, ok := list[0]["script"]; ok {
		t.Fatalf("library list must not include the script body: %v", list[0])
	}
	envelope = postForm(t, server, "/script-library/get", url.Values{"id": {"test"}})
	var item map[string]any
	if err := json.Unmarshal(envelope.Data, &item); err != nil {
		t.Fatal(err)
	}
	if item["script"] != "echo library-content" {
		t.Fatalf("unexpected library item: %v", item)
	}
	envelope = postForm(t, server, "/script-library/get", url.Values{"id": {"missing"}})
	if envelope.Code != 405 || !strings.Contains(envelope.Msg, "找不到对应的脚本") {
		t.Fatalf("unexpected missing library: %+v", envelope)
	}
	envelope = postForm(t, server, "/script-library/del", url.Values{"id": {"test"}})
	if envelope.Code != 200 || envelope.Msg != "删除成功" {
		t.Fatalf("unexpected library del: %+v", envelope)
	}
}

func TestScriptSaveSyncAndEdit(t *testing.T) {
	server, _ := scriptTestServer(t)
	form := url.Values{
		"type": {"add"}, "id": {"fixed-id"}, "name": {"n1"}, "context": {"echo one"},
		"autoExecCron": {""}, "defArgs": {"[]"},
	}
	envelope := postForm(t, server, "/script/save.json", form)
	if envelope.Code != 200 {
		t.Fatalf("add failed: %+v", envelope)
	}
}

// TestScriptCronSecondLevel verifies the autoExecCron wiring into the unified
// sysinfo scheduler: with jpom.system.timer-match-second=true a "*/1 * * * * ?"
// script must be executed automatically and the task must be visible in
// sysinfo.CronList (/system/cache.taskList).
func TestScriptCronSecondLevel(t *testing.T) {
	instance := apiTestApp(t)
	instance.Config.Jpom.System.TimerMatchSecond = true
	// prepare the script before the cron registration done by registerScript
	manager := runtimeFor(instance).manager
	item := map[string]any{
		"id": "cron-int", "name": "cron-int", "context": "echo cron-integration",
		"workspaceId": "DEFAULT", "autoExecCron": "*/1 * * * * ?", "defArgs": "[]",
	}
	if err := manager.Add(item); err != nil {
		t.Fatal(err)
	}
	sysinfo.Start(instance)
	router := httpx.New(instance)
	registerScript(router, instance)
	server := httptest.NewServer(router)
	defer server.Close()

	deadline := time.Now().Add(10 * time.Second)
	var records []map[string]any
	for time.Now().Before(deadline) {
		list, err := manager.ListExecLogs()
		if err != nil {
			t.Fatal(err)
		}
		filtered := make([]map[string]any, 0, len(list))
		for _, record := range list {
			if record["scriptId"] == "cron-int" {
				filtered = append(filtered, record)
			}
		}
		if len(filtered) > 0 {
			records = filtered
			break
		}
		time.Sleep(100 * time.Millisecond)
	}
	if len(records) == 0 {
		t.Fatal("autoExecCron did not trigger the script")
	}
	if fmt.Sprint(records[0]["triggerExecType"]) != "1" {
		t.Fatalf("unexpected triggerExecType: %v", records[0]["triggerExecType"])
	}
	// taskList must contain the "script:<id>" task with the original expression
	found := false
	for _, task := range sysinfo.CronList() {
		if task["taskId"] == "script:cron-int" {
			found = true
			if task["cron"] != "*/1 * * * * ?" {
				t.Fatalf("unexpected task cron: %v", task["cron"])
			}
		}
	}
	if !found {
		t.Fatalf("script cron task not registered in sysinfo taskList: %v", sysinfo.CronList())
	}
	// clean the global scheduler
	_ = sysinfo.RemoveCronTask("script:cron-int")
	_ = manager.Delete("cron-int")
}

// TestScriptCronGroupsAndYear verifies the Hutool cron extensions required by
// G4: "|" OR groups and the 7 field year expression are registered as-is
// (taskList keeps the raw expression) and only matching tasks trigger.
func TestScriptCronGroupsAndYear(t *testing.T) {
	instance := apiTestApp(t)
	instance.Config.Jpom.System.TimerMatchSecond = true
	manager := runtimeFor(instance).manager
	orExpr := "*/1 * * * * ? | 0 0 0 1 1 ?"
	yearExpr := "*/1 * * * * ? 2026"
	staleYearExpr := "*/1 * * * * ? 2025"
	scripts := []map[string]any{
		{"id": "cron-g", "name": "g", "context": "echo g", "workspaceId": "DEFAULT", "autoExecCron": orExpr, "defArgs": "[]"},
		{"id": "cron-y", "name": "y", "context": "echo y", "workspaceId": "DEFAULT", "autoExecCron": yearExpr, "defArgs": "[]"},
		{"id": "cron-y0", "name": "y0", "context": "echo y0", "workspaceId": "DEFAULT", "autoExecCron": staleYearExpr, "defArgs": "[]"},
	}
	for _, item := range scripts {
		if err := manager.Add(item); err != nil {
			t.Fatal(err)
		}
	}
	sysinfo.Start(instance)
	router := httpx.New(instance)
	registerScript(router, instance)
	server := httptest.NewServer(router)
	defer server.Close()

	rawCrons := map[string]string{}
	for _, task := range sysinfo.CronList() {
		if taskID, ok := task["taskId"].(string); ok {
			rawCrons[taskID] = fmt.Sprint(task["cron"])
		}
	}
	if rawCrons["script:cron-g"] != orExpr {
		t.Fatalf("taskList must keep the raw | expression: %v", rawCrons["script:cron-g"])
	}
	if rawCrons["script:cron-y"] != yearExpr {
		t.Fatalf("taskList must keep the raw 7 field expression: %v", rawCrons["script:cron-y"])
	}

	deadline := time.Now().Add(10 * time.Second)
	triggered := map[string]int{}
	for time.Now().Before(deadline) {
		list, err := manager.ListExecLogs()
		if err != nil {
			t.Fatal(err)
		}
		triggered = map[string]int{}
		for _, record := range list {
			scriptID := fmt.Sprint(record["scriptId"])
			triggered[scriptID]++
		}
		if triggered["cron-g"] > 0 && triggered["cron-y"] > 0 {
			break
		}
		time.Sleep(100 * time.Millisecond)
	}
	if triggered["cron-g"] == 0 {
		t.Fatal("OR group expression did not trigger")
	}
	if triggered["cron-y"] == 0 {
		t.Fatal("7 field year expression did not trigger for the current year")
	}
	if triggered["cron-y0"] != 0 {
		t.Fatalf("expression with a mismatched year must not trigger: %v", triggered["cron-y0"])
	}
	for _, id := range []string{"cron-g", "cron-y", "cron-y0"} {
		_ = sysinfo.RemoveCronTask("script:" + id)
		_ = manager.Delete(id)
	}
}

// TestScriptParamValidationCodes locks the N1 error codes: @ValidatorItem
// failures are HTTP body code 400 (custom i18n message first), business Assert
// stays 405, malformed request bodies are 417.
func TestScriptParamValidationCodes(t *testing.T) {
	server, _ := scriptTestServer(t)
	cases := []struct {
		path string
		form url.Values
		code int
		msg  string
	}{
		{"/script/log", url.Values{}, 400, "日志ID缺失"},
		{"/script/log", url.Values{"id": {"x"}}, 400, "执行ID缺失"},
		{"/script/log", url.Values{"id": {"x"}, "executeId": {"y"}}, 400, "行号不正确"},
		{"/script/del_log", url.Values{}, 400, "日志ID缺失"},
		{"/script/exec", url.Values{}, 400, "参数验证失败"},
		{"/script/change-workspace-id", url.Values{}, 400, "参数验证失败"},
		{"/script-library/get", url.Values{}, 400, "参数验证失败"},
		{"/script-library/save", url.Values{}, 400, "参数验证失败"},
		{"/script-library/save", url.Values{"id": {"x"}}, 400, "脚本内容不能为空"},
		{"/script-library/del", url.Values{}, 400, "参数验证失败"},
		{"/script/pull_exec_log", url.Values{"pullCount": {"abc"}}, 500, "服务异常"},
		{"/script/pull_exec_log", url.Values{}, 400, "参数验证失败"},
		{"/script/pull_exec_log", url.Values{"pullCount": {"0"}}, 405, "pull count error"},
	}
	for _, item := range cases {
		envelope := postForm(t, server, item.path, item.form)
		if envelope.Code != item.code || !strings.Contains(envelope.Msg, item.msg) {
			t.Fatalf("%s %v: expected code=%d msg contains %q, got code=%d msg=%q", item.path, item.form, item.code, item.msg, envelope.Code, envelope.Msg)
		}
	}
	// malformed JSON request body -> 417 incorrect_parameter_format
	request, err := http.NewRequest(http.MethodPost, server.URL+"/script/del_exec_log", strings.NewReader("{bad"))
	if err != nil {
		t.Fatal(err)
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Jpom-Agent-Authorize", apiTestAuthorize)
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = response.Body.Close() }()
	envelope := apiEnvelope{}
	if err := json.NewDecoder(response.Body).Decode(&envelope); err != nil {
		t.Fatal(err)
	}
	if envelope.Code != 417 || !strings.Contains(envelope.Msg, "传入的参数格式不正确") {
		t.Fatalf("malformed JSON must be 417, got code=%d msg=%q", envelope.Code, envelope.Msg)
	}
}

// TestScriptExecMalformedParams locks the NPE/运行时分支：params 不是合法 JSON 时
// Java JSONObject.parseObject 抛 JSONException -> code 500 服务异常。
func TestScriptExecMalformedParams(t *testing.T) {
	server, _ := scriptTestServer(t)
	form := url.Values{"type": {"add"}, "name": {"p"}, "context": {"echo x"}, "autoExecCron": {""}, "defArgs": {"[]"}}
	if envelope := postForm(t, server, "/script/save.json", form); envelope.Code != 200 {
		t.Fatalf("add script failed: %+v", envelope)
	}
	envelope := postForm(t, server, "/script/list.json", url.Values{})
	var list []map[string]any
	if err := json.Unmarshal(envelope.Data, &list); err != nil || len(list) != 1 {
		t.Fatalf("list script failed: %v %v", err, list)
	}
	id, _ := list[0]["id"].(string)
	envelope = postForm(t, server, "/script/exec", url.Values{"id": {id}, "params": {"{bad"}})
	if envelope.Code != 500 || !strings.Contains(envelope.Msg, "服务异常") {
		t.Fatalf("malformed params must be 500 服务异常, got code=%d msg=%q", envelope.Code, envelope.Msg)
	}
	// no execution record must be created when params parsing fails
	envelope = postForm(t, server, "/script/pull_exec_log", url.Values{"pullCount": {"10"}})
	var records []map[string]any
	if err := json.Unmarshal(envelope.Data, &records); err != nil {
		t.Fatal(err)
	}
	if len(records) != 0 {
		t.Fatalf("no execution record expected: %v", records)
	}
}

// TestScriptNullFieldSemantics locks the Java Jackson NON_NULL / model defaults
// observed by the E2E round:
//   - add 分支不 setAutoExecCron/setDefArgs/setNodeId：缺参字段为 null -> 输出省略
//   - edit/sync 分支始终 setDefArgs(...)：缺参 -> "[]"；edit 始终 setAutoExecCron -> ""
func TestScriptNullFieldSemantics(t *testing.T) {
	server, _ := scriptTestServer(t)
	// add without autoExecCron/nodeId/defArgs (same payload as the E2E driver)
	form := url.Values{"type": {"add"}, "name": {"null-semantics"}, "context": {"echo n"}}
	if envelope := postForm(t, server, "/script/save.json", form); envelope.Code != 200 {
		t.Fatalf("add failed: %+v", envelope)
	}
	envelope := postForm(t, server, "/script/list.json", url.Values{})
	var list []map[string]any
	if err := json.Unmarshal(envelope.Data, &list); err != nil || len(list) != 1 {
		t.Fatalf("list failed: %v %v", err, list)
	}
	id, _ := list[0]["id"].(string)
	item := getScriptItem(t, server, id)
	for _, key := range []string{"autoExecCron", "nodeId", "defArgs"} {
		if _, ok := item[key]; ok {
			t.Fatalf("add must omit null field %q, got %v", key, item[key])
		}
	}
	if item["lastRunUser"] != "-" {
		t.Fatalf("lastRunUser getter default must be -, got %v", item["lastRunUser"])
	}
	if item["workspaceId"] != "DEFAULT" {
		t.Fatalf("workspaceId must come from the request header, got %v", item["workspaceId"])
	}
	// update (edit) without defArgs/autoExecCron -> defArgs [] and autoExecCron ""
	form = url.Values{"id": {id}, "name": {"null-semantics-2"}, "context": {"echo n2"}}
	if envelope := postForm(t, server, "/script/save.json", form); envelope.Code != 200 {
		t.Fatalf("update failed: %+v", envelope)
	}
	item = getScriptItem(t, server, id)
	if item["defArgs"] != "[]" {
		t.Fatalf("edit must default defArgs to [], got %v", item["defArgs"])
	}
	if value, ok := item["autoExecCron"]; !ok || value != "" {
		t.Fatalf("edit must set autoExecCron to empty string, got %v (present=%v)", value, ok)
	}
	if _, ok := item["nodeId"]; ok {
		t.Fatalf("edit must not invent nodeId: %v", item["nodeId"])
	}
	// sync without defArgs -> defArgs [] + scriptType server-sync
	form = url.Values{"type": {"sync"}, "id": {id}, "name": {"null-semantics-3"}, "context": {"echo n3"}}
	if envelope := postForm(t, server, "/script/save.json", form); envelope.Code != 200 {
		t.Fatalf("sync failed: %+v", envelope)
	}
	item = getScriptItem(t, server, id)
	if item["defArgs"] != "[]" || item["scriptType"] != "server-sync" {
		t.Fatalf("sync must default defArgs [] and scriptType server-sync, got %v / %v", item["defArgs"], item["scriptType"])
	}
}

func getScriptItem(t *testing.T, server *httptest.Server, id string) map[string]any {
	t.Helper()
	envelope := postForm(t, server, "/script/item.json", url.Values{"id": {id}})
	item := map[string]any{}
	if err := json.Unmarshal(envelope.Data, &item); err != nil {
		t.Fatalf("decode item: %v", err)
	}
	return item
}
