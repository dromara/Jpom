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
	"bufio"
	"os"
	"path/filepath"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"time"
)

// ListProcesses 按名称查询进程（等价 Java OshiUtils.getProcesses）。
//
// name 匹配进程名（忽略大小写），按 CPU 使用率倒序返回前 count 个；count <= 0 时默认 20。
// 返回 map 是为了与 Java JSONObject 输出字段一一对应：
// Java 侧 Jackson 把 long 序列化为字符串，因此 long 类型字段（startTime/upTime/
// virtualSize/openFiles/bytesRead 等）均为字符串，processId 与 priority 为数字。
func ListProcesses(name string, count int) []map[string]any {
	if count <= 0 {
		count = 20
	}
	if runtime.GOOS != "linux" {
		return listProcessesByPs(name, count)
	}
	boot := bootTimeMillis()
	pageSize := int64(os.Getpagesize())
	clockTicks := int64(100)

	result := make([]map[string]any, 0, 16)
	for _, pid := range listProcessPids() {
		item, ok := readProcess(pid, boot, pageSize, clockTicks)
		if !ok {
			continue
		}
		if !strings.EqualFold(item["name"].(string), name) {
			continue
		}
		result = append(result, item)
	}
	sort.Slice(result, func(i, j int) bool {
		left := result[i]["processCpuLoadCumulative"].(float64)
		right := result[j]["processCpuLoadCumulative"].(float64)
		if left == right {
			return result[i]["processId"].(int) < result[j]["processId"].(int)
		}
		return left > right
	})
	if len(result) > count {
		result = result[:count]
	}
	return result
}

// bootTimeMillis 读取 /proc/stat 的 btime（秒）并转成毫秒。
func bootTimeMillis() int64 {
	data, err := os.ReadFile("/proc/stat")
	if err != nil {
		return 0
	}
	for _, line := range strings.Split(string(data), "\n") {
		if !strings.HasPrefix(line, "btime ") {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			return 0
		}
		value, err := strconv.ParseInt(fields[1], 10, 64)
		if err != nil {
			return 0
		}
		return value * 1000
	}
	return 0
}

// readProcess 读取单个进程的监控信息。
func readProcess(pid int, bootTime, pageSize, clockTicks int64) (map[string]any, bool) {
	base := filepath.Join("/proc", strconv.Itoa(pid))
	statData, err := os.ReadFile(filepath.Join(base, "stat"))
	if err != nil {
		return nil, false
	}
	stat := string(statData)
	open := strings.IndexByte(stat, 40)
	closePos := strings.LastIndexByte(stat, 41)
	if open < 0 || closePos <= open {
		return nil, false
	}
	comm := stat[open+1 : closePos]
	rest := strings.Fields(stat[closePos+2:])
	if len(rest) < 22 {
		return nil, false
	}
	atoi := func(value string) int64 {
		v, _ := strconv.ParseInt(value, 10, 64)
		return v
	}
	utime := atoi(rest[11])
	stime := atoi(rest[12])
	priority := atoi(rest[15])
	startTicks := atoi(rest[19])
	vsize := atoi(rest[20])
	rssPages := atoi(rest[21])

	startMillis := bootTime + startTicks*1000/clockTicks
	if startMillis <= 0 {
		startMillis = time.Now().UnixMilli()
	}
	upTime := time.Now().UnixMilli() - startMillis
	if upTime < 0 {
		upTime = 0
	}
	cpuLoad := 0.0
	if upTime > 0 {
		cpuMillis := (utime + stime) * 1000 / clockTicks
		cpuLoad = float64(cpuMillis) / float64(upTime)
		if cpuLoad > 1 {
			cpuLoad = 1
		}
	}
	item := map[string]any{
		"processCpuLoadCumulative": cpuLoad,
		"processId":                pid,
		"priority":                 int(priority),
		"name":                     comm,
		"state":                    mapProcessState(rest[0]),
		"virtualSize":              strconv.FormatInt(vsize, 10),
		"residentSetSize":          strconv.FormatInt(rssPages*pageSize, 10),
		"startTime":                strconv.FormatInt(startMillis, 10),
		"upTime":                   strconv.FormatInt(upTime, 10),
		// Java 实现中 userTime 也取 upTime（复制原逻辑）
		"userTime":    strconv.FormatInt(upTime, 10),
		"commandLine": strings.Join(readCmdline(pid), " "),
	}
	if exe, err := os.Readlink(filepath.Join(base, "exe")); err == nil {
		item["path"] = strings.TrimSuffix(exe, " (deleted)")
	}
	item["user"] = readProcessUser(filepath.Join(base, "status"))
	if entries, err := os.ReadDir(filepath.Join(base, "fd")); err == nil {
		item["openFiles"] = strconv.Itoa(len(entries))
	}
	soft, hard := readOpenFileLimit(filepath.Join(base, "limits"))
	item["softOpenFileLimit"] = soft
	item["hardOpenFileLimit"] = hard
	readBytes, writeBytes := readProcessIO(filepath.Join(base, "io"))
	item["bytesRead"] = readBytes
	item["bytesWritten"] = writeBytes
	return item, true
}

