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
	"runtime"
	"strings"
)

// 升级包平台标识（CONTRACT §1/§3）：agent.json.platform 必须精确等于
// "runtime.GOOS/runtime.GOARCH"，容器内可执行文件必须精确命名为
// go-agent/bin/go-agent-<os>-<arch>（windows 额外允许 .exe）。
type targetPlatform struct {
	GOOS   string
	GOARCH string
}

// currentPlatform 返回当前进程的编译平台。
func currentPlatform() targetPlatform {
	return targetPlatform{GOOS: runtime.GOOS, GOARCH: runtime.GOARCH}
}

// String 返回与 agent.json.platform 一致的 "os/arch" 文本。
func (p targetPlatform) String() string { return p.GOOS + "/" + p.GOARCH }

// binaryName 返回发布包内可执行文件名（windows 带 .exe）。
func (p targetPlatform) binaryName() string {
	name := "go-agent-" + p.GOOS + "-" + p.GOARCH
	if p.GOOS == "windows" {
		name += ".exe"
	}
	return name
}

// containerBinaryPath 返回升级容器内必须精确存在的可执行文件路径。
// zip 条目统一使用 "/" 分隔，不依赖运行平台的 filepath.Separator。
func (p targetPlatform) containerBinaryPath() string {
	return "go-agent/bin/" + p.binaryName()
}

// normalizePlatform 归一化平台文本（去空格、统一小写与分隔符），
// 用于 agent.json.platform 与 runtime 平台的大小写无关比较。
func normalizePlatform(value string) string {
	value = strings.TrimSpace(value)
	value = strings.ReplaceAll(value, "\\", "/")
	return strings.ToLower(value)
}

// platformEqual 判断两个平台标识是否一致（大小写归一化）。
func platformEqual(left, right string) bool {
	return normalizePlatform(left) == normalizePlatform(right)
}

// isLegacyLinuxAmd64 表示“平台字段缺失的旧格式升级容器”唯一允许的目标平台。
// CONTRACT §3：容器 platform 为空视为 legacy，仅允许 linux/amd64 节点。
func (p targetPlatform) isLegacyLinuxAmd64() bool {
	return p.GOOS == "linux" && p.GOARCH == "amd64"
}
