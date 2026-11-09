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
	"log/slog"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"os/exec"
	"path/filepath"
	"reflect"
	"strings"
	"testing"
	"time"
	"unicode/utf8"

	"jpom.io/go-agent/internal/config"
	"jpom.io/go-agent/internal/proc"
)

func newTestRuntime(t *testing.T) (*Runtime, string) {
	t.Helper()
	base := t.TempDir()
	if err := os.MkdirAll(filepath.Join(base, "data"), 0o755); err != nil {
		t.Fatalf("创建 data 目录失败: %v", err)
	}
	cfg := config.Default()
	cfg.Jpom.Project.StatusWaitTime = 1
	cfg.Jpom.Project.StatusDetectionInterval = 10
	cfg.Jpom.Project.Log.AutoBackupToFile = true
	return NewRuntime(base, cfg, slog.Default()), base
}

func writeWhitelist(t *testing.T, runtime *Runtime, dirs ...string) {
	t.Helper()
	if err := WriteWhitelist(runtime.Options.BaseDir, Whitelist{Project: dirs}); err != nil {
		t.Fatalf("写入授权目录失败: %v", err)
	}
}

func saveJarProject(t *testing.T, runtime *Runtime, root, id, lib string) {
	t.Helper()
	params := SaveParams{
		Values: map[string]any{
			"id":                 id,
			"name":               "测试项目" + id,
			"runMode":            RunModeJar,
			"whitelistDirectory": root,
			"lib":                lib,
			"jvm":                "-Xmx64m",
			"args":               "--server.port=9999",
			"autoStart":          "true",
			"token":              "",
		},
		WorkspaceID: "DEFAULT",
		UserName:    "tester",
	}
	if _, err := runtime.SaveProject(params); err != nil {
		t.Fatalf("保存项目失败: %v", err)
	}
}

func TestValidateProjectID(t *testing.T) {
	cases := []struct {
		id      string
		wantErr bool
	}{
		{"AB", false},
		{"6KA03M", false},
		{"A-1", false}, // 中划线会被替换为下划线后再校验
		{"A", true},
		{"a b", true},
		{"中文", true},
		{"system", true},
	}
	for _, item := range cases {
		err := ValidateProjectID(item.id)
		if item.wantErr && err == nil {
			t.Errorf("id=%q 期望失败但通过", item.id)
		}
		if !item.wantErr && err != nil {
			t.Errorf("id=%q 期望通过但失败: %v", item.id, err)
		}
	}
}

func TestOpResultOfMsg(t *testing.T) {
	result := OpResultOfMsg("running:1234,5678:8080")
	if !result.Success || result.Pid == nil || *result.Pid != 1234 {
		t.Fatalf("解析 running 状态失败: %+v", result)
	}
	if len(result.Pids) != 2 || result.Pids[1] != 5678 {
		t.Fatalf("pids 解析错误: %+v", result.Pids)
	}
	if result.Ports == nil || *result.Ports != "8080" {
		t.Fatalf("ports 解析错误: %+v", result.Ports)
	}
	data := result.JSON()
	if data["pids"] == nil || data["ports"] != "8080" || data["statusMsg"] != "running:1234,5678:8080" {
		t.Fatalf("JSON 输出错误: %+v", data)
	}
	if _, ok := data["pid"]; !ok {
		t.Fatalf("JSON 应包含 pid: %+v", data)
	}

	stopped := OpResultOfMsg("stopped")
	if stopped.Success || stopped.Pid == nil || *stopped.Pid != 0 {
		t.Fatalf("解析 stopped 状态错误: %+v", stopped)
	}
	data = stopped.JSON()
	if _, ok := data["ports"]; ok {
		t.Fatalf("stopped 不应包含 ports: %+v", data)
	}
	if _, ok := data["pids"]; ok {
		t.Fatalf("stopped 不应包含 pids: %+v", data)
	}
	if msgs, ok := data["msgs"].([]string); !ok || len(msgs) != 1 || msgs[0] != "stopped" {
		t.Fatalf("msgs 输出错误: %+v", data["msgs"])
	}
}

func TestParseDataSize(t *testing.T) {
	cases := map[string]int64{
		"50MB": 50 * 1024 * 1024,
		"1KB":  1024,
		"1 GB": 1024 * 1024 * 1024,
		"1024": 1024,
	}
	for input, want := range cases {
		if got := ParseDataSize(input); got != want {
			t.Errorf("ParseDataSize(%q)=%d, want %d", input, got, want)
		}
	}
}

