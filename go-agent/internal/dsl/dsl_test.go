/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package dsl

import (
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"jpom.io/go-agent/internal/app"
	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/project"
	"jpom.io/go-agent/internal/script"
)

func yamlText(lines ...string) string { return strings.Join(lines, string(rune(10))) }

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
		TempDir:           filepath.Join(dataDir, "temp"),
		Config:            config.Default(),
	}
	for _, dir := range []string{instance.DataDir, instance.ScriptDir, instance.GlobalScriptDir, instance.ScriptRunCacheDir, instance.TempDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	return instance
}

// sampleDSL mirrors the real agent dslContent (reference data).
func sampleDSL(scriptID string) string {
	return yamlText(
		"description: 测试",
		"run:",
		"  start:",
		"    scriptId: "+scriptID,
		"    scriptArgs: start",
		"    scriptEnv:",
		"      boot_active: test",
		"  status:",
		"    scriptId: "+scriptID,
		"    scriptArgs: status",
		"  stop:",
		"    scriptId: "+scriptID,
		"  execPath: ./",
		"  fileChangeReload: true",
		"file:",
		"  backupCount: 5",
		"  backupSuffix: ['.jar']",
		"config:",
		"  autoBackToFile: true",
	)
}

func TestParseDSLContent(t *testing.T) {
	content, err := Parse(sampleDSL("3e3862b4"))
	if err != nil {
		t.Fatal(err)
	}
	if content.Description != "测试" {
		t.Fatalf("description: %v", content.Description)
	}
	if !content.HasProcess("start") || !content.HasProcess("status") || !content.HasProcess("stop") {
		t.Fatalf("processes missing: %+v", content.Run)
	}
	if content.HasProcess("restart") || content.HasProcess("reload") {
		t.Fatalf("unexpected processes configured")
	}
	start := content.Process("start")
	if start.ScriptID != "3e3862b4" || start.ScriptArgs != "start" || start.ScriptEnv["boot_active"] != "test" {
		t.Fatalf("unexpected start process: %+v", start)
	}
	if content.Run.ExecPath != "./" || !content.Run.FileChangeReload {
		t.Fatalf("unexpected run options: %+v", content.Run)
	}
	if content.File.BackupCount != 5 || len(content.File.BackupSuffix) != 1 || content.File.BackupSuffix[0] != ".jar" {
		t.Fatalf("unexpected file config: %+v", content.File)
	}
	if !content.Config.AutoBackToFile {
		t.Fatalf("unexpected config: %+v", content.Config)
	}
	// 空内容
	if _, err := Parse("   "); err == nil {
		t.Fatal("empty dsl content must fail")
	}
	// snake_case keys are accepted as well
	snake, err := Parse(yamlText("run:", "  start:", "    script_id: abc", "  exec_path: ./x"))
	if err != nil {
		t.Fatal(err)
	}
	if snake.Process("start") == nil || snake.Process("start").ScriptID != "abc" || snake.Run.ExecPath != "./x" {
		t.Fatalf("snake_case keys not supported: %+v", snake.Run)
	}
}

func TestResolveScriptSources(t *testing.T) {
	instance := testApp(t)
	scriptManager := script.NewManager(instance)
	engine := NewEngine(scriptManager)
	libDir := filepath.Join(instance.BaseDir, "lib")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	original := map[string]any{
		"id": "p1", "name": "dsl", "workspaceId": "DEFAULT",
		"whitelistDirectory": instance.BaseDir, "lib": "lib",
	}
	// project file
	if err := os.WriteFile(filepath.Join(libDir, "run.sh"), []byte("echo file"), 0o644); err != nil {
		t.Fatal(err)
	}
	resolved, err := engine.Resolve(original, &Process{ScriptID: "run.sh"})
	if err != nil || resolved.Type != "file" || resolved.Temp {
		t.Fatalf("resolve project file: %+v %v", resolved, err)
	}
	// node script
	if err := scriptManager.Add(map[string]any{"id": "node-1", "name": "n", "context": "echo node", "workspaceId": "DEFAULT"}); err != nil {
		t.Fatal(err)
	}
	resolved, err = engine.Resolve(original, &Process{ScriptID: "node-1"})
	if err != nil || resolved.Type != "script" || !resolved.Temp {
		t.Fatalf("resolve node script: %+v %v", resolved, err)
	}
	_ = os.Remove(resolved.Path)
	// script library
	if err := scriptManager.LibrarySave("lib-tag", "echo library", "", ""); err != nil {
		t.Fatal(err)
	}
	resolved, err = engine.Resolve(original, &Process{ScriptID: "G@lib-tag"})
	if err != nil || resolved.Type != "library" || !resolved.Temp {
		t.Fatalf("resolve library: %+v %v", resolved, err)
	}
	_ = os.Remove(resolved.Path)
	// missing
	if _, err := engine.Resolve(original, &Process{ScriptID: "missing.sh"}); err == nil {
		t.Fatal("missing project file must fail")
	}
	if _, err := engine.Resolve(original, &Process{ScriptID: "G@missing"}); err == nil {
		t.Fatal("missing library must fail")
	}
	if _, err := engine.Resolve(original, &Process{ScriptID: ""}); err == nil {
		t.Fatal("empty scriptId must fail")
	}
}

