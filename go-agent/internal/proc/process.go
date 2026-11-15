/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package proc

import (
	"bytes"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"sync"
)

// RunningTag / StoppedTag 状态前缀，与 Java AbstractProjectCommander 一致。
const (
	RunningTag = "running"
	StoppedTag = "stopped"
)

// pidTagPrefixes 与 Java JvmUtil.JPOM_PID_TAG 一致（-DJpom.application / -Jpom.application）。
var pidTagPrefixes = []string{"-DJpom.application=", "-Jpom.application="}

// MatchPidTag 判断命令行参数中是否包含项目标识（等价 JvmUtil.checkCommandLineIsJpom）。
func MatchPidTag(args []string, tag string) bool {
	if tag == "" {
		return false
	}
	for _, arg := range args {
		for _, prefix := range pidTagPrefixes {
			if strings.EqualFold(arg, prefix+tag) {
				return true
			}
		}
	}
	return false
}

// matchCommandLine 按空格拆分命令行后匹配（对应 Java 对 ps 输出的处理）。
func matchCommandLine(line, tag string) bool {
	if strings.TrimSpace(line) == "" {
		return false
	}
	return MatchPidTag(strings.Fields(line), tag)
}

// listProcessPids 列出 /proc 下的进程 ID（Linux）。
func listProcessPids() []int {
	entries, err := os.ReadDir("/proc")
	if err != nil {
		return nil
	}
	pids := make([]int, 0, len(entries))
	for _, entry := range entries {
		if !entry.IsDir() {
			continue
		}
		pid, err := strconv.Atoi(entry.Name())
		if err != nil {
			continue
		}
		pids = append(pids, pid)
	}
	sort.Ints(pids)
	return pids
}

// readCmdline 读取 /proc/<pid>/cmdline 并拆分为参数列表。
func readCmdline(pid int) []string {
	data, err := os.ReadFile(filepath.Join("/proc", strconv.Itoa(pid), "cmdline"))
	if err != nil || len(data) == 0 {
		return nil
	}
	parts := bytes.Split(data, []byte{0})
	args := make([]string, 0, len(parts))
	for _, part := range parts {
		if len(part) == 0 {
			continue
		}
		args = append(args, string(part))
	}
	return args
}

// PidsByTag 查询包含项目标识的进程 ID 列表。
//
// Linux 直接读取 /proc/<pid>/cmdline（等价 Java 的 jps -mv 与 ps -ef 两路探测的并集，
// 且不受 ps 输出截断影响）；其他系统回退到 jps -mv / ps 命令。
func PidsByTag(tag string) []int {
	if tag == "" {
		return nil
	}
	if runtime.GOOS != "windows" {
		if pids := pidsByProc(tag); pids != nil {
			return pids
		}
	}
	if pids := pidsByJps(tag); len(pids) > 0 {
		return pids
	}
	if runtime.GOOS != "windows" {
		if pids := pidsByPs(tag); len(pids) > 0 {
			return pids
		}
	}
	return nil
}

func pidsByProc(tag string) []int {
	pids := listProcessPids()
	if len(pids) == 0 {
		return nil
	}
	matched := make([]int, 0, 2)
	for _, pid := range pids {
		if MatchPidTag(readCmdline(pid), tag) {
			matched = append(matched, pid)
		}
	}
	return matched
}

// pidsByJps 解析 jps -mv 输出（等价 Java JvmUtil.getPidByTag）。
func pidsByJps(tag string) []int {
	output := Exec("", nil, "jps -mv")
	if strings.HasPrefix(output, "error") {
		return nil
	}
	pids := make([]int, 0, 2)
	for _, line := range strings.Split(output, "\n") {
		if !matchCommandLine(line, tag) {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) == 0 {
			continue
		}
		if pid, err := strconv.Atoi(fields[0]); err == nil && pid > 0 {
			pids = append(pids, pid)
		}
	}
	sort.Ints(pids)
	return pids
}

// pidsByPs 解析 ps 输出（等价 Java BaseUnixProjectCommander.bySystemPs）。
func pidsByPs(tag string) []int {
	var output string
	if runtime.GOOS == "darwin" {
		output = Exec("", nil, "ps aux")
	} else {
		output = Exec("", nil, "ps -ef")
	}
	if strings.HasPrefix(output, "error") {
		return nil
	}
	pids := make([]int, 0, 2)
	for _, line := range strings.Split(output, "\n") {
		if !strings.Contains(line, tag) || !matchCommandLine(line, tag) {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}
		if pid, err := strconv.Atoi(fields[1]); err == nil && pid > 0 {
			pids = append(pids, pid)
		}
	}
	sort.Ints(pids)
	return pids
}