func TestBuildRunCommand(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	libDir := filepath.Join(root, "app")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	for _, name := range []string{"app.jar", "lib.jar"} {
		if err := os.WriteFile(filepath.Join(libDir, name), []byte("jar"), 0o644); err != nil {
			t.Fatal(err)
		}
	}
	info := map[string]any{
		"id":                 "TEST01",
		"name":               "测试",
		"runMode":            RunModeJar,
		"whitelistDirectory": root,
		"lib":                "/app",
		"jvm":                "-Xmx64m",
		"args":               "--server.port=9999",
	}
	command := runtime.BuildRunCommand(info)
	if !strings.HasPrefix(command, "nohup java -Xmx64m -DJpom.application=TEST01 -DJpom.basedir=") {
		t.Fatalf("启动命令前缀错误: %s", command)
	}
	if !strings.Contains(command, "-jar "+filepath.Join(libDir, "app.jar")) {
		t.Fatalf("启动命令缺少 -jar: %s", command)
	}
	if !strings.Contains(command, "--server.port=9999") || !strings.HasSuffix(command, "2>&1 &") {
		t.Fatalf("启动命令参数错误: %s", command)
	}

	// ClassPath 模式应拼接全部 jar 并追加 mainClass
	classPath := map[string]any{
		"id":                 "TEST02",
		"runMode":            RunModeClassPath,
		"whitelistDirectory": root,
		"lib":                "/app",
		"mainClass":          "com.example.Main",
	}
	command = runtime.BuildRunCommand(classPath)
	if !strings.Contains(command, "-classpath "+filepath.Join(libDir, "app.jar")+":"+filepath.Join(libDir, "lib.jar")) {
		t.Fatalf("ClassPath 命令错误: %s", command)
	}
	if !strings.Contains(command, "com.example.Main") {
		t.Fatalf("ClassPath 命令缺少 mainClass: %s", command)
	}

	// JavaExtDirsCp 模式
	extDirs := map[string]any{
		"id":                 "TEST03",
		"runMode":            RunModeJavaExtDirsCp,
		"whitelistDirectory": root,
		"lib":                "/app",
		"mainClass":          "com.example.Main",
		"javaExtDirsCp":      "lib:conf",
	}
	command = runtime.BuildRunCommand(extDirs)
	if !strings.Contains(command, "-Djava.ext.dirs=lib -cp conf:") {
		t.Fatalf("JavaExtDirsCp 命令错误: %s", command)
	}

	// 没有任何 jar 时应返回空
	emptyInfo := map[string]any{
		"id":                 "TEST04",
		"runMode":            RunModeJar,
		"whitelistDirectory": root,
		"lib":                "/empty",
	}
	if err := os.MkdirAll(filepath.Join(root, "empty"), 0o755); err != nil {
		t.Fatal(err)
	}
	if command := runtime.BuildRunCommand(emptyInfo); command != "" {
		t.Fatalf("无 jar 时应返回空命令: %s", command)
	}
}

