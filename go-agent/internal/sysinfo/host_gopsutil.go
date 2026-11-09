//go:build !linux

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
	"context"
	"fmt"
	"net"
	"os"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/shirou/gopsutil/v4/cpu"
	"github.com/shirou/gopsutil/v4/disk"
	"github.com/shirou/gopsutil/v4/host"
	"github.com/shirou/gopsutil/v4/load"
	"github.com/shirou/gopsutil/v4/mem"
	gnet "github.com/shirou/gopsutil/v4/net"
	"github.com/shirou/gopsutil/v4/process"
)

// 本文件是 Windows/macOS（以及其它非 Linux 平台）的系统信息采集实现，
// 基于 gopsutil v4.25.1，字段名/类型与 Linux（/proc）实现保持一致。

func readCPUTicks() (CPUTicks, error) {
	times, err := cpu.Times(false)
	if err != nil {
		return CPUTicks{}, err
	}
	var ticks CPUTicks
	for _, stat := range times {
		// gopsutil 的 Times 以秒为单位，换算为毫秒。
		ticks.User += uint64(stat.User * 1000)
		ticks.Nice += uint64(stat.Nice * 1000)
		ticks.System += uint64(stat.System * 1000)
		ticks.Idle += uint64(stat.Idle * 1000)
		ticks.IOWait += uint64(stat.Iowait * 1000)
		ticks.IRQ += uint64(stat.Irq * 1000)
		ticks.SoftIRQ += uint64(stat.Softirq * 1000)
		ticks.Steal += uint64(stat.Steal * 1000)
	}
	return ticks, nil
}

func cpuCount() int {
	if count, err := cpu.Counts(true); err == nil && count > 0 {
		return count
	}
	return runtime.NumCPU()
}

func cpuModelName() string {
	infos, err := cpu.Info()
	if err != nil || len(infos) == 0 {
		return ""
	}
	return strings.TrimSpace(infos[0].ModelName)
}

func cpuIdentifierName() string {
	if model := cpuModelName(); model != "" {
		return model
	}
	return "unknown"
}

func cpuModel() string {
	model := cpuModelName()
	if model == "" {
		model = "unknown"
	}
	logical := cpuCount()
	physical := logical
	if counts, err := cpu.Counts(false); err == nil && counts > 0 {
		physical = counts
	}
	packages := 1
	if physical > 0 && logical%physical == 0 && physical < logical {
		packages = logical / physical
	}
	return fmt.Sprintf("%s\n %d physical CPU package(s)\n %d physical CPU core(s)\n %d logical CPU(s)",
		model, packages, physical, logical)
}

func readMemory() (memoryStat, error) {
	virtual, err := mem.VirtualMemory()
	if err != nil {
		return memoryStat{}, err
	}
	swap, _ := mem.SwapMemory()
	stat := memoryStat{
		Total:     virtual.Total,
		Available: virtual.Available,
		Free:      virtual.Free,
		Buffers:   virtual.Buffers,
		Cached:    virtual.Cached,
	}
	if swap != nil {
		stat.SwapTotal = swap.Total
		stat.SwapFree = swap.Free
	}
	stat.VirtualMax = stat.Total
	stat.VirtualInUse = virtual.Used
	return stat, nil
}

func readUptime() (int64, error) {
	info, err := host.Info()
	if err != nil {
		return 0, err
	}
	return int64(info.Uptime), nil
}

func readHostName() (string, error) {
	info, err := host.Info()
	if err != nil {
		return "", err
	}
	if strings.TrimSpace(info.Hostname) != "" {
		return info.Hostname, nil
	}
	return os.Hostname()
}

func osVersion() string {
	info, err := host.Info()
	if err != nil {
		return ""
	}
	platform := strings.TrimSpace(info.Platform)
	version := strings.TrimSpace(info.PlatformVersion)
	if platform == "" {
		platform = strings.TrimSpace(info.OS)
	}
	parts := make([]string, 0, 4)
	if osName := strings.TrimSpace(info.OS); osName != "" {
		parts = append(parts, osName)
	}
	if platform != "" {
		parts = append(parts, platform)
	}
	if version != "" {
		parts = append(parts, version)
	}
	if kernel := strings.TrimSpace(info.KernelVersion); kernel != "" {
		parts = append(parts, "build", kernel)
	}
	return strings.Join(parts, " ")
}

