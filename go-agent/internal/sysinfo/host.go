/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// Package sysinfo collects the system information exposed by the Jpom agent
// (/info, /get-stat-info, /processList, /disk-info, /hw-disk--info and
// /network-interfaces) as well as the platform side data files (system config,
// whitelist, workspace environment variables, logs and upgrades).
//
// 字段名与类型严格对齐 Java 端 OshiUtils：Java 通过 Jackson 输出，Long 一律序列化
// 为字符串、null 字段不输出，因此这里的 long 值全部格式化为字符串。
package sysinfo

import (
	"math"
	"sort"
	"strconv"
	"time"

	"jpom.io/go-agent/internal/config"
)

// 与 OshiUtils 中常量保持一致（毫秒）。
const (
	NetStatSleep = 1000
	CpuStatSleep = 1000
	SystemSleep  = NetStatSleep + CpuStatSleep
)

// CPU 采样结果（百分比保留两位小数，与 hutool CpuInfo 一致）。
type cpuSample struct {
	Num   int
	Model string
	Ticks CPUTicks
}

// CPUTicks 为两次采样之间的增量，保持 hutool CpuTicks 的字段名。
type CPUTicks struct {
	Idle    uint64
	Nice    uint64
	IRQ     uint64
	SoftIRQ uint64
	Steal   uint64
	System  uint64
	User    uint64
	IOWait  uint64
}

// Total 返回所有 tick 之和。
func (t CPUTicks) Total() uint64 {
	return t.Idle + t.Nice + t.IRQ + t.SoftIRQ + t.Steal + t.System + t.User + t.IOWait
}

type memoryStat struct {
	Total        uint64
	Available    uint64
	Free         uint64
	Buffers      uint64
	Cached       uint64
	SwapTotal    uint64
	SwapFree     uint64
	VirtualMax   uint64
	VirtualInUse uint64
}

type mountInfo struct {
	Device  string
	Mount   string
	Type    string
	Options string
}

type fileStore struct {
	Name          string
	Volume        string
	LogicalVolume string
	Mount         string
	Label         string
	Description   string
	Type          string
	Options       string
	UUID          string
	FreeInodes    uint64
	TotalInodes   uint64
	FreeSpace     uint64
	TotalSpace    uint64
	UsableSpace   uint64
}

type diskPartition struct {
	Identification string
	Name           string
	Type           string
	Major          int
	Minor          int
	Size           uint64
	MountPoint     string
	UUID           string
}

type diskStore struct {
	Name               string
	Model              string
	Serial             string
	Size               uint64
	ReadBytes          uint64
	Reads              uint64
	WriteBytes         uint64
	Writes             uint64
	CurrentQueueLength uint64
	TransferTime       uint64
	Partitions         []diskPartition
}

type netIface struct {
	Name          string
	DisplayName   string
	Index         int
	MacAddr       string
	InDrops       uint64
	InErrors      uint64
	BytesRecv     uint64
	BytesSent     uint64
	PacketsRecv   uint64
	PacketsSent   uint64
	Collisions    uint64
	IfAlias       string
	IfType        int
	IfOperStatus  string
	IPv4          []string
	IPv6          []string
	MTU           uint64
	OutErrors     uint64
	Speed         uint64
	SubnetMasks   []int
	PrefixLengths []int
}

type rawProcess struct {
	Pid          int
	Name         string
	State        string
	User         string
	CommandLine  string
	Path         string
	Priority     int
	StartTimeMs  int64
	UpTimeMs     int64
	UserTimeMs   int64
	SystemTimeMs int64
	VirtualSize  uint64
	ResidentSize uint64
	BytesRead    uint64
	BytesWritten uint64
	OpenFiles    uint64
	SoftLimit    uint64
	HardLimit    uint64
	CPULoad      float64
}

// round2 与 hutool 的 load 格式化一致（保留两位小数）。
func round2(value float64) float64 {
	return math.Round(value*100) / 100
}

func percent(part, total uint64) float64 {
	if total == 0 {
		return 0
	}
	return round2(float64(part) / float64(total) * 100)
}