func TestSaveAndDeleteProject(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)

	saveJarProject(t, runtime, root, "PROJ01", "/app")
	info, ok, err := runtime.Manager.Get("PROJ01")
	if err != nil || !ok || info == nil {
		t.Fatalf("项目未写入: ok=%v err=%v", ok, err)
	}
	if GetString(info, "createTime") == "" || GetString(info, "runMode") != RunModeJar {
		t.Fatalf("项目字段错误: %+v", info)
	}
	if GetString(info, "whitelistDirectory") != root {
		t.Fatalf("授权目录字段错误: %+v", info)
	}

	// 修改项目
	params := SaveParams{
		Values: map[string]any{
			"id":                 "PROJ01",
			"name":               "修改后的项目",
			"runMode":            RunModeJar,
			"whitelistDirectory": root,
			"lib":                "/app",
			"jvm":                "-Xmx128m",
		},
		WorkspaceID: "DEFAULT",
		UserName:    "修改人",
	}
	if msg, err := runtime.SaveProject(params); err != nil || msg != "修改成功" {
		t.Fatalf("修改项目失败: msg=%s err=%v", msg, err)
	}
	info, _, _ = runtime.Manager.Get("PROJ01")
	if GetString(info, "name") != "修改后的项目" || GetString(info, "modifyUser") != "修改人" || GetString(info, "modifyTime") == "" {
		t.Fatalf("修改字段错误: %+v", info)
	}

	// 预检查（已存在项目）不应修改数据
	preview := params
	preview.Values = Clone(params.Values)
	preview.Values["name"] = "预检查名称"
	preview.Preview = true
	if msg, err := runtime.SaveProject(preview); err != nil || msg != "检查通过" {
		t.Fatalf("预检查失败: msg=%s err=%v", msg, err)
	}
	previewInfo, _, _ := runtime.Manager.Get("PROJ01")
	if GetString(previewInfo, "name") == "预检查名称" {
		t.Fatal("预检查不应修改项目数据")
	}

	// 路径冲突
	conflict := cloneValues(params.Values)
	conflict["id"] = "PROJ03"
	conflict["name"] = "冲突项目"
	if _, err := runtime.SaveProject(SaveParams{Values: conflict, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil ||
		!strings.Contains(err.Error(), "项目路径和") {
		t.Fatalf("路径冲突检查失败: %v", err)
	}

	// 越级路径
	slip := cloneValues(params.Values)
	slip["id"] = "PROJ04"
	slip["lib"] = "../escape"
	if _, err := runtime.SaveProject(SaveParams{Values: slip, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil ||
		!strings.Contains(err.Error(), "提升目录问题") {
		t.Fatalf("越级路径检查失败: %v", err)
	}

	// 释放分发 / 修改工作空间
	if _, err := runtime.ReleaseOutGiving("PROJ01", "tester"); err != nil {
		t.Fatalf("释放分发失败: %v", err)
	}
	if _, err := runtime.ChangeWorkspaceID("PROJ01", "WS01", "NODE01", "tester"); err != nil {
		t.Fatalf("修改工作空间失败: %v", err)
	}
	info, _, _ = runtime.Manager.Get("PROJ01")
	if GetString(info, "workspaceId") != "WS01" || GetString(info, "nodeId") != "NODE01" {
		t.Fatalf("工作空间字段错误: %+v", info)
	}

	// thoroughly 删除
	logFile, err := runtime.Manager.ResolveLogFile(info)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Dir(logFile), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(logFile, []byte("console log"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "app", "app.jar"), []byte("jar"), 0o644); err != nil {
		t.Fatal(err)
	}
	if msg, err := runtime.DeleteProject("PROJ01", "true"); err != nil || msg != "删除成功！" {
		t.Fatalf("彻底删除失败: msg=%s err=%v", msg, err)
	}
	if _, ok, _ := runtime.Manager.Get("PROJ01"); ok {
		t.Fatal("项目未删除")
	}
	if _, err := os.Stat(filepath.Join(root, "app")); !os.IsNotExist(err) {
		t.Fatal("彻底删除应删除项目目录")
	}
	if _, err := os.Stat(logFile); !os.IsNotExist(err) {
		t.Fatal("彻底删除应删除日志文件")
	}
	if msg, _ := runtime.DeleteProject("PROJ01", ""); msg != "项目不存在" {
		t.Fatalf("删除不存在项目应返回成功消息: %s", msg)
	}
}

func cloneValues(values map[string]any) map[string]any {
	return Clone(values)
}

func TestProjectLogOperations(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	saveJarProject(t, runtime, root, "LOG001", "/app")
	info, _, _ := runtime.Manager.Get("LOG001")

	logFile, err := runtime.Manager.ResolveLogFile(info)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Dir(logFile), 0o755); err != nil {
		t.Fatal(err)
	}
	// 小于 100KB 不备份
	if err := os.WriteFile(logFile, []byte("small log"), 0o644); err != nil {
		t.Fatal(err)
	}
	if msg := runtime.BackLog(info); msg != "ok" {
		t.Fatalf("小日志清空失败: %s", msg)
	}
	logBack, err := runtime.Manager.ResolveLogBack(info)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := os.Stat(logBack); !os.IsNotExist(err) {
		t.Fatal("小日志不应产生备份")
	}

	// 大日志触发备份
	big := strings.Repeat("0123456789", 20*1024)
	if err := os.WriteFile(logFile, []byte(big), 0o644); err != nil {
		t.Fatal(err)
	}
	if msg := runtime.BackLog(info); msg != "ok" {
		t.Fatalf("大日志备份失败: %s", msg)
	}
	entries, err := os.ReadDir(logBack)
	if err != nil || len(entries) != 1 {
		t.Fatalf("日志备份目录错误: len=%d err=%v", len(entries), err)
	}
	if strings.HasSuffix(entries[0].Name(), ".log") == false {
		t.Fatalf("备份文件名错误: %s", entries[0].Name())
	}

	// logSize：日志文件已被清空
	logBackExists, size, err := runtime.LogSize(info)
	if err != nil {
		t.Fatalf("logSize 失败: %v", err)
	}
	if !logBackExists {
		t.Fatal("logBack 目录应存在")
	}
	if size != "" {
		t.Fatalf("日志清空后大小应为空: %s", size)
	}
	if err := os.WriteFile(logFile, []byte(big), 0o644); err != nil {
		t.Fatal(err)
	}
	_, size, _ = runtime.LogSize(info)
	if size == "" || !strings.Contains(size, "KB") {
		t.Fatalf("日志大小格式化错误: %s", size)
	}

	// logBack 列表（long 字段输出字符串）
	list, err := runtime.LogBackList(info)
	if err != nil {
		t.Fatalf("logBack 列表失败: %v", err)
	}
	array, ok := list["array"].([]map[string]any)
	if !ok || len(array) != 1 {
		t.Fatalf("日志备份列表错误: %+v", list)
	}
	if _, ok := array[0]["fileSizeLong"].(string); !ok {
		t.Fatalf("fileSizeLong 应为字符串: %+v", array[0])
	}
	if GetString(array[0], "filename") == "" {
		t.Fatalf("filename 缺失: %+v", array[0])
	}

	// 目录穿越防护
	if _, err := runtime.LogBackDownloadFile(info, "../data/project.json"); err == nil {
		t.Fatal("应拒绝目录穿越下载")
	}
	if err := runtime.LogBackDelete(info, "../../x"); err == nil {
		t.Fatal("应拒绝目录穿越删除")
	}

	// 正常删除
	if err := runtime.LogBackDelete(info, entries[0].Name()); err != nil {
		t.Fatalf("删除日志备份失败: %v", err)
	}

	// resetLog
	msg, code, err := runtime.ResetLog(info)
	if err != nil {
		t.Fatalf("重置日志失败: %v", err)
	}
	if code != 200 || msg != "重置成功" {
		t.Fatalf("重置日志失败: code=%d msg=%s", code, msg)
	}
	if stat, err := os.Stat(logFile); err != nil || stat.Size() != 0 {
		t.Fatalf("重置后日志应为空: %v", err)
	}
	exportFile, err := runtime.ExportLogFile(info)
	if err != nil || !strings.HasSuffix(exportFile, "LOG001.log") {
		t.Fatalf("导出日志路径错误: %s err=%v", exportFile, err)
	}
}

const dslYaml = "description: 测试\n" +
	"run:\n" +
	"  start:\n" +
	"    scriptId: start.sh\n" +
	"    scriptArgs: start\n" +
	"  status:\n" +
	"    scriptId: status.sh\n" +
	"  stop:\n" +
	"    scriptId: stop.sh\n" +
	"  reload:\n" +
	"    scriptId: reload.sh\n"

func TestDslProcessInfoAndStatus(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	libDir := filepath.Join(root, "dsl")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	// 项目目录脚本
	for _, name := range []string{"start.sh", "stop.sh", "reload.sh"} {
		if err := os.WriteFile(filepath.Join(libDir, name), []byte("#!/bin/bash\necho "+name+"\n"), 0o755); err != nil {
			t.Fatal(err)
		}
	}
	// status.sh 输出 Java 约定的状态串
	statusScript := "#!/bin/bash\necho running:4321:8080\n"
	if err := os.WriteFile(filepath.Join(libDir, "status.sh"), []byte(statusScript), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	// 节点脚本
	scriptJSON := "{\"NODE001\":{\"id\":\"NODE001\",\"name\":\"节点脚本\",\"context\":\"echo node-script\\n\",\"workspaceId\":\"DEFAULT\"}}"
	if err := os.WriteFile(filepath.Join(base, "data", "script.json"), []byte(scriptJSON), 0o644); err != nil {
		t.Fatal(err)
	}
	values := map[string]any{
		"id":                 "DSL001",
		"name":               "DSL项目",
		"runMode":            RunModeDsl,
		"whitelistDirectory": root,
		"lib":                "/dsl",
		"dslContent":         dslYaml,
	}
	if _, err := runtime.SaveProject(SaveParams{Values: values, WorkspaceID: "DEFAULT", UserName: "tester"}); err != nil {
		t.Fatalf("保存 DSL 项目失败: %v", err)
	}
	info, _, _ := runtime.Manager.Get("DSL001")

	canReload, list, err := runtime.DslProcessInfo(info)
	if err != nil {
		t.Fatalf("解析 DSL 流程失败: %v", err)
	}
	if !canReload || len(list) != 4 {
		t.Fatalf("DSL 流程信息错误: canReload=%v len=%d", canReload, len(list))
	}
	for _, item := range list {
		if item["process"] == nil {
			t.Fatalf("DSL 流程缺少 process 字段: %+v", item)
		}
		if GetString(item, "process") == OpRestart {
			if GetBool(item, "status") || GetString(item, "msg") != "流程不存在" {
				t.Fatalf("未配置 restart 流程时应返回流程不存在: %+v", item)
			}
			continue
		}
		if GetBool(item, "status") != true {
			t.Fatalf("DSL 流程脚本解析失败: %+v", item)
		}
	}
	if GetString(list[0], "type") != "file" {
		t.Fatalf("start 应为项目文件脚本: %+v", list[0])
	}

	// DSL status 脚本执行
	result := runtime.Status(info)
	if !result.Success || result.Pid == nil || *result.Pid != 4321 {
		t.Fatalf("DSL 状态解析错误: %+v", result)
	}
	if result.Ports == nil || *result.Ports != "8080" {
		t.Fatalf("DSL 状态端口错误: %+v", result.Ports)
	}
	logFile, _ := runtime.Manager.ResolveLogFile(info)
	content, err := os.ReadFile(logFile)
	if err != nil || !strings.Contains(string(content), "[status] - running:4321:8080") {
		t.Fatalf("DSL 状态日志格式错误: %s err=%v", string(content), err)
	}

	// DSL 异步 start 写日志（带 SYSTEM-INFO 与格式行）
	if err := runtime.dslRun(info, info, OpStart, true); err != nil {
		t.Fatalf("执行 DSL start 失败: %v", err)
	}
	content, _ = os.ReadFile(logFile)
	text := string(content)
	if !strings.Contains(text, "[SYSTEM-INFO] 开始执行: start") || !strings.Contains(text, "[start] - ") {
		t.Fatalf("DSL 执行日志格式错误: %s", text)
	}
}

// TestGoldenProjectInfo 用 dev 环境 Java 插件端真实抓取的黄金响应校验 /manage/getProjectInfo 数据结构。
//
// 参考数据只在工作区 .tmp/go-agent-ref 存在（未入库），缺失时跳过，避免 CI 失败。
func TestGoldenProjectInfo(t *testing.T) {
	refDir := filepath.Join("..", "..", "..", "..", "go-agent-ref")
	dataFile := filepath.Join(refDir, "data", "project.json")
	goldenFile := filepath.Join(refDir, "golden", "project-info.json")
	if _, err := os.Stat(dataFile); err != nil {
		t.Skip("缺少真实参考数据 .tmp/go-agent-ref，跳过黄金响应校验")
	}
	base := t.TempDir()
	if err := os.MkdirAll(filepath.Join(base, "data"), 0o755); err != nil {
		t.Fatal(err)
	}
	content, err := os.ReadFile(dataFile)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(base, "data", "project.json"), content, 0o644); err != nil {
		t.Fatal(err)
	}
	runtime := NewRuntime(base, config.Default(), slog.Default())
	list, err := runtime.Manager.List()
	if err != nil {
		t.Fatal(err)
	}
	actualJSON, err := json.Marshal(list)
	if err != nil {
		t.Fatal(err)
	}
	var actual []map[string]any
	if err := json.Unmarshal(actualJSON, &actual); err != nil {
		t.Fatal(err)
	}
	goldenContent, err := os.ReadFile(goldenFile)
	if err != nil {
		t.Fatal(err)
	}
	var golden struct {
		Data []map[string]any "json:\"data\""
	}
	if err := json.Unmarshal(goldenContent, &golden); err != nil {
		t.Fatal(err)
	}
	if len(actual) != len(golden.Data) {
		t.Fatalf("项目数量不一致: go=%d java=%d", len(actual), len(golden.Data))
	}
	for index := range golden.Data {
		want := golden.Data[index]
		got := actual[index]
		if GetString(got, "id") != GetString(want, "id") {
			t.Fatalf("第 %d 个项目顺序不一致: go=%s java=%s", index, GetString(got, "id"), GetString(want, "id"))
		}
		if !reflect.DeepEqual(got, want) {
			t.Fatalf("项目 %s 字段与 Java 响应不一致:\ngo  =%#v\njava=%#v", GetString(got, "id"), got, want)
		}
	}
}

func TestWebHooks(t *testing.T) {
	runtime, _ := newTestRuntime(t)
	var query url.Values
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, request *http.Request) {
		query = request.URL.Query()
		_, _ = w.Write([]byte("webhook-ok"))
	}))
	defer server.Close()

	info := map[string]any{"id": "WH001", "name": "通知项目", "token": server.URL}
	body := runtime.WebHooks(info, "start", map[string]any{"result": map[string]any{"success": true, "pid": 123}})
	if body != "webhook-ok" {
		t.Fatalf("WebHooks 响应错误: %s", body)
	}
	if query.Get("projectId") != "WH001" || query.Get("projectName") != "通知项目" || query.Get("type") != "start" {
		t.Fatalf("WebHooks 参数错误: %v", query)
	}
	if !strings.Contains(query.Get("result"), "success") {
		t.Fatalf("WebHooks result 参数错误: %s", query.Get("result"))
	}
	if empty := runtime.WebHooks(map[string]any{"id": "WH002"}, "start", nil); empty != "" {
		t.Fatalf("空 token 不应发起请求: %s", empty)
	}
}

