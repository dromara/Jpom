/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package script

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"jpom.io/go-agent/internal/app"
)

func testApp(t *testing.T) *app.App {
	t.Helper()
	base := t.TempDir()
	dataDir := filepath.Join(base, "data")
	instance := &app.App{
		BaseDir:           base,
		DataDir:           dataDir,
		ScriptDir:         filepath.Join(dataDir, "script"),
		GlobalScriptDir:   filepath.Join(dataDir, "global-script"),
		ScriptRunCacheDir: filepath.Join(dataDir, "script_run_cache"),
	}
	for _, dir := range []string{instance.DataDir, instance.ScriptDir, instance.GlobalScriptDir, instance.ScriptRunCacheDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatalf("mkdir %s: %v", dir, err)
		}
	}
	return instance
}

func mustJSON(t *testing.T, value any) string {
	t.Helper()
	encoded, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	return string(encoded)
}

func TestConvertToParam(t *testing.T) {
	legacy := ConvertToParam("a b c")
	var params []CommandParam
	if err := json.Unmarshal([]byte(legacy), &params); err != nil {
		t.Fatalf("legacy convert: %v (%s)", err, legacy)
	}
	if len(params) != 3 || params[0].Value != "a" || params[2].Desc != "参数3" {
		t.Fatalf("unexpected legacy params: %+v", params)
	}
	jsonArray := mustJSON(t, []CommandParam{{Value: "x", Desc: "d"}})
	if got := ConvertToParam(jsonArray); got != jsonArray {
		t.Fatalf("json array must be kept, got %s", got)
	}
	if got := ConvertToParam(""); got != "[]" {
		t.Fatalf("empty must become [], got %s", got)
	}
}

func TestToCommandList(t *testing.T) {
	if got := ToCommandList(""); len(got) != 0 {
		t.Fatalf("empty args: %v", got)
	}
	got := ToCommandList(mustJSON(t, []CommandParam{{Value: "1", Desc: "a"}, {Value: "2"}}))
	if len(got) != 2 || got[0] != "1" || got[1] != "2" {
		t.Fatalf("json args: %v", got)
	}
	legacy := ToCommandList("one two")
	if len(legacy) != 2 || legacy[1] != "two" {
		t.Fatalf("legacy args: %v", legacy)
	}
}

func TestTriggerParamMapKeepsUpperCase(t *testing.T) {
	params, err := TriggerParamMap(mustJSON(t, map[string]string{"BUILD_ID": "12", "buildName": "demo"}))
	if err != nil {
		t.Fatal(err)
	}
	if params["trigger_BUILD_ID"] != "12" {
		t.Fatalf("upper param lost: %v", params)
	}
	if params["trigger_build_name"] != "demo" {
		t.Fatalf("camel param not underlined: %v", params)
	}
	if _, err := TriggerParamMap("{bad"); err == nil {
		t.Fatal("malformed params must return an error")
	}
}

func TestWorkspaceEnvPrivacy(t *testing.T) {
	instance := testApp(t)
	payload := map[string]any{
		"DEFAULT": map[string]any{
			"varData": map[string]any{
				"TEST_PWD": map[string]any{"name": "TEST_PWD", "value": "1234"},
				"PLAIN":    map[string]any{"name": "PLAIN", "value": "1", "privacy": 0},
			},
		},
	}
	if err := os.WriteFile(filepath.Join(instance.DataDir, "workspace_env_var.json"), []byte(mustJSON(t, payload)), 0o644); err != nil {
		t.Fatal(err)
	}
	manager := NewManager(instance)
	builder, err := manager.WorkspaceEnv("DEFAULT")
	if err != nil {
		t.Fatal(err)
	}
	values := builder.Environment()
	if values["TEST_PWD"] != "1234" || values["PLAIN"] != "1" {
		t.Fatalf("environment values: %v", values)
	}
	var lines []string
	builder.EachStr(func(line string) { lines = append(lines, line) })
	joined := strings.Join(lines, "\n")
	if !strings.Contains(joined, "TEST_PWD=******") {
		t.Fatalf("privacy var must be masked: %s", joined)
	}
	if !strings.Contains(joined, "PLAIN=1") {
		t.Fatalf("public var must be visible: %s", joined)
	}
}

func TestScriptCRUDAndLibrary(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	item := map[string]any{"id": "s1", "name": "n", "context": "echo hi", "workspaceId": "DEFAULT"}
	if err := manager.Add(item); err != nil {
		t.Fatal(err)
	}
	if err := manager.Add(item); err == nil {
		t.Fatal("duplicate id must fail")
	}
	list, err := manager.List()
	if err != nil || len(list) != 1 || list[0]["name"] != "n" {
		t.Fatalf("list: %v %v", list, err)
	}
	if err := manager.UpdateFields("s1", map[string]any{"name": "n2"}); err != nil {
		t.Fatal(err)
	}
	got, exists, err := manager.Get("s1")
	if err != nil || !exists || got["name"] != "n2" || got["context"] != "echo hi" {
		t.Fatalf("get after update: %v %v %v", got, exists, err)
	}
	if err := manager.Delete("s1"); err != nil {
		t.Fatal(err)
	}
	if _, exists, _ := manager.Get("s1"); exists {
		t.Fatal("record must be deleted")
	}
}