// Status 返回项目进程状态："running:<pid>" 或 "stopped"（等价 ProjectCommander.status(tag)）。
func Status(tag string) string {
	pids := PidsByTag(tag)
	if len(pids) == 0 {
		return StoppedTag
	}
	return RunningTag + ":" + strconv.Itoa(pids[0])
}

// portCache 缓存 pid 对应端口（等价 Java AbstractProjectCommander.PID_PORT）。
var portCache = struct {
	sync.Mutex
	values map[int]string
}{values: make(map[int]string)}

// ClearPortCache 清理指定进程的端口缓存（停止项目时调用）。
func ClearPortCache(pid int) {
	portCache.Lock()
	defer portCache.Unlock()
	delete(portCache.values, pid)
}

// MainPort 获取进程占用的端口（多个端口以英文逗号拼接，无端口返回 "-"）。
// 等价 Java ProjectCommander.getMainPort。
func MainPort(pid int) string {
	if pid <= 0 {
		return "-"
	}
	portCache.Lock()
	cached, ok := portCache.values[pid]
	portCache.Unlock()
	if ok {
		return cached
	}
	ports := PortsByPid(pid)
	if len(ports) == 0 {
		return "-"
	}
	parts := make([]string, 0, len(ports))
	for _, port := range ports {
		parts = append(parts, strconv.Itoa(port))
	}
	value := strings.Join(parts, ",")
	portCache.Lock()
	portCache.values[pid] = value
	portCache.Unlock()
	return value
}

// PortsByPid 查询进程监听的端口。
func PortsByPid(pid int) []int {
	if pid <= 0 {
		return nil
	}
	if runtime.GOOS == "linux" {
		if ports, ok := portsFromProc(pid); ok {
			return ports
		}
	}
	switch runtime.GOOS {
	case "darwin":
		return portsByLsof(pid)
	case "windows":
		return portsByNetstatWindows(pid)
	default:
		return portsByNetstat(pid)
	}
}

// portsFromProc 通过 /proc/<pid>/fd + /proc/net/tcp{,6} 解析 LISTEN 端口。
// 第二个返回值表示 /proc 是否可读（不可读时调用方走 netstat 回退）。
func portsFromProc(pid int) ([]int, bool) {
	fdDir := filepath.Join("/proc", strconv.Itoa(pid), "fd")
	entries, err := os.ReadDir(fdDir)
	if err != nil {
		return nil, false
	}
	inodes := make(map[string]struct{}, len(entries))
	for _, entry := range entries {
		link, err := os.Readlink(filepath.Join(fdDir, entry.Name()))
		if err != nil {
			continue
		}
		if !strings.HasPrefix(link, "socket:[") {
			continue
		}
		inode := strings.TrimSuffix(strings.TrimPrefix(link, "socket:["), "]")
		inodes[inode] = struct{}{}
	}
	if len(inodes) == 0 {
		return nil, true
	}
	ports := portsFromNetTable(inodes)
	return ports, true
}

// portsFromNetTable 从 /proc/net/tcp、/proc/net/tcp6 中匹配 socket inode 的 LISTEN 端口。
func portsFromNetTable(inodes map[string]struct{}) []int {
	seen := make(map[int]struct{}, 4)
	ports := make([]int, 0, 4)
	for _, file := range []string{"/proc/net/tcp", "/proc/net/tcp6"} {
		data, err := os.ReadFile(file)
		if err != nil {
			continue
		}
		lines := strings.Split(string(data), "\n")
		for _, line := range lines[1:] {
			fields := strings.Fields(line)
			if len(fields) < 10 {
				continue
			}
			// st==0A 表示 LISTEN
			if !strings.EqualFold(fields[3], "0A") {
				continue
			}
			if _, ok := inodes[fields[9]]; !ok {
				continue
			}
			local := fields[1]
			pos := strings.LastIndex(local, ":")
			if pos < 0 {
				continue
			}
			port, err := strconv.ParseInt(local[pos+1:], 16, 32)
			if err != nil || port <= 0 {
				continue
			}
			if _, ok := seen[int(port)]; ok {
				continue
			}
			seen[int(port)] = struct{}{}
			ports = append(ports, int(port))
		}
	}
	sort.Ints(ports)
	return ports
}

