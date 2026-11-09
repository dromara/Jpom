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
	"strconv"
)

// pidFileName 是 Go 插件端 PID 文件名（CONTRACT §4）：所有平台统一写到
// <base>/bin/agent.pid，由 AgentGo.sh / AgentGo.bat 的 stop/status/restart 读取。
const pidFileName = "agent.pid"

// PidFilePath 返回 PID 文件路径。
func (a *App) PidFilePath() string {
	return filepath.Join(a.BaseDir, "bin", pidFileName)
}

// WritePidFile 原子写入当前进程 PID（临时文件 + rename），供启停脚本使用。
func (a *App) WritePidFile() error {
	path := a.PidFilePath()
	dir := filepath.Dir(path)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return err
	}
	pid := a.Pid
	if pid <= 0 {
		pid = os.Getpid()
	}
	tmp, err := os.CreateTemp(dir, ".agent-pid-*")
	if err != nil {
		return err
	}
	tmpName := tmp.Name()
	defer func() { _ = os.Remove(tmpName) }()
	if _, err := tmp.WriteString(strconv.Itoa(pid) + "\n"); err != nil {
		_ = tmp.Close()
		return err
	}
	if err := tmp.Sync(); err != nil {
		_ = tmp.Close()
		return err
	}
	if err := tmp.Close(); err != nil {
		return err
	}
	if err := os.Chmod(tmpName, 0o644); err != nil {
		return err
	}
	return os.Rename(tmpName, path)
}

// RemovePidFile 正常退出时删除 PID 文件；自升级重启（exec / 子进程）由新进程覆盖，
// 因此重启路径不会走到这里（os.Exit/syscall.Exec 不执行 defer）。
func (a *App) RemovePidFile() {
	_ = os.Remove(a.PidFilePath())
}
