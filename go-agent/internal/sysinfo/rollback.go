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
	"fmt"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"time"

	"jpom.io/go-agent/internal/app"
)

// G7：升级失败自动回滚。
//
// 流程：
//  1. 安装新二进制后写 upgrade-pending.json（beforeJar=oldJars 中备份文件名、attempts=0）；
//  2. 进程重启后 StartUpgradeRecovery 读取 pending：
//     - attempts+1 后仍 <2：开始健康检查（自请求 GET /），成功则写 upgrade.json
//     status=success 并删除 pending；
//     - attempts>=2（说明上一次新版本启动后未通过健康检查就退出/崩溃）：自动把
//     oldJars 中的备份二进制恢复为当前可执行文件并重启，upgrade.json 记录 rolled-back。
//  3. 回滚失败的场景记录 status=rollback-failed 并保留日志，避免无限重启。
const (
	upgradePendingFile = "upgrade-pending.json"
	upgradeStateFile   = "upgrade.json"
	// 新版本启动后健康检查的等待时间；超时不算失败（进程仍在跑），
	// 只有进程真的退出后下次启动才会触发回滚。
	upgradeHealthyTimeout = 60 * time.Second
)

type upgradePending struct {
	BeforeJar     string `json:"beforeJar"`
	BeforeVersion string `json:"beforeVersion"`
	NewVersion    string `json:"newVersion"`
	UpdateTime    string `json:"updateTime"`
	Attempts      int    `json:"attempts"`
}

func upgradePendingPath(a *app.App) string { return filepath.Join(a.RunDir, upgradePendingFile) }

func upgradeStatePath(a *app.App) string { return filepath.Join(a.RunDir, upgradeStateFile) }

func readUpgradePending(a *app.App) (*upgradePending, error) {
	content, err := os.ReadFile(upgradePendingPath(a))
	if err != nil {
		if os.IsNotExist(err) {
			return nil, nil
		}
		return nil, err
	}
	if len(content) == 0 {
		return nil, nil
	}
	pending := &upgradePending{}
	if err := jsonUnmarshal(content, pending); err != nil {
		return nil, err
	}
	if pending.BeforeJar == "" {
		return nil, nil
	}
	return pending, nil
}

func writeUpgradePending(a *app.App, pending *upgradePending) error {
	return writeJSONAtomic(upgradePendingPath(a), pending)
}

// updateUpgradeStatus 更新 upgrade.json 的状态字段（保留已有的版本/计数信息）。
func updateUpgradeStatus(a *app.App, status string, extra map[string]any) {
	state := map[string]any{}
	if content, err := os.ReadFile(upgradeStatePath(a)); err == nil && len(content) > 0 {
		_ = jsonUnmarshal(content, &state)
	}
	state["status"] = status
	state["updateTime"] = time.Now().Format("2006-01-02 15:04:05")
	for key, value := range extra {
		state[key] = value
	}
	if err := writeJSONAtomic(upgradeStatePath(a), state); err != nil {
		a.Log().Warn("write upgrade state failed", "error", err)
	}
}

// StartUpgradeRecovery 在启动阶段检查是否存在待确认的升级（G7）。
// 失败回滚会延迟重启；健康检查在后台执行。
func StartUpgradeRecovery(a *app.App) {
	cleanupReplacedExecutables(a)
	action, err := recoverUpgrade(a, a.RestartAfter, defaultHealthyChecker(a), upgradeHealthyTimeout)
	if err != nil {
		a.Log().Warn("upgrade recovery check failed", "error", err)
		return
	}
	if action != "" && action != "none" {
		a.Log().Info("upgrade recovery check", "action", action)
	}
}

// recoverUpgrade 执行恢复决策（可注入 restart/healthy 便于单测）。
func recoverUpgrade(a *app.App, restart func(time.Duration), healthy func() bool, timeout time.Duration) (string, error) {
	pending, err := readUpgradePending(a)
	if err != nil {
		return "", fmt.Errorf("read upgrade pending marker failed: %w", err)
	}
	if pending == nil {
		return "none", nil
	}
	pending.Attempts++
	if pending.Attempts >= 2 {
		// 上一次启动后未通过健康确认就再次启动，判定新版本启动失败 -> 回滚。
		return rollbackUpgrade(a, restart, pending)
	}
	pending.UpdateTime = time.Now().Format("2006-01-02 15:04:05")
	if err := writeUpgradePending(a, pending); err != nil {
		return "", err
	}
	go func() {
		if waitUpgradeHealthy(a, pending, healthy, timeout) {
			a.Log().Info("new version health check passed", "version", pending.NewVersion)
		} else {
			a.Log().Warn("new version health check timed out; if the process exits unexpectedly the next start will roll back", "version", pending.NewVersion)
		}
	}()
	return "pending", nil
}