// portsByNetstat 通过 netstat 解析（Linux 回退，等价 Java LinuxProjectCommander.listNetstat）。
func portsByNetstat(pid int) []int {
	output := Exec("", nil, "netstat -antup 2>/dev/null | grep "+strconv.Itoa(pid)+" | grep \"LISTEN\" | head -20")
	if output == "" {
		output = Exec("", nil, "ss -tlnp 2>/dev/null | grep "+strconv.Itoa(pid)+" | head -20")
	}
	if output == "" || strings.HasPrefix(output, "error") {
		return nil
	}
	ports := make([]int, 0, 4)
	seen := make(map[int]struct{}, 4)
	for _, line := range strings.Split(output, "\n") {
		fields := strings.Fields(line)
		if len(fields) < 4 {
			continue
		}
		local := fields[3]
		// ss 输出形如 LISTEN 0 128 *:8080 ...
		if strings.Contains(local, ":") && !strings.Contains(local, "/") {
			port := parseLocalPort(local)
			if port > 0 {
				if _, ok := seen[port]; !ok {
					seen[port] = struct{}{}
					ports = append(ports, port)
				}
			}
			continue
		}
		if len(fields) >= 5 && strings.Contains(fields[4], ":") {
			port := parseLocalPort(fields[4])
			if port > 0 {
				if _, ok := seen[port]; !ok {
					seen[port] = struct{}{}
					ports = append(ports, port)
				}
			}
		}
	}
	sort.Ints(ports)
	return ports
}

// portsByLsof 通过 lsof 解析（macOS，等价 Java MacOsProjectCommander.listNetstat）。
func portsByLsof(pid int) []int {
	output := Exec("", nil, "lsof -n -P -iTCP -sTCP:LISTEN 2>/dev/null | grep "+strconv.Itoa(pid)+" | head -20")
	if output == "" || strings.HasPrefix(output, "error") {
		return nil
	}
	ports := make([]int, 0, 4)
	seen := make(map[int]struct{}, 4)
	for _, line := range strings.Split(output, "\n") {
		fields := strings.Fields(line)
		if len(fields) < 9 {
			continue
		}
		port := parseLocalPort(fields[8])
		if port > 0 {
			if _, ok := seen[port]; !ok {
				seen[port] = struct{}{}
				ports = append(ports, port)
			}
		}
	}
	sort.Ints(ports)
	return ports
}

// portsByNetstatWindows 通过 netstat 解析（Windows，等价 Java WindowsProjectCommander.listNetstat）。
func portsByNetstatWindows(pid int) []int {
	output := Exec("", nil, "netstat -nao -p tcp 2>nul | findstr \"LISTENING\" | findstr "+strconv.Itoa(pid))
	if output == "" || strings.HasPrefix(output, "error") {
		return nil
	}
	ports := make([]int, 0, 4)
	seen := make(map[int]struct{}, 4)
	for _, line := range strings.Split(output, "\n") {
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}
		port := parseLocalPort(fields[1])
		if port > 0 {
			if _, ok := seen[port]; !ok {
				seen[port] = struct{}{}
				ports = append(ports, port)
			}
		}
	}
	sort.Ints(ports)
	return ports
}

// parseLocalPort 从 "0.0.0.0:8080" / ":::18000" / "*:2123" / "127.0.0.1:8080" 中解析端口。
// 与 Java AbstractProjectCommander.getPortFormLocalIp 一致：只认可 0.0.0.0、*、::: 或纯端口形式。
func parseLocalPort(local string) int {
	local = strings.TrimSuffix(local, "(LISTEN)")
	local = strings.TrimSpace(local)
	if local == "" {
		return 0
	}
	parts := strings.Split(local, ":")
	cleaned := make([]string, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part != "" {
			cleaned = append(cleaned, part)
		}
	}
	if len(cleaned) == 0 {
		return 0
	}
	if len(cleaned) > 1 {
		first := cleaned[0]
		if first != "0.0.0.0" && first != "*" {
			// 绑定在具体 IP 上，Java 认为不是本地主端口
			return 0
		}
	}
	port, err := strconv.Atoi(cleaned[len(cleaned)-1])
	if err != nil || port <= 0 || port > 65535 {
		return 0
	}
	return port
}