func TestReferenceReplace(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	if err := manager.LibrarySave("test", "echo library", "", "1#abc"); err != nil {
		t.Fatal(err)
	}
	source := "ls" + "\n" + "G@(" + string(rune(34)) + "test" + string(rune(34)) + ")"
	replaced, err := manager.ReferenceReplace(source)
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(replaced, "echo library") {
		t.Fatalf("reference not replaced: %s", replaced)
	}
	list, err := manager.LibraryList()
	if err != nil || len(list) != 1 {
		t.Fatalf("library list: %v %v", list, err)
	}
	if _, ok := list[0]["script"]; ok {
		t.Fatal("library list must not return script content")
	}
	if list[0]["tag"] != "test" {
		t.Fatalf("tag must be file name: %v", list[0])
	}
	missing := "G@(" + string(rune(34)) + "missing" + string(rune(34)) + ")"
	if _, err := manager.ReferenceReplace(missing); err == nil {
		t.Fatal("missing library must fail")
	}
}

func TestExecutorRunsAndWritesLog(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	executor := NewExecutor(manager)
	item := map[string]any{"id": "s1", "name": "test", "context": "echo hello", "workspaceId": "DEFAULT"}
	executeID := NewExecutionID()
	if err := executor.Start(item, executeID, "", nil, "zh-CN"); err != nil {
		t.Fatal(err)
	}
	deadline := time.Now().Add(10 * time.Second)
	for executor.IsRun(executeID) && time.Now().Before(deadline) {
		time.Sleep(20 * time.Millisecond)
	}
	if executor.IsRun(executeID) {
		t.Fatalf("execution did not finish")
	}
	total, lines, err := ReadLogFile(manager.LogFile("s1", executeID), 1)
	if err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(lines, "\n")
	if !strings.Contains(joined, "hello") {
		t.Fatalf("log must contain output: %s", joined)
	}
	if !strings.Contains(joined, "[SYSTEM-INFO] 执行结束:0") {
		t.Fatalf("log must contain completion line: %s", joined)
	}
	if total != len(lines) {
		t.Fatalf("total %d != lines %d", total, len(lines))
	}
}

func TestExecutorEnglishMessages(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	executor := NewExecutor(manager)
	item := map[string]any{"id": "s3", "name": "en", "context": "echo en-output", "workspaceId": "DEFAULT"}
	executeID := NewExecutionID()
	if err := executor.Start(item, executeID, "", nil, "en-US"); err != nil {
		t.Fatal(err)
	}
	deadline := time.Now().Add(10 * time.Second)
	for executor.IsRun(executeID) && time.Now().Before(deadline) {
		time.Sleep(20 * time.Millisecond)
	}
	_, lines, err := ReadLogFile(manager.LogFile("s3", executeID), 1)
	if err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(lines, string(rune(10)))
	if !strings.Contains(joined, "[SYSTEM-INFO] End of execution: 0") {
		t.Fatalf("english completion line missing: %s", joined)
	}
}

func TestReadLogFileAllowsLineZero(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	if err := os.MkdirAll(manager.LogDir("s4"), 0o755); err != nil {
		t.Fatal(err)
	}
	path := manager.LogFile("s4", "e1")
	if err := os.WriteFile(path, []byte("a"+string(rune(10))+"b"+string(rune(10))), 0o644); err != nil {
		t.Fatal(err)
	}
	total, lines, err := ReadLogFile(path, 0)
	if err != nil {
		t.Fatal(err)
	}
	if total != 2 || len(lines) != 2 || lines[0] != "a" {
		t.Fatalf("line=0 must read from the beginning: total=%d lines=%v", total, lines)
	}
}

func TestExecutorAttachReceivesLiveOutput(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	executor := NewExecutor(manager)
	item := map[string]any{"id": "s2", "name": "sleep", "context": "echo first" + "\n" + "sleep 0.2" + "\n" + "echo second", "workspaceId": "DEFAULT"}
	executeID := NewExecutionID()
	var received []string
	send := func(line string) error {
		received = append(received, line)
		return nil
	}
	if err := executor.Attach(item, executeID, "", "session-1", send, "zh-CN"); err != nil {
		t.Fatal(err)
	}
	deadline := time.Now().Add(10 * time.Second)
	for executor.IsRun(executeID) && time.Now().Before(deadline) {
		time.Sleep(20 * time.Millisecond)
	}
	joined := strings.Join(received, "\n")
	if !strings.Contains(joined, "first") || !strings.Contains(joined, "second") {
		t.Fatalf("listener must receive output: %s", joined)
	}
	if !strings.Contains(joined, "JPOM_MSG") {
		t.Fatalf("listener must receive completion message: %s", joined)
	}
}

func TestLibraryListDirectoryOrder(t *testing.T) {
	instance := testApp(t)
	manager := NewManager(instance)
	// 创建顺序与文件名排序不同，验证按目录遍历顺序输出（Java File.listFiles 语义）
	for _, id := range []string{"zzz", "aaa", "mmm"} {
		if err := manager.LibrarySave(id, "echo "+id, "", ""); err != nil {
			t.Fatal(err)
		}
	}
	handle, err := os.Open(manager.GlobalScriptDir())
	if err != nil {
		t.Fatal(err)
	}
	names, err := handle.Readdirnames(-1)
	_ = handle.Close()
	if err != nil {
		t.Fatal(err)
	}
	expected := make([]string, 0, len(names))
	for _, name := range names {
		if strings.EqualFold(filepath.Ext(name), ".json") {
			expected = append(expected, strings.TrimSuffix(name, filepath.Ext(name)))
		}
	}
	list, err := manager.LibraryList()
	if err != nil {
		t.Fatal(err)
	}
	actual := make([]string, 0, len(list))
	for _, item := range list {
		actual = append(actual, stringField(item, "tag"))
	}
	if strings.Join(actual, ",") != strings.Join(expected, ",") {
		t.Fatalf("library list must keep directory order: expected %v, got %v", expected, actual)
	}
}
