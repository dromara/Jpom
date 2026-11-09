/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package proc 提供插件端进程/命令相关的基础能力：
// 命令执行（含 sudo）、按端口/进程标识查进程、结束进程、进程列表、日志 tail。
//
// 命令字符串与 Java 插件端 CommandUtil 保持一致（bash -c / cmd /c），项目启动命令、
// jvm 参数、args 都是用户在服务端配置的原始命令片段。
package proc

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"sort"
	"strings"
)

// shellCommand 返回当前系统的 shell 前缀（等价 Java CommandUtil.COMMAND）。
func shellCommand(command string) (string, []string) {
	if runtime.GOOS == "windows" {
		return "cmd", []string{"/c", command}
	}
	// linux/mac 与 Java 一致使用 /bin/bash -c
	return "/bin/bash", []string{"-c", command}
}

// normalizeCommand 去掉命令中的换行（Java 会把 CRLF/LF 替换成空格）。
func normalizeCommand(command string) string {
	command = strings.ReplaceAll(command, "\r\n", " ")
	command = strings.ReplaceAll(command, "\n", " ")
	return command
}

// ShellQuote 对路径/参数做 shell 安全引用，避免命令注入。
func ShellQuote(value string) string {
	if runtime.GOOS == "windows" {
		return "\"" + strings.ReplaceAll(value, "\"", "\\\"") + "\""
	}
	return "'" + strings.ReplaceAll(value, "'", "'\\''") + "'"
}

// MergeEnv 合并系统环境变量与附加环境变量（附加项覆盖同名项）。
func MergeEnv(env map[string]string) []string {
	base := os.Environ()
	if len(env) == 0 {
		return base
	}
	index := make(map[string]int, len(base))
	for i, item := range base {
		if pos := strings.IndexByte(item, 61); pos >= 0 {
			index[item[:pos]] = i
		}
	}
	for key, value := range env {
		item := key + "=" + value
		if i, ok := index[key]; ok {
			base[i] = item
		} else {
			base = append(base, item)
			index[key] = len(base) - 1
		}
	}
	return base
}

// splitOutput 按行拆分命令输出（等价 Java IoUtil.readLines 后 join）。
func splitOutput(output string) []string {
	output = strings.ReplaceAll(output, "\r\n", "\n")
	if output == "" {
		return nil
	}
	lines := strings.Split(output, "\n")
	// 末尾换行会产生一个空元素，Java readLines 不会返回该行
	if last := len(lines) - 1; last >= 0 && lines[last] == "" {
		lines = lines[:last]
	}
	for i := range lines {
		lines[i] = strings.TrimSuffix(lines[i], "\r")
	}
	return lines
}

// Exec 在 dir 目录下执行命令并返回合并后的输出。
//
// 行为对齐 Java CommandUtil.execSystemCommand：stdout/stderr 合并，按行以换行拼接；
// 命令启动失败时返回 "error"+错误信息（非 0 退出码不算异常，与原实现一致）。
func Exec(dir string, env map[string]string, command string) string {
	_, lines, err := ExecCapture(dir, env, command)
	if err != nil {
		if len(lines) == 0 {
			return "error" + err.Error()
		}
		return strings.Join(lines, "\n") + err.Error()
	}
	return strings.Join(lines, "\n")
}

// ExecCapture 执行命令并返回退出码与逐行输出。
func ExecCapture(dir string, env map[string]string, command string) (int, []string, error) {
	name, args := shellCommand(normalizeCommand(command))
	cmd := exec.Command(name, args...)
	if strings.TrimSpace(dir) != "" {
		cmd.Dir = dir
	}
	cmd.Env = MergeEnv(env)
	var builder strings.Builder
	cmd.Stdout = &builder
	cmd.Stderr = &builder
	err := cmd.Run()
	lines := splitOutput(builder.String())
	if err == nil {
		return 0, lines, nil
	}
	if exitErr, ok := err.(*exec.ExitError); ok {
		return exitErr.ExitCode(), lines, nil
	}
	return -1, lines, err
}

// AsyncExe 异步执行命令（等价 Java CommandUtil.asyncExeLocalCommand）。
//
// hopeUseSudo 与配置文件 jpom.system.command-use-sudo 同时为真时填充 sudo 前缀。
// 子进程 stdout/stderr 丢弃：项目启动命令自身会把输出重定向到项目日志文件。
func AsyncExe(dir string, env map[string]string, command string, hopeUseSudo bool) error {
	command = normalizeCommand(command)
	if hopeUseSudo {
		command = "sudo " + command
	}
	name, args := shellCommand(command)
	cmd := exec.Command(name, args...)
	if strings.TrimSpace(dir) != "" {
		cmd.Dir = dir
	}
	cmd.Env = MergeEnv(env)
	if err := cmd.Start(); err != nil {
		return err
	}
	go func() {
		_ = cmd.Wait()
	}()
	return nil
}

