//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

//go:build windows

package proc

import (
	"fmt"
	"os/exec"
)

// SetProcessGroup 在 Windows 上没有 POSIX 进程组语义：结束进程树走
// taskkill /PID <pid> /T，因此这里不做任何处理（保留函数签名供构建执行器统一调用）。
func SetProcessGroup(cmd *exec.Cmd) {}

// TerminateProcessGroup 结束进程树（taskkill /T 先发关闭请求，不带 /F 保持可优雅退出）。
func TerminateProcessGroup(pid int) error {
	if pid <= 0 {
		return nil
	}
	return exec.Command("taskkill", "/PID", fmt.Sprintf("%d", pid), "/T").Run()
}

// ProcessGroupAlive 在 Windows 上没有可靠的进程组探测，恒为 true（taskkill 幂等）。
func ProcessGroupAlive(pid int) bool { return pid > 0 }

// KillProcessGroup 强制结束进程树（taskkill /PID x /T /F）。
func KillProcessGroup(pid int) error {
	if pid <= 0 {
		return nil
	}
	return exec.Command("taskkill", "/PID", fmt.Sprintf("%d", pid), "/T", "/F").Run()
}
