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

import "strings"

// serviceManagerProcessNames 是 Windows 下以服务方式托管插件端的常见父进程名。
// CONTRACT §4：父进程为服务管理器时只 os.Exit(0)，交给服务管理器按配置重启，
// 避免“起新进程 + 服务管理器再拉起”造成双实例抢端口。
var serviceManagerProcessNames = map[string]struct{}{
	"services": {},
	"nssm":     {},
	"svchost":  {},
}

// isServiceManagerProcess 判断父进程名是否为服务管理器/托管工具（大小写无关，
// 允许带 .exe 后缀）。放在跨平台文件中便于在 Linux 上做静态/单元测试。
func isServiceManagerProcess(name string) bool {
	name = strings.ToLower(strings.TrimSpace(name))
	name = strings.TrimSuffix(name, ".exe")
	_, ok := serviceManagerProcessNames[name]
	return ok
}