func hardwareVersion() string {
	info, err := host.Info()
	if err != nil {
		return ""
	}
	parts := make([]string, 0, 3)
	for _, value := range []string{info.Platform, info.PlatformVersion, info.KernelArch} {
		if value = strings.TrimSpace(value); value != "" {
			parts = append(parts, value)
		}
	}
	return strings.Join(parts, " ")
}

func localIPv4s() []string {
	interfaces, err := net.Interfaces()
	if err != nil {
		return []string{}
	}
	list := make([]string, 0, 8)
	for _, iface := range interfaces {
		addrs, err := iface.Addrs()
		if err != nil {
			continue
		}
		for _, addr := range addrs {
			ipNet, ok := addr.(*net.IPNet)
			if !ok {
				continue
			}
			ip := ipNet.IP
			if ip.To4() == nil || ip.IsLoopback() {
				continue
			}
			list = append(list, ip.String())
		}
	}
	return list
}

func loadAverage() []float64 {
	avg, err := load.Avg()
	if err != nil {
		return []float64{0, 0, 0}
	}
	return []float64{round2(avg.Load1), round2(avg.Load5), round2(avg.Load15)}
}

func listFileStores() ([]fileStore, error) {
	partitions, err := disk.Partitions(false)
	if err != nil {
		return nil, err
	}
	stores := make([]fileStore, 0, len(partitions))
	for _, partition := range partitions {
		usage, err := disk.Usage(partition.Mountpoint)
		if err != nil {
			continue
		}
		name := partition.Device
		if partition.Mountpoint == "/" || strings.EqualFold(partition.Mountpoint, "\\") {
			name = partition.Mountpoint
		}
		stores = append(stores, fileStore{
			Name:        name,
			Volume:      partition.Device,
			Mount:       partition.Mountpoint,
			Label:       "",
			Description: "Local Disk",
			Type:        partition.Fstype,
			Options:     strings.Join(partition.Opts, ","),
			UUID:        "",
			FreeInodes:  usage.InodesFree,
			TotalInodes: usage.InodesTotal,
			FreeSpace:   usage.Free,
			TotalSpace:  usage.Total,
			UsableSpace: usage.Free,
		})
	}
	return stores, nil
}

// listDiskStores 在非 Linux 平台用分区信息近似 Java HWDiskStore 的字段结构
// （model/serial/读写统计 gopsutil 跨平台不提供，置空/0，字段名与类型保持一致）。
func listDiskStores() ([]diskStore, error) {
	partitions, err := disk.Partitions(true)
	if err != nil {
		return nil, err
	}
	byDisk := map[string]*diskStore{}
	for _, partition := range partitions {
		device := strings.TrimSpace(partition.Device)
		if device == "" {
			continue
		}
		diskName := physicalDiskName(device)
		store, ok := byDisk[diskName]
		if !ok {
			store = &diskStore{Name: diskName}
			byDisk[diskName] = store
		}
		usage, usageErr := disk.Usage(partition.Mountpoint)
		var size uint64
		mountPoint := partition.Mountpoint
		if usageErr == nil {
			size = usage.Total
			store.Size += usage.Total
		}
		partType := partition.Fstype
		store.Partitions = append(store.Partitions, diskPartition{
			Identification: device,
			Name:           device,
			Type:           partType,
			Size:           size,
			MountPoint:     mountPoint,
		})
	}
	names := make([]string, 0, len(byDisk))
	for name := range byDisk {
		names = append(names, name)
	}
	sort.Strings(names)
	stores := make([]diskStore, 0, len(names))
	for _, name := range names {
		stores = append(stores, *byDisk[name])
	}
	return stores, nil
}

// physicalDiskName 从分区设备名推断物理磁盘名（/dev/sda1 -> /dev/sda）。
func physicalDiskName(device string) string {
	lower := strings.ToLower(device)
	if strings.HasPrefix(lower, "/dev/disk") && strings.Contains(lower, "s") {
		if index := strings.LastIndex(lower, "s"); index > 0 && index+1 < len(lower) {
			if _, err := strconv.Atoi(lower[index+1:]); err == nil {
				return device[:index]
			}
		}
	}
	if strings.HasPrefix(lower, "/dev/") {
		trimmed := strings.TrimRightFunc(device, func(r rune) bool { return r >= '0' && r <= '9' })
		if trimmed != "" && trimmed != device {
			return trimmed
		}
	}
	// Windows 形如 C: 的卷直接作为磁盘名。
	return device
}