func TestResolveFileBackupConfig(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	dslDir := filepath.Join(root, "dsl-backup")
	if err := os.MkdirAll(dslDir, 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	backupPath := filepath.Join(base, "custom-backup")
	dslContent := "run:\n  status:\n    scriptId: status.sh\n" +
		"file:\n  backupCount: 3\n  backupSuffix: [ '.jar', '.txt' ]\n  backupPath: " + backupPath + "\n"
	values := map[string]any{
		"id":                 "BKP001",
		"name":               "备份项目",
		"runMode":            RunModeDsl,
		"whitelistDirectory": root,
		"lib":                "/dsl-backup",
		"dslContent":         dslContent,
	}
	if _, err := runtime.SaveProject(SaveParams{Values: values, WorkspaceID: "DEFAULT", UserName: "tester"}); err != nil {
		t.Fatalf("保存 DSL 项目失败: %v", err)
	}
	info, _, _ := runtime.Manager.Get("BKP001")
	count, suffix, path := runtime.ResolveFileBackupConfig(info)
	if count != 3 || path != backupPath || len(suffix) != 2 || suffix[1] != ".txt" {
		t.Fatalf("备份配置解析错误: count=%d suffix=%v path=%s", count, suffix, path)
	}
	dir, err := runtime.ProjectFileBackupDir(info)
	if err != nil || dir != filepath.Join(backupPath, "BKP001") {
		t.Fatalf("备份目录错误: %s err=%v", dir, err)
	}

	// 未配置 dsl file 时使用全局配置
	if err := os.MkdirAll(filepath.Join(root, "jar-backup"), 0o755); err != nil {
		t.Fatal(err)
	}
	saveJarProject(t, runtime, root, "BKP002", "/jar-backup")
	jarInfo, _, _ := runtime.Manager.Get("BKP002")
	count, suffix, path = runtime.ResolveFileBackupConfig(jarInfo)
	if count != runtime.Options.FileBackupCount || path != "" || len(suffix) != len(runtime.Options.FileBackupSuffix) {
		t.Fatalf("全局备份配置解析错误: count=%d suffix=%v path=%s", count, suffix, path)
	}
	dir, err = runtime.ProjectFileBackupDir(jarInfo)
	if err != nil || dir != filepath.Join(base, "data", "project_file_backup", "BKP002") {
		t.Fatalf("默认备份目录错误: %s err=%v", dir, err)
	}
}

// TestSaveBooleanFieldTypes 校验 project.json 布尔字段按 Java 模型类型落盘（不是字符串）。
func TestSaveBooleanFieldTypes(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "bool-app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	params := SaveParams{
		Values: map[string]any{
			"id":                 "BOOL01",
			"name":               "布尔字段项目",
			"runMode":            "Jar",
			"whitelistDirectory": root,
			"lib":                "/bool-app",
			"autoStart":          "false",
			"disableScanDir":     "true",
		},
		WorkspaceID: "DEFAULT",
		UserName:    "tester",
	}
	if _, err := runtime.SaveProject(params); err != nil {
		t.Fatalf("保存项目失败: %v", err)
	}
	raw, err := os.ReadFile(filepath.Join(base, "data", "project.json"))
	if err != nil {
		t.Fatal(err)
	}
	var all map[string]map[string]any
	if err := json.Unmarshal(raw, &all); err != nil {
		t.Fatal(err)
	}
	item := all["BOOL01"]
	if item == nil {
		t.Fatal("project.json 中没有新增项目")
	}
	for key, want := range map[string]bool{"autoStart": false, "disableScanDir": true, "outGivingProject": false} {
		value, ok := item[key].(bool)
		if !ok {
			t.Fatalf("字段 %s 应为布尔，实际为 %T(%v)", key, item[key], item[key])
		}
		if value != want {
			t.Fatalf("字段 %s 期望 %v，实际 %v", key, want, value)
		}
	}

	// 修改项目同样保持布尔类型
	update := Clone(params.Values)
	update["autoStart"] = "true"
	if _, err := runtime.SaveProject(SaveParams{Values: update, WorkspaceID: "DEFAULT", UserName: "tester"}); err != nil {
		t.Fatalf("修改项目失败: %v", err)
	}
	raw, _ = os.ReadFile(filepath.Join(base, "data", "project.json"))
	all = map[string]map[string]any{}
	if err := json.Unmarshal(raw, &all); err != nil {
		t.Fatal(err)
	}
	if value, ok := all["BOOL01"]["autoStart"].(bool); !ok || !value {
		t.Fatalf("修改后 autoStart 应为 true 布尔，实际 %T(%v)", all["BOOL01"]["autoStart"], all["BOOL01"]["autoStart"])
	}
}

