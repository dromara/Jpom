/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package sysinfo

import (
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
)

// prepareUpgradeFixture 构造升级回滚单测的基础目录：当前可执行文件 + oldJars 备份。
func prepareUpgradeFixture(t *testing.T, attempts int, withBackup bool) (*app.App, *upgradePending) {
	t.Helper()
	a := newTestApp(t)
	if err := os.MkdirAll(filepath.Dir(a.Executable), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(a.Executable, []byte("new-binary"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.MkdirAll(a.OldJarsPath(), 0o755); err != nil {
		t.Fatal(err)
	}
	backupName := "agent-old-1"
	if withBackup {
		backupPath := filepath.Join(a.OldJarsPath(), backupName)
		if err := os.WriteFile(backupPath, []byte("old-binary"), 0o755); err != nil {
			t.Fatal(err)
		}
	}
	pending := &upgradePending{
		BeforeJar:     backupName,
		BeforeVersion: "2.11.12",
		NewVersion:    "2.11.13",
		UpdateTime:    time.Now().Format("2006-01-02 15:04:05"),
		Attempts:      attempts,
	}
	if err := writeUpgradePending(a, pending); err != nil {
		t.Fatal(err)
	}
	if err := writeJSONAtomic(upgradeStatePath(a), map[string]any{
		"beforeJar":    backupName,
		"newJar":       filepath.Base(a.Executable),
		"upgradeCount": 1,
		"status":       "installed",
	}); err != nil {
		t.Fatal(err)
	}
	return a, pending
}

func readUpgradeState(t *testing.T, a *app.App) map[string]any {
	t.Helper()
	content, err := os.ReadFile(upgradeStatePath(a))
	if err != nil {
		t.Fatalf("读取 upgrade.json 失败: %v", err)
	}
	state := map[string]any{}
	if err := jsonUnmarshal(content, &state); err != nil {
		t.Fatalf("解析 upgrade.json 失败: %v", err)
	}
	return state
}

// TestRecoverUpgradeRollback 验证第二次启动（attempts>=2）自动回滚到 oldJars 备份。
func TestRecoverUpgradeRollback(t *testing.T) {
	a, _ := prepareUpgradeFixture(t, 1, true)
	restarted := 0
	action, err := recoverUpgrade(a, func(time.Duration) { restarted++ }, func() bool { return true }, time.Second)
	if err != nil {
		t.Fatalf("recoverUpgrade: %v", err)
	}
	if action != "rolled-back" {
		t.Fatalf("action = %q, want rolled-back", action)
	}
	content, err := os.ReadFile(a.Executable)
	if err != nil {
		t.Fatal(err)
	}
	if string(content) != "old-binary" {
		t.Errorf("可执行文件未回滚, content=%q", string(content))
	}
	if _, err := os.Stat(upgradePendingPath(a)); !os.IsNotExist(err) {
		t.Error("回滚后应删除 upgrade-pending.json")
	}
	if restarted != 1 {
		t.Errorf("回滚后应触发重启, restarted=%d", restarted)
	}
	state := readUpgradeState(t, a)
	if state["status"] != "rolled-back" {
		t.Errorf("upgrade.json status = %v, want rolled-back", state["status"])
	}
	if state["failedVersion"] != "2.11.13" {
		t.Errorf("upgrade.json failedVersion = %v", state["failedVersion"])
	}
}

// TestRecoverUpgradeRollbackMissingBackup 备份缺失时记录 rollback-failed，不触发重启。
func TestRecoverUpgradeRollbackMissingBackup(t *testing.T) {
	a, _ := prepareUpgradeFixture(t, 1, false)
	restarted := 0
	action, err := recoverUpgrade(a, func(time.Duration) { restarted++ }, func() bool { return true }, time.Second)
	if err == nil {
		t.Fatal("备份缺失应返回错误")
	}
	if action != "rollback-failed" {
		t.Errorf("action = %q", action)
	}
	if restarted != 0 {
		t.Error("回滚失败不应重启")
	}
	state := readUpgradeState(t, a)
	if state["status"] != "rollback-failed" {
		t.Errorf("upgrade.json status = %v", state["status"])
	}
}

// TestConfirmHealthy 验证健康检查通过后写 success 并删除待确认标记。
func TestConfirmHealthy(t *testing.T) {
	a, pending := prepareUpgradeFixture(t, 0, true)
	if !waitUpgradeHealthy(a, pending, func() bool { return true }, 2*time.Second) {
		t.Fatal("健康检查应通过")
	}
	if _, err := os.Stat(upgradePendingPath(a)); !os.IsNotExist(err) {
		t.Error("确认成功后应删除 upgrade-pending.json")
	}
	state := readUpgradeState(t, a)
	if state["status"] != "success" {
		t.Errorf("upgrade.json status = %v, want success", state["status"])
	}
	if state["version"] != "2.11.13" {
		t.Errorf("upgrade.json version = %v", state["version"])
	}
}

// TestRecoverUpgradeFirstBoot 首次启动（attempts=0）只增加计数并等待健康检查，不回滚。
func TestRecoverUpgradeFirstBoot(t *testing.T) {
	a, _ := prepareUpgradeFixture(t, 0, true)
	action, err := recoverUpgrade(a, func(time.Duration) {}, func() bool { return true }, 2*time.Second)
	if err != nil {
		t.Fatalf("recoverUpgrade: %v", err)
	}
	if action != "pending" {
		t.Errorf("action = %q", action)
	}
	// 等待后台健康确认删除 pending。
	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) {
		if _, err := os.Stat(upgradePendingPath(a)); os.IsNotExist(err) {
			return
		}
		time.Sleep(50 * time.Millisecond)
	}
	t.Error("健康确认后应删除 upgrade-pending.json")
}

// TestRecoverUpgradeNoPending 没有待确认升级时返回 none。
func TestRecoverUpgradeNoPending(t *testing.T) {
	a := newTestApp(t)
	action, err := recoverUpgrade(a, func(time.Duration) {}, func() bool { return true }, time.Second)
	if err != nil {
		t.Fatalf("recoverUpgrade: %v", err)
	}
	if action != "none" {
		t.Errorf("action = %q", action)
	}
}