func listNetworkInterfaces() ([]netIface, error) {
	interfaces, err := net.Interfaces()
	if err != nil {
		return nil, err
	}
	counters, _ := gnet.IOCounters(true)
	counterMap := map[string]gnet.IOCountersStat{}
	for _, counter := range counters {
		counterMap[counter.Name] = counter
	}
	list := make([]netIface, 0, len(interfaces))
	for _, iface := range interfaces {
		item := netIface{
			Name:         iface.Name,
			DisplayName:  iface.Name,
			Index:        iface.Index,
			MacAddr:      iface.HardwareAddr.String(),
			MTU:          uint64(iface.MTU),
			IfType:       1,
			IfOperStatus: "UNKNOWN",
		}
		if iface.Flags&net.FlagLoopback != 0 {
			item.IfType = 772
		}
		if iface.Flags&net.FlagUp != 0 {
			item.IfOperStatus = "UP"
		} else {
			item.IfOperStatus = "DOWN"
		}
		if item.MacAddr == "" || item.MacAddr == "00:00:00:00:00:00" {
			item.MacAddr = "unknown"
		}
		item.IPv4, item.IPv6, item.SubnetMasks, item.PrefixLengths = interfaceAddresses(iface)
		if counter, ok := counterMap[iface.Name]; ok {
			item.BytesRecv = counter.BytesRecv
			item.BytesSent = counter.BytesSent
			item.PacketsRecv = counter.PacketsRecv
			item.PacketsSent = counter.PacketsSent
			item.InErrors = counter.Errin
			item.OutErrors = counter.Errout
			item.InDrops = counter.Dropin
			item.Collisions = counter.Dropout
		}
		list = append(list, item)
	}
	sort.SliceStable(list, func(i, j int) bool { return list[i].Index > list[j].Index })
	return list, nil
}

func interfaceAddresses(iface net.Interface) ([]string, []string, []int, []int) {
	addrs, err := iface.Addrs()
	if err != nil {
		return []string{}, []string{}, []int{}, []int{}
	}
	ipv4 := make([]string, 0, 2)
	ipv6 := make([]string, 0, 2)
	masks := make([]int, 0, 2)
	prefixes := make([]int, 0, 2)
	for _, addr := range addrs {
		ipNet, ok := addr.(*net.IPNet)
		if !ok {
			continue
		}
		ones, _ := ipNet.Mask.Size()
		if ipv4Addr := ipNet.IP.To4(); ipv4Addr != nil {
			ipv4 = append(ipv4, ipv4Addr.String())
			masks = append(masks, ones)
			continue
		}
		if ipNet.IP.To16() != nil {
			ipv6 = append(ipv6, ipNet.IP.String())
			prefixes = append(prefixes, ones)
		}
	}
	return ipv4, ipv6, masks, prefixes
}

func readNetStats(filter *NetworkFilter, withNames bool) (netStatSum, error) {
	counters, err := gnet.IOCounters(true)
	if err != nil {
		return netStatSum{}, err
	}
	var sum netStatSum
	names := make([]string, 0, len(counters))
	for _, counter := range counters {
		if filter != nil && !filter.Match(counter.Name) {
			continue
		}
		sum.BytesRecv += counter.BytesRecv
		sum.BytesSent += counter.BytesSent
		sum.PacketsRecv += counter.PacketsRecv
		sum.PacketsSent += counter.PacketsSent
		names = append(names, counter.Name)
	}
	if withNames {
		sum.IfsNames = strings.Join(names, ",")
	}
	return sum, nil
}

func listProcesses() ([]rawProcess, error) {
	ctx := context.Background()
	processes, err := process.ProcessesWithContext(ctx)
	if err != nil {
		return nil, err
	}
	now := time.Now().UnixMilli()
	result := make([]rawProcess, 0, len(processes))
	for _, item := range processes {
		raw, err := readGopsutilProcess(item, now)
		if err != nil {
			continue
		}
		result = append(result, raw)
	}
	return result, nil
}

