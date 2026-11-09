//go:build windows

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
	"os/exec"

	"github.com/shirou/gopsutil/v4/process"
)

// replaceProcess 在 Windows 上重启插件端。
//
// CONTRACT §4：
//   - 父进程是服务管理器（services.exe/nssm.exe/svchost.exe）时只退出进程，
//     由服务管理器按 Restart 配置重新拉起，避免双实例抢端口；
//   - 否则保持“起新进程后退出当前进程”的行为。
func replaceProcess(executable string) error {
	if managedByServiceManager() {
		os.Exit(0)
		return nil
	}
	cmd := exec.Command(executable, os.Args[1:]...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin
	if err := cmd.Start(); err != nil {
		return err
	}
	os.Exit(0)
	return nil
}

// managedByServiceManager 通过 gopsutil 读取父进程名判断是否由服务管理器托管。
func managedByServiceManager() bool {
	parentPID := os.Getppid()
	if parentPID <= 0 {
		return false
	}
	parent, err := process.NewProcess(int32(parentPID))
	if err != nil {
		return false
	}
	name, err := parent.Name()
	if err != nil {
		return false
	}
	return isServiceManagerProcess(name)
}
