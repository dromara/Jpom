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
	"path/filepath"
	"strings"
)

// MigrateLegacyProjectLogs 迁移旧版布局的项目控制台日志（等价 AgentStartInit.autoStartProject 的迁移段）。
//
// 旧版（2.x 早期）把项目控制台日志直接放在白名单目录下：<parent>/<id>.log 与 <parent>/<id>.log_back。
// 新版按项目分目录存放：<logPath 或 base>/<id>/<id>.log，备份在 <logPath 或 base>/<id>/back/。
// 升级或从旧插件端切过来时把旧文件搬进新位置，避免界面上"日志凭空消失"。
//
// 跳过条件与 Java 一致：软链项目（runMode=Link）、自定义了 logPath 的项目；项目运行中时
// 单个 .log 只告警不迁移。迁移是 move 语义，不删除日志内容，可逆。
func (r *Runtime) MigrateLegacyProjectLogs() {
	list, err := r.Manager.List()
	if err != nil {
		if r.Log != nil {
			r.Log.Warn("skip legacy project log migration: list project failed", "error", err)
		}
		return
	}
	lang := r.Lang()
	for _, info := range list {
		func() {
			defer func() {
				if recovered := recover(); recovered != nil && r.Log != nil {
					r.Log.Warn("legacy project log migration panic", "id", GetString(info, "id"), "panic", recovered)
				}
			}()
			r.migrateLegacyProjectLog(info, lang)
		}()
	}
}

// migrateLegacyProjectLog 迁移单个项目的旧版日志文件。
func (r *Runtime) migrateLegacyProjectLog(info map[string]any, lang string) {
	if GetString(info, "runMode") == RunModeLink {
		return
	}
	// 自定义 logPath 的项目日志一直按项目分目录存放，没有旧版布局
	if strings.TrimSpace(GetString(info, "logPath")) != "" {
		return
	}
	id := GetString(info, "id")
	if id == "" {
		return
	}
	lib, err := r.Manager.ResolveLibFile(info)
	if err != nil {
		return
	}
	// 旧版日志放在项目目录的父级（白名单目录）下
	parent := filepath.Dir(lib)
	if parent == "" || parent == "." {
		return
	}
	legacyLog := filepath.Join(parent, id+".log")
	legacyBack := legacyLog + "_back"
	backDir, err := r.Manager.ResolveLogBack(info)
	if err != nil {
		return
	}
	migrateFailed := func(action, path string, err error) {
		if r.Log != nil {
			r.Log.Error("legacy project log migration failed", "id", id, "action", action, "path", path, "error", err)
		}
	}
	if stat, err := os.Stat(legacyBack); err == nil && stat.IsDir() {
		if err := os.MkdirAll(backDir, 0o755); err != nil {
			migrateFailed("mkdir", backDir, err)
			return
		}
		if r.Log != nil {
			r.Log.Info(tr(lang, keyAutoMigrateExistBackupLogs, legacyBack, backDir))
		}
		if err := moveContentTo(legacyBack, backDir); err != nil {
			migrateFailed("move-back-dir", legacyBack, err)
			return
		}
		_ = os.RemoveAll(legacyBack)
	}
	stat, err := os.Stat(legacyLog)
	if err != nil || stat.IsDir() {
		return
	}
	// 与 Java 一致：运行中的项目不动旧日志，只提示手工迁移
	if r.IsRun(info) {
		if r.Log != nil {
			r.Log.Warn(tr(lang, keyOldVersionProjectLogsRunning, GetString(info, "name"), legacyLog))
		}
		return
	}
	if err := os.MkdirAll(backDir, 0o755); err != nil {
		migrateFailed("mkdir", backDir, err)
		return
	}
	if r.Log != nil {
		r.Log.Info(tr(lang, keyAutoMigrateExistLogs, legacyLog, backDir))
	}
	target := filepath.Join(backDir, id+".log")
	if err := os.Rename(legacyLog, target); err != nil {
		// 跨设备等场景退化为复制 + 删除
		if copyErr := copyFileContent(legacyLog, target); copyErr != nil {
			migrateFailed("move-log", legacyLog, copyErr)
			return
		}
		_ = os.Remove(legacyLog)
	}
}