func readGopsutilProcess(item *process.Process, now int64) (rawProcess, error) {
	name, err := item.Name()
	if err != nil || name == "" {
		return rawProcess{}, fmt.Errorf("process name unavailable")
	}
	raw := rawProcess{Pid: int(item.Pid), Name: name}
	if statuses, err := item.Status(); err == nil && len(statuses) > 0 {
		raw.State = mapGopsutilState(statuses[0])
	} else {
		raw.State = "OTHER"
	}
	if username, err := item.Username(); err == nil {
		raw.User = username
	}
	if commandLine, err := item.Cmdline(); err == nil {
		raw.CommandLine = strings.TrimSpace(commandLine)
	}
	if executable, err := item.Exe(); err == nil {
		raw.Path = executable
	}
	if createTime, err := item.CreateTime(); err == nil && createTime > 0 {
		raw.StartTimeMs = createTime
		raw.UpTimeMs = now - createTime
	}
	if times, err := item.Times(); err == nil {
		raw.UserTimeMs = int64(times.User * 1000)
		raw.SystemTimeMs = int64(times.System * 1000)
	}
	if raw.UpTimeMs > 0 {
		cpuMillis := float64(raw.UserTimeMs + raw.SystemTimeMs)
		raw.CPULoad = cpuMillis / float64(raw.UpTimeMs)
		if raw.CPULoad > 1 {
			raw.CPULoad = 1
		}
	}
	if memoryInfo, err := item.MemoryInfo(); err == nil {
		raw.VirtualSize = memoryInfo.VMS
		raw.ResidentSize = memoryInfo.RSS
	}
	if ioCounters, err := item.IOCounters(); err == nil {
		raw.BytesRead = ioCounters.ReadBytes
		raw.BytesWritten = ioCounters.WriteBytes
	}
	if files, err := item.OpenFiles(); err == nil {
		raw.OpenFiles = uint64(len(files))
	}
	if limits, err := item.Rlimit(); err == nil && len(limits) > 0 {
		raw.SoftLimit = uint64(limits[0].Soft)
		raw.HardLimit = uint64(limits[0].Hard)
	}
	if nice, err := item.Nice(); err == nil && nice >= 0 {
		raw.Priority = int(nice)
	}
	if raw.UpTimeMs == 0 {
		raw.UserTimeMs = 0
	}
	return raw, nil
}

func mapGopsutilState(state string) string {
	switch strings.ToLower(strings.TrimSpace(state)) {
	case "running", "r":
		return "RUNNING"
	case "sleeping", "sleep", "s":
		return "SLEEPING"
	case "waiting", "wait", "d":
		return "WAITING"
	case "zombie", "z":
		return "ZOMBIE"
	case "stopped", "stop", "t":
		return "STOPPED"
	case "idle", "i":
		return "IDLE"
	default:
		return "OTHER"
	}
}

func killPID(pid int) error {
	if process, err := process.NewProcess(int32(pid)); err == nil {
		return process.Kill()
	}
	processHandle, err := os.FindProcess(pid)
	if err != nil {
		return err
	}
	return processHandle.Kill()
}

// listeningPorts 返回进程监听的端口（gopsutil 连接表）。
func listeningPorts(pid int) string {
	ports := make([]int, 0, 4)
	seen := map[int]struct{}{}
	for _, network := range []string{"tcp", "tcp6"} {
		connections, err := gnet.ConnectionsPid(network, int32(pid))
		if err != nil {
			continue
		}
		for _, connection := range connections {
			if connection.Status != "LISTEN" {
				continue
			}
			ip := strings.TrimSpace(connection.Laddr.IP)
			if ip != "" && ip != "0.0.0.0" && ip != "::" && ip != "*" {
				continue
			}
			if _, ok := seen[int(connection.Laddr.Port)]; ok {
				continue
			}
			seen[int(connection.Laddr.Port)] = struct{}{}
			ports = append(ports, int(connection.Laddr.Port))
		}
	}
	if len(ports) == 0 {
		return "-"
	}
	sort.Ints(ports)
	values := make([]string, 0, len(ports))
	for _, port := range ports {
		values = append(values, strconv.Itoa(port))
	}
	return strings.Join(values, ",")
}
