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

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/sysinfo"
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