// waitUpgradeHealthy 轮询本地 HTTP 服务直到可访问；成功则确认升级。
func waitUpgradeHealthy(a *app.App, pending *upgradePending, healthy func() bool, timeout time.Duration) bool {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if healthy() {
			updateUpgradeStatus(a, "success", map[string]any{
				"version":       pending.NewVersion,
				"beforeVersion": pending.BeforeVersion,
				"confirmedTime": time.Now().Format("2006-01-02 15:04:05"),
			})
			_ = os.Remove(upgradePendingPath(a))
			return true
		}
		time.Sleep(time.Second)
	}
	return false
}

// rollbackUpgrade 用 oldJars 中的备份恢复上一个可用版本并重启。
func rollbackUpgrade(a *app.App, restart func(time.Duration), pending *upgradePending) (string, error) {
	backupPath := filepath.Join(a.OldJarsPath(), pending.BeforeJar)
	if info, err := os.Stat(backupPath); err != nil || info.IsDir() {
		updateUpgradeStatus(a, "rollback-failed", map[string]any{
			"beforeJar":  pending.BeforeJar,
			"newVersion": pending.NewVersion,
			"error":      fmt.Sprintf("backup does not exist: %s", backupPath),
		})
		_ = os.Remove(upgradePendingPath(a))
		return "rollback-failed", fmt.Errorf("rollback failed, backup file does not exist: %s", backupPath)
	}
	staged := a.Executable + ".rollback"
	if err := copyFile(backupPath, staged); err != nil {
		updateUpgradeStatus(a, "rollback-failed", map[string]any{"error": err.Error()})
		return "rollback-failed", fmt.Errorf("write rollback file failed: %w", err)
	}
	if err := os.Chmod(staged, 0o755); err != nil {
		_ = os.Remove(staged)
		updateUpgradeStatus(a, "rollback-failed", map[string]any{"error": err.Error()})
		return "rollback-failed", err
	}
	if err := os.Rename(staged, a.Executable); err != nil {
		_ = os.Remove(staged)
		updateUpgradeStatus(a, "rollback-failed", map[string]any{"error": err.Error()})
		return "rollback-failed", fmt.Errorf("replace rollback file failed: %w", err)
	}
	updateUpgradeStatus(a, "rolled-back", map[string]any{
		"beforeJar":     pending.BeforeJar,
		"beforeVersion": pending.BeforeVersion,
		"rollbackTime":  time.Now().Format("2006-01-02 15:04:05"),
		"failedVersion": pending.NewVersion,
	})
	_ = os.WriteFile(filepath.Join(a.RunDir, "run.bin"), []byte(filepath.Base(a.Executable)), 0o644)
	_ = os.Remove(upgradePendingPath(a))
	a.Log().Warn("new version failed to start, rolled back to the previous available version", "version", pending.BeforeVersion, "failed", pending.NewVersion)
	restart(2 * time.Second)
	return "rolled-back", nil
}

// defaultHealthyChecker 自请求公开的 index 接口判断 HTTP 服务是否可用。
func defaultHealthyChecker(a *app.App) func() bool {
	return func() bool {
		address := a.Config.Server.Address
		if address == "" || address == "0.0.0.0" || address == "::" {
			address = "127.0.0.1"
		}
		port := a.Config.Server.Port
		if port <= 0 {
			return false
		}
		host := net.JoinHostPort(address, strconv.Itoa(port))
		client := &http.Client{Timeout: 2 * time.Second}
		response, err := client.Get("http://" + host + "/")
		if err != nil {
			return false
		}
		defer func() { _ = response.Body.Close() }()
		return response.StatusCode == http.StatusOK
	}
}