func ticksJSON(t CPUTicks) map[string]any {
	return map[string]any{
		"idle":    strconv.FormatUint(t.Idle, 10),
		"nice":    strconv.FormatUint(t.Nice, 10),
		"irq":     strconv.FormatUint(t.IRQ, 10),
		"softIrq": strconv.FormatUint(t.SoftIRQ, 10),
		"steal":   strconv.FormatUint(t.Steal, 10),
		"cSys":    strconv.FormatUint(t.System, 10),
		"user":    strconv.FormatUint(t.User, 10),
		"ioWait":  strconv.FormatUint(t.IOWait, 10),
	}
}

// sampleCPU 读取两次 tick 并返回增量信息（默认休眠 1 秒）。
func sampleCPU(sleepMs int) (cpuSample, error) {
	before, err := readCPUTicks()
	if err != nil {
		return cpuSample{}, err
	}
	time.Sleep(time.Duration(sleepMs) * time.Millisecond)
	after, err := readCPUTicks()
	if err != nil {
		return cpuSample{}, err
	}
	delta := CPUTicks{
		Idle:    after.Idle - before.Idle,
		Nice:    after.Nice - before.Nice,
		IRQ:     after.IRQ - before.IRQ,
		SoftIRQ: after.SoftIRQ - before.SoftIRQ,
		Steal:   after.Steal - before.Steal,
		System:  after.System - before.System,
		User:    after.User - before.User,
		IOWait:  after.IOWait - before.IOWait,
	}
	return cpuSample{Num: cpuCount(), Model: cpuModel(), Ticks: delta}, nil
}

// SimpleStatus 对应 OshiUtils.getSimpleInfo 的输出。
func SimpleStatus(monitor *config.Monitor) (map[string]any, error) {
	sample, err := sampleCPU(CpuStatSleep)
	if err != nil {
		return nil, err
	}
	mem, err := readMemory()
	if err != nil {
		return nil, err
	}
	stores, err := listFileStores()
	if err != nil {
		return nil, err
	}
	var diskTotal, diskUsed uint64
	for _, store := range stores {
		diskTotal += store.TotalSpace
		if store.TotalSpace > store.UsableSpace {
			diskUsed += store.TotalSpace - store.UsableSpace
		}
	}
	filter := NewNetworkFilter(monitor)
	startNet, err := readNetStats(filter, true)
	if err != nil {
		return nil, err
	}
	time.Sleep(time.Duration(NetStatSleep) * time.Millisecond)
	endNet, err := readNetStats(filter, false)
	if err != nil {
		return nil, err
	}
	total := sample.Ticks.Total()
	cpuInfo := map[string]any{
		"cpuNum":   sample.Num,
		"toTal":    float64(total),
		"sys":      percent(sample.Ticks.System, total),
		"user":     percent(sample.Ticks.User, total),
		"wait":     percent(sample.Ticks.IOWait, total),
		"free":     percent(sample.Ticks.Idle, total),
		"cpuModel": sample.Model,
		"ticks":    ticksJSON(sample.Ticks),
	}
	cpuInfo["used"] = round2(100 - cpuInfo["free"].(float64))
	status := map[string]any{
		"time":            strconv.FormatInt(time.Now().UnixMilli(), 10),
		"cpu":             cpuInfo["used"],
		"cpuInfo":         cpuInfo,
		"memory":          percent(mem.Total-mem.Available, mem.Total),
		"disk":            percent(diskUsed, diskTotal),
		"netTxBytes":      strconv.FormatUint(subUint(endNet.BytesSent, startNet.BytesSent), 10),
		"netRxBytes":      strconv.FormatUint(subUint(endNet.BytesRecv, startNet.BytesRecv), 10),
		"monitorIfsNames": endNet.IfsNames,
	}
	if mem.SwapTotal > 0 {
		status["swapMemory"] = percent(mem.SwapTotal-mem.SwapFree, mem.SwapTotal)
	}
	if mem.VirtualMax > 0 {
		status["virtualMemory"] = percent(mem.VirtualInUse, mem.VirtualMax)
	}
	return status, nil
}

func subUint(a, b uint64) uint64 {
	if a < b {
		return 0
	}
	return a - b
}