// prepareRuntime writes project.json/lib script and returns the project runtime.
func prepareRuntime(t *testing.T, instance *app.App) (*project.Runtime, map[string]any, map[string]any, string) {
	t.Helper()
	libDir := filepath.Join(instance.BaseDir, "lib")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	scriptFile := filepath.Join(libDir, "demo.sh")
	if err := os.WriteFile(scriptFile, []byte("echo dsl-hello"), 0o644); err != nil {
		t.Fatal(err)
	}
	info := map[string]any{
		"id": "p1", "name": "dsl", "workspaceId": "DEFAULT", "runMode": "Dsl",
		"whitelistDirectory": instance.BaseDir, "lib": "lib",
		"dslContent": sampleDSL("demo.sh"),
	}
	encoded, err := json.Marshal(map[string]any{"p1": info})
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(instance.DataDir, "project.json"), encoded, 0o644); err != nil {
		t.Fatal(err)
	}
	runtime := project.NewRuntime(instance.BaseDir, instance.Config, instance.Log())
	loaded, exists, err := runtime.Manager.Get("p1")
	if err != nil || !exists {
		t.Fatalf("load project: %v %v", exists, err)
	}
	return runtime, loaded, loaded, scriptFile
}

func TestSyncRunProjectFile(t *testing.T) {
	instance := testApp(t)
	scriptManager := script.NewManager(instance)
	engine := NewEngine(scriptManager)
	runtime, info, original, _ := prepareRuntime(t, instance)
	exitCode, lines, err := engine.SyncRun(runtime, info, original, "start")
	if err != nil {
		t.Fatal(err)
	}
	if exitCode != 0 {
		t.Fatalf("unexpected exit code %d (lines %v)", exitCode, lines)
	}
	joined := strings.Join(lines, string(rune(10)))
	if !strings.Contains(joined, "[start] - dsl-hello") {
		t.Fatalf("formatted output missing: %s", joined)
	}
	if !strings.HasPrefix(lines[0], "") || !strings.Contains(lines[0], "本次执行退出码: 0") {
		t.Fatalf("exit code line must be first: %v", lines)
	}
	if !strings.Contains(lines[0], "["+"start"+"] - ") {
		t.Fatalf("exit code line must be formatted: %s", lines[0])
	}
}

func TestRunWritesProjectLog(t *testing.T) {
	instance := testApp(t)
	scriptManager := script.NewManager(instance)
	engine := NewEngine(scriptManager)
	runtime, info, original, _ := prepareRuntime(t, instance)
	if err := engine.Run(runtime, info, original, "start", false); err != nil {
		t.Fatal(err)
	}
	logFile, err := runtime.Manager.ResolveLogFile(info)
	if err != nil {
		t.Fatal(err)
	}
	content, err := os.ReadFile(logFile)
	if err != nil {
		t.Fatal(err)
	}
	text := string(content)
	if !strings.Contains(text, "[SYSTEM-INFO] 开始执行:start") {
		t.Fatalf("start line missing: %s", text)
	}
	if !strings.Contains(text, "[start] - dsl-hello") {
		t.Fatalf("formatted output missing: %s", text)
	}
	if !strings.Contains(text, "[SYSTEM-INFO] 执行结束: start 0") {
		t.Fatalf("end line missing: %s", text)
	}
}

func TestRunMissingProcess(t *testing.T) {
	instance := testApp(t)
	engine := NewEngine(script.NewManager(instance))
	runtime, info, original, _ := prepareRuntime(t, instance)
	if err := engine.Run(runtime, info, original, "restart", false); err == nil {
		t.Fatal("missing restart process must fail")
	}
	if _, _, err := engine.SyncRun(runtime, info, original, "reload"); err == nil {
		t.Fatal("missing reload process must fail")
	}
}

func TestRegisterIsIdempotent(t *testing.T) {
	instance := testApp(t)
	manager := script.NewManager(instance)
	Register(manager)
	Register(manager)
}