// mapProcessState 把 /proc 状态字符映射为 Oshi State 枚举名。
func mapProcessState(state string) string {
	switch state {
	case "R":
		return "RUNNING"
	case "S":
		return "SLEEPING"
	case "D":
		return "WAITING"
	case "Z":
		return "ZOMBIE"
	case "T", "t":
		return "STOPPED"
	case "I":
		return "IDLE"
	default:
		return "OTHER"
	}
}

// readProcessUser 读取 /proc/<pid>/status 的 Uid 并转换为用户名。
func readProcessUser(statusPath string) string {
	file, err := os.Open(statusPath)
	if err != nil {
		return ""
	}
	defer func() { _ = file.Close() }()
	scanner := bufio.NewScanner(file)
	uid := ""
	for scanner.Scan() {
		line := scanner.Text()
		if !strings.HasPrefix(line, "Uid:") {
			continue
		}
		fields := strings.Fields(line)
		// Uid: real effective saved fs
		if len(fields) >= 3 {
			uid = fields[2]
		} else if len(fields) >= 2 {
			uid = fields[1]
		}
		break
	}
	if uid == "" {
		return ""
	}
	return lookupUserName(uid)
}

// lookupUserName 通过 /etc/passwd 把 uid 转成用户名（读不到时返回 uid）。
func lookupUserName(uid string) string {
	file, err := os.Open("/etc/passwd")
	if err != nil {
		return uid
	}
	defer func() { _ = file.Close() }()
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		fields := strings.Split(scanner.Text(), ":")
		if len(fields) >= 3 && fields[2] == uid {
			return fields[0]
		}
	}
	return uid
}

// readOpenFileLimit 读取 /proc/<pid>/limits 中 Max open files 的 soft/hard。
func readOpenFileLimit(path string) (soft, hard string) {
	data, err := os.ReadFile(path)
	if err != nil {
		return "", ""
	}
	for _, line := range strings.Split(string(data), "\n") {
		if !strings.HasPrefix(line, "Max open files") {
			continue
		}
		fields := strings.Fields(line)
		// Max open files <soft> <hard> files
		if len(fields) >= 5 {
			return fields[3], fields[4]
		}
	}
	return "", ""
}

// readProcessIO 读取 /proc/<pid>/io 的读写字节数。
func readProcessIO(path string) (read, write string) {
	data, err := os.ReadFile(path)
	if err != nil {
		return "", ""
	}
	for _, line := range strings.Split(string(data), "\n") {
		switch {
		case strings.HasPrefix(line, "read_bytes:"):
			read = strings.TrimSpace(strings.TrimPrefix(line, "read_bytes:"))
		case strings.HasPrefix(line, "write_bytes:"):
			write = strings.TrimSpace(strings.TrimPrefix(line, "write_bytes:"))
		}
	}
	return read, write
}

// listProcessesByPs 非 Linux 平台的简易实现（字段尽量对齐，缺失项为空字符串）。
func listProcessesByPs(name string, count int) []map[string]any {
	output := Exec("", nil, "ps -eo pid,user,pcpu,stat,comm,args")
	if output == "" || strings.HasPrefix(output, "error") {
		return nil
	}
	result := make([]map[string]any, 0, 16)
	lines := strings.Split(output, "\n")
	for _, line := range lines[1:] {
		fields := strings.Fields(line)
		if len(fields) < 5 {
			continue
		}
		pid, err := strconv.Atoi(fields[0])
		if err != nil {
			continue
		}
		comm := fields[4]
		if !strings.EqualFold(comm, name) {
			continue
		}
		cpu, _ := strconv.ParseFloat(fields[2], 64)
		if cpu > 1 {
			cpu = cpu / 100
		}
		result = append(result, map[string]any{
			"processCpuLoadCumulative": cpu,
			"processId":                pid,
			"user":                     fields[1],
			"state":                    fields[3],
			"name":                     comm,
			"commandLine":              strings.Join(fields[5:], " "),
		})
		if len(result) >= count {
			break
		}
	}
	return result
}