// SystemInfo 对应 OshiUtils.getSystemInfo 的输出。
func SystemInfo() (map[string]any, error) {
	uptime, err := readUptime()
	if err != nil {
		return nil, err
	}
	mem, err := readMemory()
	if err != nil {
		return nil, err
	}
	hostName, err := readHostName()
	if err != nil {
		return nil, err
	}
	stores, err := listFileStores()
	if err != nil {
		return nil, err
	}
	var storeTotal uint64
	for _, store := range stores {
		storeTotal += store.TotalSpace
	}
	return map[string]any{
		"systemUptime":        strconv.FormatInt(uptime, 10),
		"osVersion":           osVersion(),
		"hostName":            hostName,
		"hardwareVersion":     hardwareVersion(),
		"hostIpv4s":           localIPv4s(),
		"osCpuIdentifierName": cpuIdentifierName(),
		"osCpuCores":          cpuCount(),
		"osMoneyTotal":        strconv.FormatUint(mem.Total, 10),
		"osSwapTotal":         strconv.FormatUint(mem.SwapTotal, 10),
		"osVirtualMax":        strconv.FormatUint(mem.VirtualMax, 10),
		"osLoadAverage":       loadAverage(),
		"osFileStoreTotal":    strconv.FormatUint(storeTotal, 10),
	}, nil
}

// FileStoresJSON 对应 OshiUtils.fileStores。
func FileStoresJSON() ([]map[string]any, error) {
	stores, err := listFileStores()
	if err != nil {
		return nil, err
	}
	list := make([]map[string]any, 0, len(stores))
	for _, store := range stores {
		list = append(list, map[string]any{
			"name":          store.Name,
			"volume":        store.Volume,
			"logicalVolume": store.LogicalVolume,
			"mount":         store.Mount,
			"label":         store.Label,
			"description":   store.Description,
			"type":          store.Type,
			"options":       store.Options,
			"uuid":          store.UUID,
			"freeInodes":    strconv.FormatUint(store.FreeInodes, 10),
			"totalInodes":   strconv.FormatUint(store.TotalInodes, 10),
			"freeSpace":     strconv.FormatUint(store.FreeSpace, 10),
			"totalSpace":    strconv.FormatUint(store.TotalSpace, 10),
			"usableSpace":   strconv.FormatUint(store.UsableSpace, 10),
		})
	}
	return list, nil
}

// DiskStoresJSON 对应 OshiUtils.diskStores。
func DiskStoresJSON() ([]map[string]any, error) {
	stores, err := listDiskStores()
	if err != nil {
		return nil, err
	}
	now := strconv.FormatInt(time.Now().UnixMilli(), 10)
	list := make([]map[string]any, 0, len(stores))
	for _, disk := range stores {
		partitions := make([]map[string]any, 0, len(disk.Partitions))
		for _, partition := range disk.Partitions {
			partitions = append(partitions, map[string]any{
				"identification": partition.Identification,
				"name":           partition.Name,
				"type":           partition.Type,
				"major":          partition.Major,
				"minor":          partition.Minor,
				"size":           strconv.FormatUint(partition.Size, 10),
				"mountPoint":     partition.MountPoint,
				"uuid":           partition.UUID,
			})
		}
		list = append(list, map[string]any{
			"name":               disk.Name,
			"model":              disk.Model,
			"serial":             disk.Serial,
			"size":               strconv.FormatUint(disk.Size, 10),
			"readBytes":          strconv.FormatUint(disk.ReadBytes, 10),
			"reads":              strconv.FormatUint(disk.Reads, 10),
			"writeBytes":         strconv.FormatUint(disk.WriteBytes, 10),
			"writes":             strconv.FormatUint(disk.Writes, 10),
			"currentQueueLength": strconv.FormatUint(disk.CurrentQueueLength, 10),
			"transferTime":       strconv.FormatUint(disk.TransferTime, 10),
			"timeStamp":          now,
			"partition":          partitions,
		})
	}
	return list, nil
}

