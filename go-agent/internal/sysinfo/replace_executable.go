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
	"strings"

	"jpom.io/go-agent/internal/app"
)

// cleanupReplacedExecutables 清理 Windows 升级时留下的 <exe>.old 文件
// （旧进程退出/新进程启动后不再被占用，可直接删除）。
func cleanupReplacedExecutables(a *app.App) {
	entries, err := os.ReadDir(a.RunDir)
	if err != nil {
		return
	}
	executableName := strings.ToLower(filepath.Base(a.Executable))
	for _, entry := range entries {
		name := strings.ToLower(entry.Name())
		if entry.IsDir() || !strings.HasSuffix(name, ".old") {
			continue
		}
		if !strings.HasPrefix(name, executableName+".") {
			continue
		}
		path := filepath.Join(a.RunDir, entry.Name())
		if err := os.Remove(path); err != nil {
			a.Log().Debug("cleanup replaced executable failed", "path", path, "error", err)
		}
	}
}
