//go:build !windows

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
	"syscall"
)

// replaceProcess re-executes the agent binary in place (the listener socket is
// created with SO_REUSEADDR by the Go runtime, so a short overlap is fine).
func replaceProcess(executable string) error {
	args := append([]string{executable}, os.Args[1:]...)
	return syscall.Exec(executable, args, os.Environ())
}
