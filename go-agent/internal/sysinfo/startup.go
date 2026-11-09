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
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
)

// Start 注册启动任务（对应 Java 的 SystemEvent / AgentStartInit）：
//   - 控制台日志自动备份 cron（jpom.project.log.auto-backup-console-cron）
//   - 缓存刷新 cron（system_cache，与 Java 一致每 10 分钟）
//   - 异步刷新 remote_version 缓存
//   - 清理 data/temp 下超过一天的临时目录
//
// 该函数必须在 HTTP 服务启动前调用一次，且只会生效一次。
func Start(a *app.App) {
	manager := NewCronManager(a)
	// 控制台日志自动备份（auto_back_log）属于项目域，由 project-ops 通过
	// RegisterCronTask 注册；系统域只注册缓存刷新任务，避免重复备份。
	if err := manager.Upsert("system_cache", "0 0/10 * * * ?", func() {
		refreshCacheSizes(a)
	}); err != nil {
		a.Log().Warn("register cache refresh task failed", "error", err)
	}
	SetGlobalCron(manager)
	manager.Start()

	go func() {
		// G7：先做升级恢复检查（新版本启动失败自动回滚/健康确认）。
		StartUpgradeRecovery(a)
		refreshCacheSizes(a)
		// AgentStartInit.executeTask：启动加载远程版本信息、清理临时文件、清理脚本执行缓存。
		if _, err := LoadRemoteInfo(a); err != nil {
			a.Log().Warn("refresh remote version failed", "error", err)
		}
		CleanTempFiles(a)
		CleanScriptRunCache(a)
	}()
}

// CleanTempFiles 清理 data/temp 下超过一天的临时目录（保留一天以内）。
func CleanTempFiles(a *app.App) {
	entries, err := os.ReadDir(a.TempRoot())
	if err != nil {
		if !os.IsNotExist(err) {
			a.Log().Warn("read temp directory failed", "error", err)
		}
		return
	}
	now := time.Now()
	for _, entry := range entries {
		dateTime, err := time.ParseInLocation("2006-01-02", entry.Name(), time.Local)
		if err != nil {
			continue
		}
		if int(now.Sub(dateTime).Hours()/24) > 1 {
			target := filepath.Join(a.TempRoot(), entry.Name())
			if err := os.RemoveAll(target); err != nil {
				a.Log().Warn("clear temp files failed, please check the directory", "path", target, "error", err)
			}
		}
	}
}

// CleanScriptRunCache 清理 data/script_run_cache 下超过 1 小时的临时脚本
// （对应 BaseRunScript.clearRunScript）。
func CleanScriptRunCache(a *app.App) {
	entries, err := os.ReadDir(a.ScriptRunCacheDir)
	if err != nil {
		return
	}
	now := time.Now()
	for _, entry := range entries {
		info, err := entry.Info()
		if err != nil {
			continue
		}
		if now.Sub(info.ModTime()) > time.Hour {
			_ = os.RemoveAll(filepath.Join(a.ScriptRunCacheDir, entry.Name()))
		}
	}
}

// RefreshCache 刷新缓存统计（/system/cache 使用；与 Java refreshCache 对应）。
func RefreshCache(a *app.App) { refreshCacheSizes(a) }
