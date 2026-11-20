//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

//go:build !windows

package proc

import (
	"os/exec"
	"syscall"
)

// SetProcessGroup 让 cmd 成为新进程组的组长（Unix: setpgid(0, 0)）。
//
// 构建执行必须能一次杀掉「sh + mvn + 子进程」整棵树，而现有 Kill/ForceKill 只按
// 单个 pid 结束进程（项目生命周期的语义），因此这里新增进程组实现，不改动既有函数。
func SetProcessGroup(cmd *exec.Cmd) {
	if cmd == nil {
		return
	}
	if cmd.SysProcAttr == nil {
		cmd.SysProcAttr = &syscall.SysProcAttr{}
	}
	cmd.SysProcAttr.Setpgid = true
}

// TerminateProcessGroup 向进程组发送 SIGTERM（pid 为组长 pid，即 shell 进程 pid）。
func TerminateProcessGroup(pid int) error {
	if pid <= 0 {
		return nil
	}
	if err := syscall.Kill(-pid, syscall.SIGTERM); err != nil {
		if err == syscall.ESRCH {
			return nil
		}
		return err
	}
	return nil
}

// ProcessGroupAlive 报告进程组是否仍然存在（SIGTERM 后确认再补 SIGKILL，避免误杀复用 pgid）。
func ProcessGroupAlive(pid int) bool {
	if pid <= 0 {
		return false
	}
	return syscall.Kill(-pid, 0) == nil
}

// KillProcessGroup 向进程组发送 SIGKILL（TerminateProcessGroup 的兜底）。
func KillProcessGroup(pid int) error {
	if pid <= 0 {
		return nil
	}
	if err := syscall.Kill(-pid, syscall.SIGKILL); err != nil {
		if err == syscall.ESRCH {
			return nil
		}
		return err
	}
	return nil
}
