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

package sysinfo

import "os"

// replaceExecutable 在 Windows 上用 staged 覆盖 target。
//
// Windows 不允许覆盖/删除正在运行的可执行文件（sharing violation），但允许改名：
// 先把当前文件改名为 <exe>.old，再把新文件放回原名；<exe>.old 由下次启动清理
// （cleanupReplacedExecutables）。失败时回滚旧文件，保证当前版本仍可执行。
func replaceExecutable(staged, target string) error {
	previous := target + ".old"
	_ = os.Remove(previous)
	if err := os.Rename(target, previous); err != nil {
		return err
	}
	if err := os.Rename(staged, target); err != nil {
		// 回滚：把旧文件放回原名，避免节点不可用。
		_ = os.Rename(previous, target)
		return err
	}
	// 旧文件仍被当前进程映射时删除会失败，留给下次启动清理。
	_ = os.Remove(previous)
	return nil
}