// TestStartCommandWithoutSudo 校验项目启动命令不加 sudo（Java 仅 kill -9 兜底加 sudo）。
func TestStartCommandWithoutSudo(t *testing.T) {
	runtime, base := newTestRuntime(t)
	runtime.Options.CommandUseSudo = true // 即使配置开启 sudo，启动命令也不能加
	root := filepath.Join(base, "projects")
	libDir := filepath.Join(root, "sudo-app")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(libDir, "app.jar"), []byte("jar"), 0o644); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	saveJarProject(t, runtime, root, "SUDO01", "/sudo-app")
	info, _, _ := runtime.Manager.Get("SUDO01")

	var capturedSudo *bool
	var capturedCommand string
	original := asyncExec
	defer func() { asyncExec = original }()
	asyncExec = func(dir string, env map[string]string, command string, useSudo bool) error {
		value := useSudo
		capturedSudo = &value
		capturedCommand = command
		return nil
	}
	if err := runtime.execStartCommand(info, info); err != nil {
		t.Fatalf("执行启动命令失败: %v", err)
	}
	if capturedSudo == nil {
		t.Fatal("未捕获启动执行")
	}
	if *capturedSudo {
		t.Fatalf("项目启动命令不应加 sudo: %s", capturedCommand)
	}
	if strings.Contains(capturedCommand, "sudo ") {
		t.Fatalf("启动命令包含 sudo: %s", capturedCommand)
	}
	if !strings.Contains(capturedCommand, "nohup java") {
		t.Fatalf("启动命令未使用 java: %s", capturedCommand)
	}
}

