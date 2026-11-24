//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package project

import (
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// TestBackLogKeepsFileWhenBackupFails 备份失败（back 目录不可写）时不得清空控制台日志。
//
// 与 Java 对齐：FileUtil.copy 抛异常会中断 checkStart，不会走到 emptyLogFile。
func TestBackLogKeepsFileWhenBackupFails(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	saveJarProject(t, runtime, root, "BKFAIL1", "/app")
	info, _, _ := runtime.Manager.Get("BKFAIL1")

	logFile, err := runtime.Manager.ResolveLogFile(info)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(filepath.Dir(logFile), 0o755); err != nil {
		t.Fatal(err)
	}
	big := strings.Repeat("0123456789", 20*1024)
	if err := os.WriteFile(logFile, []byte(big), 0o644); err != nil {
		t.Fatal(err)
	}
	// 用同名文件占住 back 目录：MkdirAll/复制必然失败
	backDir, err := runtime.Manager.ResolveLogBack(info)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(backDir, []byte("占位"), 0o644); err != nil {
		t.Fatal(err)
	}

	msg := runtime.BackLog(info)
	if msg == "ok" {
		t.Fatalf("备份失败时不应返回成功，实际: %s", msg)
	}
	content, err := os.ReadFile(logFile)
	if err != nil {
		t.Fatalf("备份失败后控制台日志应保留: %v", err)
	}
	if string(content) != big {
		t.Fatalf("备份失败后控制台日志内容被改动: len=%d want=%d", len(content), len(big))
	}

	// 反证：恢复可写后同样的日志应被备份并清空
	if err := os.Remove(backDir); err != nil {
		t.Fatal(err)
	}
	if msg := runtime.BackLog(info); msg != "ok" {
		t.Fatalf("可备份场景应成功，实际: %s", msg)
	}
	entries, err := os.ReadDir(backDir)
	if err != nil || len(entries) != 1 {
		t.Fatalf("应产生一个备份文件: len=%d err=%v", len(entries), err)
	}
	stat, err := os.Stat(logFile)
	if err != nil {
		t.Fatal(err)
	}
	if stat.Size() != 0 {
		t.Fatalf("备份成功后控制台日志应被清空，实际 %d 字节", stat.Size())
	}
}

// TestMigrateLegacyProjectLogs 旧版布局日志迁移进 <logDir>/back，并按 Java 规则跳过。
func TestMigrateLegacyProjectLogs(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	for _, dir := range []string{"app", "app2"} {
		if err := os.MkdirAll(filepath.Join(root, dir), 0o755); err != nil {
			t.Fatal(err)
		}
	}
	// 自定义 logPath 必须已授权
	customLogPath := filepath.Join(base, "customlog")
	if err := os.MkdirAll(customLogPath, 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root, customLogPath)
	saveJarProject(t, runtime, root, "MIG001", "app")

	if _, err := runtime.SaveProject(SaveParams{
		Values: map[string]any{
			"id":                 "MIG002",
			"name":               "自定义日志路径",
			"runMode":            RunModeJar,
			"whitelistDirectory": root,
			"lib":                "app2",
			"logPath":            customLogPath,
		},
		WorkspaceID: "DEFAULT",
		UserName:    "tester",
	}); err != nil {
		t.Fatalf("保存自定义日志路径项目失败: %v", err)
	}
	// 软链项目：不迁移
	if err := runtime.Manager.Mutate(func(values map[string]map[string]any) error {
		values["MIG003"] = map[string]any{
			"id":                 "MIG003",
			"name":               "软链项目",
			"runMode":            RunModeLink,
			"linkId":             "MIG001",
			"whitelistDirectory": root,
			"lib":                "app2",
		}
		return nil
	}); err != nil {
		t.Fatal(err)
	}

	legacyLog := filepath.Join(root, "MIG001.log")
	legacyBack := legacyLog + "_back"
	legacyContent := "old-layout-log\n"
	if err := os.WriteFile(legacyLog, []byte(legacyContent), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(legacyBack, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(legacyBack, "20260101000000.log"), []byte("backup"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "MIG002.log"), []byte("keep"), 0o644); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(root, "MIG003.log"), []byte("keep"), 0o644); err != nil {
		t.Fatal(err)
	}

	runtime.MigrateLegacyProjectLogs()

	info, _, _ := runtime.Manager.Get("MIG001")
	backDir, err := runtime.Manager.ResolveLogBack(info)
	if err != nil {
		t.Fatal(err)
	}
	migrated, err := os.ReadFile(filepath.Join(backDir, "MIG001.log"))
	if err != nil || string(migrated) != legacyContent {
		t.Fatalf("旧版日志未迁移到 back: content=%q err=%v", string(migrated), err)
	}
	if _, err := os.Stat(filepath.Join(backDir, "20260101000000.log")); err != nil {
		t.Fatalf("旧版备份目录内容未迁移: %v", err)
	}
	if _, err := os.Stat(legacyLog); !os.IsNotExist(err) {
		t.Fatal("迁移后旧版日志文件应被移除")
	}
	if _, err := os.Stat(legacyBack); !os.IsNotExist(err) {
		t.Fatal("迁移后旧版备份目录应被移除")
	}
	for _, id := range []string{"MIG002", "MIG003"} {
		if _, err := os.Stat(filepath.Join(root, id+".log")); err != nil {
			t.Fatalf("%s 不应被迁移: %v", id, err)
		}
	}

	// 幂等：再跑一次不报错、状态不变
	runtime.MigrateLegacyProjectLogs()
	if content, err := os.ReadFile(filepath.Join(backDir, "MIG001.log")); err != nil || string(content) != legacyContent {
		t.Fatalf("重复迁移破坏了内容: %q err=%v", string(content), err)
	}
}

// TestMigrateLegacyProjectLogsSkipsRunning 项目运行中时单个旧日志只告警不迁移（备份目录仍迁移）。
func TestMigrateLegacyProjectLogsSkipsRunning(t *testing.T) {
	runtime, base := newTestRuntime(t)
	root := filepath.Join(base, "projects")
	if err := os.MkdirAll(filepath.Join(root, "app"), 0o755); err != nil {
		t.Fatal(err)
	}
	writeWhitelist(t, runtime, root)
	saveJarProject(t, runtime, root, "MIGRUN1", "app")
	info, _, _ := runtime.Manager.Get("MIGRUN1")

	// 用子进程 cmdline 带上 Jpom 项目标识参数，模拟"项目正在运行"
	cmd := exec.Command("sh", "-c", "sleep 20", "-DJpom.application="+GetString(info, "id"))
	if err := cmd.Start(); err != nil {
		t.Skipf("无法构造运行中进程: %v", err)
	}
	defer func() {
		_ = cmd.Process.Kill()
		_, _ = cmd.Process.Wait()
	}()
	deadline := time.Now().Add(3 * time.Second)
	for !runtime.IsRun(info) {
		if time.Now().After(deadline) {
			t.Skip("无法让项目呈现运行中状态")
		}
		time.Sleep(50 * time.Millisecond)
	}

	legacyLog := filepath.Join(root, "MIGRUN1.log")
	if err := os.WriteFile(legacyLog, []byte("running"), 0o644); err != nil {
		t.Fatal(err)
	}
	legacyBack := legacyLog + "_back"
	if err := os.MkdirAll(legacyBack, 0o755); err != nil {
		t.Fatal(err)
	}

	runtime.MigrateLegacyProjectLogs()

	if _, err := os.Stat(legacyLog); err != nil {
		t.Fatalf("运行中项目的旧日志不应被迁移: %v", err)
	}
	backDir, err := runtime.Manager.ResolveLogBack(info)
	if err != nil {
		t.Fatal(err)
	}
	if stat, err := os.Stat(backDir); err != nil || !stat.IsDir() {
		t.Fatalf("旧备份目录仍应迁移（与 Java 一致）: %v", err)
	}
}
