/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package app

import (
	"os"
	"path/filepath"
	"time"
)

// TempRoot is <data>/temp, the equivalent of JpomApplication#getTempPath().
func (a *App) TempRoot() string { return filepath.Join(a.DataDir, "temp") }

// TempPath is the day scoped temporary directory used by the Java agent
// (AgentConfig#getTempPath / getFixedTempPathName): <data>/temp/yyyy-MM-dd.
func (a *App) TempPath() string {
	path := filepath.Join(a.TempRoot(), time.Now().Format("2006-01-02"))
	_ = os.MkdirAll(path, 0o755)
	return path
}

// OldJarsPath is where superseded agent packages/binaries are kept.
// The Java agent uses <lib>/oldJars; the Go agent keeps the same layout next to
// the executable (normally <base>/bin/oldJars).
func (a *App) OldJarsPath() string { return filepath.Join(a.RunDir, "oldJars") }

// DataSize returns the total size in bytes of the data directory.
func (a *App) DataSize() int64 {
	var total int64
	_ = filepath.Walk(a.DataDir, func(_ string, info os.FileInfo, err error) error {
		if err != nil || info == nil || info.IsDir() {
			return nil
		}
		total += info.Size()
		return nil
	})
	return total
}

// DirSize returns the total size in bytes of path (0 when it does not exist).
func DirSize(path string) int64 {
	var total int64
	_ = filepath.Walk(path, func(_ string, info os.FileInfo, err error) error {
		if err != nil || info == nil || info.IsDir() {
			return nil
		}
		total += info.Size()
		return nil
	})
	return total
}

// RestartAfter restarts the agent after delay, mirroring the Java agent which
// schedules a script based restart ~2 seconds after the response has been
// written. The delay gives the HTTP client time to read the response first.
func (a *App) RestartAfter(delay time.Duration) {
	go func() {
		if delay > 0 {
			time.Sleep(delay)
		}
		a.Log().Info("agent restarting", "executable", a.Executable)
		if err := replaceProcess(a.Executable); err != nil {
			a.Log().Error("agent restart failed", "error", err)
		}
	}()
}