// TestForceKillUsesSudo 校验 kill -9 兜底在配置开启时使用 sudo（Java hopeUseSudo=true 的唯一调用点）。
func TestForceKillUsesSudo(t *testing.T) {
	runtime, base := newTestRuntime(t)
	runtime.Options.CommandUseSudo = true
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "kill-app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	saveJarProject(t, runtime, root, "KILL01", "/kill-app")
	info, _, _ := runtime.Manager.Get("KILL01")

	// 启动一个忽略 SIGTERM、且命令行带 jpom 标识的假进程，用于触发 kill -9 兜底
	process := exec.Command("python3", "-c", "import signal,time; signal.signal(signal.SIGTERM, signal.SIG_IGN); time.sleep(120)", "-DJpom.application=KILL01")
	if err := process.Start(); err != nil {
		t.Skipf("当前环境无法启动 python 假进程: %v", err)
	}
	defer func() {
		_ = process.Process.Kill()
		_, _ = process.Process.Wait()
	}()
	pid := process.Process.Pid
	// 等待 python 安装 SIGTERM 忽略逻辑并让 /proc 可见
	time.Sleep(400 * time.Millisecond)
	matched := false
	for i := 0; i < 100; i++ {
		if strings.HasPrefix(proc.Status("KILL01"), "running:") {
			matched = true
			break
		}
		time.Sleep(20 * time.Millisecond)
	}
	if !matched {
		t.Skip("假进程未被状态探测识别，跳过 kill -9 兜底测试")
	}

	var sudoFlags []bool
	var commands []string
	original := asyncExec
	defer func() { asyncExec = original }()
	asyncExec = func(dir string, env map[string]string, command string, useSudo bool) error {
		sudoFlags = append(sudoFlags, useSudo)
		commands = append(commands, command)
		// 不真正加 sudo（测试环境通常没有免密 sudo），直接执行命令
		return proc.AsyncExe(dir, env, command, false)
	}
	result := runtime.stopJava(info, info, pid)
	if len(sudoFlags) == 0 {
		t.Fatalf("kill -9 兜底未执行（result=%+v commands=%v）", result, commands)
	}
	if !sudoFlags[len(sudoFlags)-1] {
		t.Fatalf("kill -9 兜底应使用 sudo（配置开启时）: %v", sudoFlags)
	}
	if !strings.Contains(commands[len(commands)-1], "kill -9") {
		t.Fatalf("兜底命令应为 kill -9: %s", commands[len(commands)-1])
	}
}

