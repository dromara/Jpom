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

import "testing"

// TestIsServiceManagerProcess 覆盖 Windows 服务管理器父进程名判断（CONTRACT §4）。
func TestIsServiceManagerProcess(t *testing.T) {
	managed := []string{"services.exe", "Services.EXE", "nssm.exe", "NSSM", "svchost.exe", " svchost "}
	for _, name := range managed {
		if !isServiceManagerProcess(name) {
			t.Errorf("%q 应识别为服务管理器", name)
		}
	}
	unmanaged := []string{"explorer.exe", "cmd.exe", "powershell.exe", "", "service", "nssm2.exe"}
	for _, name := range unmanaged {
		if isServiceManagerProcess(name) {
			t.Errorf("%q 不应识别为服务管理器", name)
		}
	}
}