// NetworkInterfacesJSON 对应 OshiUtils.networkInterfaces。
func NetworkInterfacesJSON() ([]map[string]any, error) {
	ifaces, err := listNetworkInterfaces()
	if err != nil {
		return nil, err
	}
	now := strconv.FormatInt(time.Now().UnixMilli(), 10)
	list := make([]map[string]any, 0, len(ifaces))
	for _, item := range ifaces {
		ipv4 := item.IPv4
		if ipv4 == nil {
			ipv4 = []string{}
		}
		ipv6 := item.IPv6
		if ipv6 == nil {
			ipv6 = []string{}
		}
		masks := item.SubnetMasks
		if masks == nil {
			masks = []int{}
		}
		prefixes := item.PrefixLengths
		if prefixes == nil {
			prefixes = []int{}
		}
		list = append(list, map[string]any{
			"name":           item.Name,
			"displayName":    item.DisplayName,
			"index":          item.Index,
			"macaddr":        item.MacAddr,
			"timeStamp":      now,
			"tnDrops":        strconv.FormatUint(item.InDrops, 10),
			"inErrors":       strconv.FormatUint(item.InErrors, 10),
			"bytesRecv":      strconv.FormatUint(item.BytesRecv, 10),
			"bytesSent":      strconv.FormatUint(item.BytesSent, 10),
			"packetsRecv":    strconv.FormatUint(item.PacketsRecv, 10),
			"packetsSent":    strconv.FormatUint(item.PacketsSent, 10),
			"collisions":     strconv.FormatUint(item.Collisions, 10),
			"ifAlias":        item.IfAlias,
			"ifType":         item.IfType,
			"ifOperStatus":   item.IfOperStatus,
			"ipv4addr":       ipv4,
			"ipv6addr":       ipv6,
			"mtu":            strconv.FormatUint(item.MTU, 10),
			"outErrors":      strconv.FormatUint(item.OutErrors, 10),
			"speed":          strconv.FormatUint(item.Speed, 10),
			"subnetMasks":    masks,
			"prefixLengths":  prefixes,
			"knownVmMacAddr": false,
		})
	}
	return list, nil
}

// ProcessListJSON 对应 OshiUtils.getProcesses + IndexController 补充的 port 字段。
func ProcessListJSON(name string, count int) ([]map[string]any, error) {
	processes, err := listProcesses()
	if err != nil {
		return nil, err
	}
	matched := make([]rawProcess, 0, len(processes))
	for _, process := range processes {
		if !equalFold(process.Name, name) {
			continue
		}
		matched = append(matched, process)
	}
	sort.SliceStable(matched, func(i, j int) bool {
		return matched[i].CPULoad > matched[j].CPULoad
	})
	if count > 0 && len(matched) > count {
		matched = matched[:count]
	}
	list := make([]map[string]any, 0, len(matched))
	for _, process := range matched {
		list = append(list, map[string]any{
			"processCpuLoadCumulative": process.CPULoad,
			"hardOpenFileLimit":        strconv.FormatUint(process.HardLimit, 10),
			"softOpenFileLimit":        strconv.FormatUint(process.SoftLimit, 10),
			"openFiles":                strconv.FormatUint(process.OpenFiles, 10),
			"processId":                process.Pid,
			"bytesWritten":             strconv.FormatUint(process.BytesWritten, 10),
			"bytesRead":                strconv.FormatUint(process.BytesRead, 10),
			"startTime":                strconv.FormatInt(process.StartTimeMs, 10),
			"upTime":                   strconv.FormatInt(process.UpTimeMs, 10),
			"userTime":                 strconv.FormatInt(process.UserTimeMs, 10),
			"user":                     process.User,
			"state":                    process.State,
			"name":                     process.Name,
			"virtualSize":              strconv.FormatUint(process.VirtualSize, 10),
			"commandLine":              process.CommandLine,
			"priority":                 process.Priority,
			"path":                     process.Path,
			"residentSetSize":          strconv.FormatUint(process.ResidentSize, 10),
			"port":                     PortOfProcess(process.Pid),
		})
	}
	return list, nil
}

// JavaProcessCount 与 JvmUtil.getJavaVirtualCount 等价（统计 java 进程个数）。
func JavaProcessCount() int {
	processes, err := listProcesses()
	if err != nil {
		return 0
	}
	count := 0
	for _, process := range processes {
		if equalFold(process.Name, "java") {
			count++
		}
	}
	return count
}

// KillProcess 结束指定进程（Linux 为 kill <pid>，与 Java 端一致）。
func KillProcess(pid int) (string, error) {
	if err := killPID(pid); err != nil {
		return "", err
	}
	return "", nil
}