// TestProjectLogGBK 校验项目控制台日志按配置字符集编码写入与读取（G5）。
func TestProjectLogGBK(t *testing.T) {
	runtime, base := newTestRuntime(t)
	runtime.Options.ProjectLog.FileCharset = "GBK"
	runtime.Options.ConsoleCharset = "GBK"
	root := filepath.Join(base, "projects")
	libDir := filepath.Join(root, "gbk-dsl")
	if err := os.MkdirAll(libDir, 0o755); err != nil {
		t.Fatal(err)
	}
	// 脚本输出 GBK 编码的“中文”，验证 console charset 解码
	statusScript := "#!/bin/bash\nprintf '\\xd6\\xd0\\xce\\xc4'\n"
	if err := os.WriteFile(filepath.Join(libDir, "gbk-status.sh"), []byte(statusScript), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	dslContent := "run:\n  status:\n    scriptId: gbk-status.sh\n"
	values := map[string]any{
		"id":                 "GBK001",
		"name":               "GBK项目",
		"runMode":            "Dsl",
		"whitelistDirectory": root,
		"lib":                "/gbk-dsl",
		"dslContent":         dslContent,
	}
	if _, err := runtime.SaveProject(SaveParams{Values: values, WorkspaceID: "DEFAULT", UserName: "tester"}); err != nil {
		t.Fatalf("保存 GBK DSL 项目失败: %v", err)
	}
	info, _, _ := runtime.Manager.Get("GBK001")
	if charset := runtime.ResolveLogCharset(info); charset != "GBK" {
		t.Fatalf("项目日志字符集应为 GBK: %s", charset)
	}
	_ = runtime.Status(info)
	logFile, err := runtime.Manager.ResolveLogFile(info)
	if err != nil {
		t.Fatal(err)
	}
	raw, err := os.ReadFile(logFile)
	if err != nil {
		t.Fatal(err)
	}
	if utf8.Valid(raw) {
		t.Fatalf("GBK 日志文件不应是合法 UTF-8: %v", raw)
	}
	text, err := proc.DecodeString("GBK", raw)
	if err != nil || !strings.Contains(text, "[status] - 中文") {
		t.Fatalf("GBK 日志内容错误: %q err=%v", text, err)
	}
	// WS tail 复用的读取方法按 GBK 解码
	lines, err := runtime.TailLogLines(info, 1)
	if err != nil || len(lines) != 1 || !strings.Contains(lines[0], "中文") {
		t.Fatalf("TailLogLines GBK 解码错误: %#v err=%v", lines, err)
	}
	// 项目数据 logCharset 覆盖配置
	info["logCharset"] = "UTF-8"
	if charset := runtime.ResolveLogCharset(info); charset != "UTF-8" {
		t.Fatalf("项目 logCharset 应覆盖配置: %s", charset)
	}
}

func writeBrokenLinkFixture(t *testing.T, runtime *Runtime, root string) {
	t.Helper()
	broken := map[string]any{
		"id":                 "BROKEN1",
		"name":               "断链项目",
		"runMode":            "Link",
		"linkId":             "MISSING1",
		"workspaceId":        "DEFAULT",
		"createTime":         "2024-01-01 00:00:00",
		"logPath":            "",
		"mainClass":          "",
		"token":              "",
		"outGivingProject":   false,
		"javaExtDirsCp":      "",
		"whitelistDirectory": root,
		"lib":                "/broken",
	}
	if err := runtime.Manager.AddItem(broken, "tester"); err != nil {
		t.Fatalf("写入断链项目失败: %v", err)
	}
}

// TestBrokenLinkBoundaries 校验软化链项目不存在时各方法的异常语义（G8，Java 会抛 405）。
func TestBrokenLinkBoundaries(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	saveJarProject(t, runtime, root, "TARGET1", "/app")
	writeBrokenLinkFixture(t, runtime, root)
	info, _, _ := runtime.Manager.Get("BROKEN1")

	expectValidation := func(name string, err error) {
		t.Helper()
		if err == nil {
			t.Fatalf("%s 应返回错误", name)
		}
		if KindOf(err) != ErrValidation {
			t.Fatalf("%s 应为参数/状态异常(405)，实际 kind=%v err=%v", name, KindOf(err), err)
		}
		if !strings.Contains(err.Error(), "被软链的项目已经不存在啦") || !strings.Contains(err.Error(), "MISSING1") {
			t.Fatalf("%s 文案不符合 Java: %s", name, err.Error())
		}
	}
	expectValidation("StatusE", func() error { _, err := runtime.StatusE(info); return err }())
	expectValidation("IsRunE", func() error { _, err := runtime.IsRunE(info); return err }())
	expectValidation("Operate", func() error { _, err := runtime.Operate(OpStart, info); return err }())
	expectValidation("RunCommandE", func() error { _, err := runtime.RunCommandE(info); return err }())
	expectValidation("DeleteProject", func() error { _, err := runtime.DeleteProject("BROKEN1", ""); return err }())
	expectValidation("LogSize", func() error { _, _, err := runtime.LogSize(info); return err }())
	expectValidation("LogBackList", func() error { _, err := runtime.LogBackList(info); return err }())
	expectValidation("ExportLogFile", func() error { _, err := runtime.ExportLogFile(info); return err }())
	expectValidation("LogBackDelete", func() error { return runtime.LogBackDelete(info, "x.log") }())

	// resetLog 内部异常被捕获，返回 500（Java 文案 重置日志失败）
	msg, code, err := runtime.ResetLog(info)
	if err == nil || code != 500 || msg != "" {
		t.Fatalf("ResetLog 断链语义错误: msg=%q code=%d err=%v", msg, code, err)
	}
	// logBack_download 在 Java 中被 try/catch 转 400 下载失败
	// Java 在 try/catch 中调用，任意异常转为 400 下载失败（api 层处理）
	if _, err := runtime.LogBackDownloadFile(info, "x.log"); err == nil {
		t.Fatal("LogBackDownloadFile 断链应返回错误")
	}

	// 英文语言下错误文案应切换
	enRuntime := runtime.WithLang("en-US")
	_, enErr := enRuntime.StatusE(info)
	if enErr == nil {
		t.Fatal("英文场景应返回错误")
	}
	_, enMsg := LocalizeError(enRuntime.Lang(), enErr)
	if strings.Contains(enMsg, "软链") || !strings.Contains(strings.ToLower(enMsg), "soft-chained") {
		t.Fatalf("英文错误文案未切换: %s", enMsg)
	}
}

// TestSaveProjectErrorKinds 校验 saveProject 内部错误 405/500 分类（G8）。
func TestSaveProjectErrorKinds(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)

	baseValues := func(id string) map[string]any {
		return map[string]any{
			"id":                 id,
			"name":               id,
			"runMode":            "Jar",
			"whitelistDirectory": root,
			"lib":                "/app",
		}
	}
	// 参数错误 → 405
	invalidLib := baseValues("ERR001")
	invalidLib["lib"] = "../escape"
	if _, err := runtime.SaveProject(SaveParams{Values: invalidLib, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil || KindOf(err) != ErrValidation {
		t.Fatalf("非法路径应为 405: %v", err)
	}
	// 非法 WebHooks → 405
	badToken := baseValues("ERR002")
	badToken["token"] = "not-a-url"
	if _, err := runtime.SaveProject(SaveParams{Values: badToken, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil || KindOf(err) != ErrValidation {
		t.Fatalf("非法 WebHooks 应为 405: %v", err)
	}
	// DSL 缺少内容 → 405；DSL YAML 解析失败 → 500 服务异常
	dslMissing := baseValues("ERR003")
	dslMissing["runMode"] = "Dsl"
	if _, err := runtime.SaveProject(SaveParams{Values: dslMissing, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil || KindOf(err) != ErrValidation {
		t.Fatalf("DSL 缺少内容应为 405: %v", err)
	}
	dslBroken := baseValues("ERR004")
	dslBroken["runMode"] = "Dsl"
	dslBroken["dslContent"] = "run: [invalid"
	if _, err := runtime.SaveProject(SaveParams{Values: dslBroken, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil || KindOf(err) != ErrService {
		t.Fatalf("DSL YAML 解析失败应为 500: %v", err)
	}
	// 断链 Link 保存 → 405 且文案匹配 Java
	linkSave := map[string]any{
		"id":                 "ERR005",
		"name":               "ERR005",
		"runMode":            "Link",
		"linkId":             "MISSING1",
		"whitelistDirectory": root,
		"lib":                "/app",
	}
	if _, err := runtime.SaveProject(SaveParams{Values: linkSave, WorkspaceID: "DEFAULT", UserName: "tester"}); err == nil || KindOf(err) != ErrValidation {
		t.Fatalf("断链 Link 保存应为 405: %v", err)
	}
	// 修改已有项目切换工作空间 → 405
	saveJarProject(t, runtime, root, "ERR006", "/app2")
	if err := os.MkdirAll(filepath.Join(root, "app2"), 0o755); err != nil {
		t.Fatal(err)
	}
	values := baseValues("ERR006")
	values["lib"] = "/app2"
	if _, err := runtime.SaveProject(SaveParams{Values: values, WorkspaceID: "OTHER", UserName: "tester"}); err == nil || KindOf(err) != ErrValidation {
		t.Fatalf("跨工作空间修改应为 405: %v", err)
	}
}

// TestOperateI18n 校验 operate 业务结果文案按语言切换（zh-CN 默认与 en-US）。
func TestOperateI18n(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "file-app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	values := map[string]any{
		"id":                 "FILE01",
		"name":               "文件项目",
		"runMode":            "File",
		"whitelistDirectory": root,
		"lib":                "/file-app",
	}
	if _, err := runtime.SaveProject(SaveParams{Values: values, WorkspaceID: "DEFAULT", UserName: "tester"}); err != nil {
		t.Fatal(err)
	}
	info, _, _ := runtime.Manager.Get("FILE01")
	zhResult, err := runtime.Operate(OpStop, info)
	if err != nil {
		t.Fatal(err)
	}
	if len(zhResult.Msgs) == 0 || zhResult.Msgs[0] != "file 类型项目没有 stop" {
		t.Fatalf("中文 file 类型停止文案错误: %#v", zhResult.Msgs)
	}
	enResult, err := runtime.WithLang("en-US").Operate(OpStop, info)
	if err != nil {
		t.Fatal(err)
	}
	if len(enResult.Msgs) == 0 || enResult.Msgs[0] == "file 类型项目没有 stop" {
		t.Fatalf("英文 file 类型停止文案未切换: %#v", enResult.Msgs)
	}
}

// TestValidOp 校验 ConsoleCommandOp 枚举名解析（等价 Java EnumUtil.fromStringQuietly）。
func TestValidOp(t *testing.T) {
	for _, op := range []string{"start", "stop", "restart", "status", "reload", "showlog", "heart"} {
		if !ValidOp(op) {
			t.Fatalf("合法操作 %q 应返回 true", op)
		}
	}
	for _, op := range []string{"", "no-such-opt", "START", "kill"} {
		if ValidOp(op) {
			t.Fatalf("非法操作 %q 应返回 false", op)
		}
	}
	if !CanOpt("start") || !CanOpt("reload") || CanOpt("status") || CanOpt("heart") {
		t.Fatal("CanOpt 语义错误")
	}
}
