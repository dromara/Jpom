//go:build linux

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
	"os"
	"os/user"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
	"time"
)

// listProcesses 读取 /proc 下全部进程信息（字段语义对齐 Oshi OSProcess）。
func listProcesses() ([]rawProcess, error) {
	entries, err := os.ReadDir("/proc")
	if err != nil {
		return nil, err
	}
	uptimeSeconds := float64(0)
	if uptime, err := readUptime(); err == nil {
		uptimeSeconds = float64(uptime)
	}
	cpuNum := cpuCount()
	now := time.Now().UnixMilli()
	processes := make([]rawProcess, 0, len(entries))
	for _, entry := range entries {
		pid, err := strconv.Atoi(entry.Name())
		if err != nil || pid <= 0 {
			continue
		}
		process, err := readProcess(pid, uptimeSeconds, cpuNum, now)
		if err != nil {
			continue
		}
		processes = append(processes, process)
	}
	return processes, nil
}

func readProcess(pid int, uptimeSeconds float64, cpuNum int, now int64) (rawProcess, error) {
	base := filepath.Join("/proc", strconv.Itoa(pid))
	statContent, err := os.ReadFile(filepath.Join(base, "stat"))
	if err != nil {
		return rawProcess{}, err
	}
	process := rawProcess{Pid: pid}
	// comm 可能包含空格与括号，取最后一个 ')' 之后的内容。
	stat := string(statContent)
	end := strings.LastIndex(stat, ")")
	process.Name = readTrimmed(filepath.Join(base, "comm"))
	if end > 0 {
		start := strings.Index(stat, "(")
		if start >= 0 && start < end {
			process.Name = stat[start+1 : end]
		}
		fields := strings.Fields(stat[end+1:])
		if len(fields) > 21 {
			process.State = mapProcessState(fields[0])
			if utime, err := strconv.ParseUint(fields[11], 10, 64); err == nil {
				process.UserTimeMs = int64(ticksToMillis(utime))
			}
			if stime, err := strconv.ParseUint(fields[12], 10, 64); err == nil {
				process.SystemTimeMs = int64(ticksToMillis(stime))
			}
			if priority, err := strconv.Atoi(fields[15]); err == nil {
				process.Priority = priority
			}
			if starttime, err := strconv.ParseUint(fields[19], 10, 64); err == nil {
				elapsed := uptimeSeconds - float64(starttime)/float64(usrHz)
				if elapsed < 0 {
					elapsed = 0
				}
				process.StartTimeMs = now - int64(elapsed*1000)
				process.UpTimeMs = now - process.StartTimeMs
				if elapsed > 0 && cpuNum > 0 {
					usedTicks := float64(process.UserTimeMs+process.SystemTimeMs) / 1000 * float64(usrHz)
					totalTicks := elapsed * float64(cpuNum) * float64(usrHz)
					if totalTicks > 0 {
						process.CPULoad = usedTicks / totalTicks
					}
				}
			}
			if virtualSize, err := strconv.ParseUint(fields[20], 10, 64); err == nil {
				process.VirtualSize = virtualSize
			}
			if resident, err := strconv.ParseInt(fields[21], 10, 64); err == nil && resident > 0 {
				process.ResidentSize = uint64(resident) * uint64(os.Getpagesize())
			}
		}
	}
	if process.Name == "" {
		process.Name = "unknown"
	}
	if process.State == "" {
		process.State = "OTHER"
	}
	process.User = processOwner(pid)
	process.CommandLine = readCommandLine(base)
	process.Path = readExecutable(base)
	process.BytesRead, process.BytesWritten = readProcessIO(base)
	process.OpenFiles = countDir(filepath.Join(base, "fd"))
	process.SoftLimit, process.HardLimit = readOpenFileLimits(filepath.Join(base, "limits"))
	// userTime 在 Java 端存在历史 bug（取的是 upTime），这里保持一致。
	if process.UserTimeMs == 0 {
		process.UserTimeMs = process.UpTimeMs
	}
	return process, nil
}

func mapProcessState(state string) string {
	switch state {
	case "R":
		return "RUNNING"
	case "S", "I":
		return "SLEEPING"
	case "D":
		return "WAITING"
	case "Z":
		return "ZOMBIE"
	case "T", "t":
		return "STOPPED"
	case "X", "x":
		return "DEAD"
	default:
		return "OTHER"
	}
}

func processOwner(pid int) string {
	info, err := os.Stat(filepath.Join("/proc", strconv.Itoa(pid)))
	if err != nil {
		return ""
	}
	stat, ok := info.Sys().(*syscall.Stat_t)
	if !ok {
		return ""
	}
	uid := strconv.FormatUint(uint64(stat.Uid), 10)
	if found, err := user.LookupId(uid); err == nil {
		return found.Username
	}
	return uid
}

func readCommandLine(base string) string {
	content, err := os.ReadFile(filepath.Join(base, "cmdline"))
	if err != nil {
		return ""
	}
	line := strings.TrimSpace(strings.ReplaceAll(string(content), "\x00", " "))
	return line
}

func readExecutable(base string) string {
	target, err := os.Readlink(filepath.Join(base, "exe"))
	if err != nil {
		return ""
	}
	return strings.TrimSuffix(target, " (deleted)")
}

func readProcessIO(base string) (uint64, uint64) {
	content, err := os.ReadFile(filepath.Join(base, "io"))
	if err != nil {
		return 0, 0
	}
	var readBytes, writeBytes uint64
	for _, line := range strings.Split(string(content), "\n") {
		key, value, found := strings.Cut(line, ":")
		if !found {
			continue
		}
		parsed, err := strconv.ParseUint(strings.TrimSpace(value), 10, 64)
		if err != nil {
			continue
		}
		switch strings.TrimSpace(key) {
		case "read_bytes":
			readBytes = parsed
		case "write_bytes":
			writeBytes = parsed
		}
	}
	return readBytes, writeBytes
}

func countDir(path string) uint64 {
	entries, err := os.ReadDir(path)
	if err != nil {
		return 0
	}
	return uint64(len(entries))
}

func readOpenFileLimits(path string) (uint64, uint64) {
	content, err := os.ReadFile(path)
	if err != nil {
		return 0, 0
	}
	for _, line := range strings.Split(string(content), "\n") {
		if !strings.HasPrefix(line, "Max open files") {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 5 {
			return 0, 0
		}
		soft, _ := strconv.ParseUint(fields[len(fields)-3], 10, 64)
		hard, _ := strconv.ParseUint(fields[len(fields)-2], 10, 64)
		return soft, hard
	}
	return 0, 0
}