// killCommand 返回结束进程的命令（等价 Java AbstractSystemCommander.buildKill）。
func killCommand(pid int) string {
	if runtime.GOOS == "windows" {
		return fmt.Sprintf("taskkill /F /PID %d", pid)
	}
	return fmt.Sprintf("kill  %d", pid)
}

// forceKillCommand 强制结束进程（unix 使用 kill -9）。
func forceKillCommand(pid int) string {
	if runtime.GOOS == "windows" {
		return fmt.Sprintf("taskkill /F /PID %d", pid)
	}
	return fmt.Sprintf("kill -9 %d", pid)
}

// Kill 结束进程（等价 Java systemCommander.kill）。
func Kill(dir string, pid int) string {
	return Exec(dir, nil, killCommand(pid))
}

// ForceKill 强制结束进程（等价 Java 的 kill -9 逻辑）。
func ForceKill(dir string, pid int) string {
	return Exec(dir, nil, forceKillCommand(pid))
}

// EmptyLogFile 清空日志文件内容（等价 Java SystemCommander.emptyLogFile）。
func EmptyLogFile(path string) string {
	if runtime.GOOS == "windows" {
		return Exec("", nil, "echo  \"\" > "+ShellQuote(path))
	}
	return Exec("", nil, "cp /dev/null "+ShellQuote(path))
}

// SystemFastDel 快速删除文件或目录（等价 Java CommandUtil.systemFastDel）。
// 返回值 true 表示文件仍然存在（删除失败）。
func SystemFastDel(path string) bool {
	if runtime.GOOS == "windows" {
		Exec("", nil, "rd /s/q "+ShellQuote(path))
	} else {
		Exec("", nil, "rm -rf "+ShellQuote(path))
	}
	_ = os.RemoveAll(path)
	_, err := os.Lstat(path)
	return err == nil
}

// JavaCommand 解析 java/javaw 可执行命令。
//
// 解析顺序与 Java 插件端运行环境兼容并更健壮：
//  1. PATH 中找到 java/javaw 时返回字面量 "java"/"javaw"（与 Java 生成的命令一致）；
//  2. JAVA_HOME/bin/java（javaw）；
//  3. 常见 JDK 安装路径（/usr/java/*、/usr/lib/jvm/*、/opt/java/*、/opt/jdk* 等）。
//
// 都找不到时返回明确错误，避免 nohup 报 "failed to run command 'java'"。
func JavaCommand(windows bool) (string, error) {
	names := []string{"java"}
	if windows {
		names = []string{"javaw", "java"}
	}
	for _, name := range names {
		if _, err := exec.LookPath(name); err == nil {
			// 保持与 Java 端一致的命令字面量
			return name, nil
		}
	}
	exeSuffix := ""
	if windows {
		exeSuffix = ".exe"
	}
	if home := strings.TrimSpace(os.Getenv("JAVA_HOME")); home != "" {
		for _, name := range names {
			candidate := filepath.Join(home, "bin", name+exeSuffix)
			if isExecutableFile(candidate) {
				return candidate, nil
			}
		}
	}
	var patterns []string
	if windows {
		patterns = []string{
			"C:/Program Files/Java/*/bin/javaw.exe",
			"C:/Program Files/Java/*/bin/java.exe",
			"C:/Program Files (x86)/Java/*/bin/javaw.exe",
		}
	} else {
		for _, name := range names {
			patterns = append(patterns,
				"/usr/java/*/bin/"+name,
				"/usr/lib/jvm/*/bin/"+name,
				"/usr/local/java/*/bin/"+name,
				"/opt/java/*/bin/"+name,
				"/opt/jdk*/bin/"+name,
				"/opt/jdk/*/bin/"+name,
			)
		}
	}
	for _, pattern := range patterns {
		matches, err := filepath.Glob(pattern)
		if err != nil || len(matches) == 0 {
			continue
		}
		sort.Strings(matches)
		for _, candidate := range matches {
			if isExecutableFile(candidate) {
				return candidate, nil
			}
		}
	}
	return "", fmt.Errorf("java executable not found, please configure JAVA_HOME or PATH")
}

// isExecutableFile 判断是否为可执行文件。
func isExecutableFile(path string) bool {
	stat, err := os.Stat(path)
	if err != nil || stat.IsDir() {
		return false
	}
	if runtime.GOOS == "windows" {
		return true
	}
	return stat.Mode()&0o111 != 0
}

// ExecutablePath 返回当前进程可执行文件路径。
func ExecutablePath() string {
	exe, err := os.Executable()
	if err != nil {
		return ""
	}
	return exe
}
