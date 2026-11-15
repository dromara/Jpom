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
	"bufio"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"syscall"
)

// readCPUTicks 读取 /proc/stat 第一行并按 Oshi 口径换算为毫秒 tick。
func readCPUTicks() (CPUTicks, error) {
	content, err := os.ReadFile("/proc/stat")
	if err != nil {
		return CPUTicks{}, err
	}
	scanner := bufio.NewScanner(strings.NewReader(string(content)))
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 9 || fields[0] != "cpu" {
			continue
		}
		values := make([]uint64, 8)
		for index := 0; index < 8; index++ {
			parsed, err := strconv.ParseUint(fields[index+1], 10, 64)
			if err != nil {
				return CPUTicks{}, err
			}
			values[index] = ticksToMillis(parsed)
		}
		// /proc/stat: user nice system idle iowait irq softirq steal
		return CPUTicks{
			User:    values[0],
			Nice:    values[1],
			System:  values[2],
			Idle:    values[3],
			IOWait:  values[4],
			IRQ:     values[5],
			SoftIRQ: values[6],
			Steal:   values[7],
		}, nil
	}
	return CPUTicks{}, fmt.Errorf("parse /proc/stat failed")
}

// cpuCount 返回逻辑 CPU 个数。
func cpuCount() int { return runtimeNumCPU() }

func runtimeNumCPU() int {
	count := 0
	if content, err := os.ReadFile("/proc/cpuinfo"); err == nil {
		for _, line := range strings.Split(string(content), "\n") {
			if strings.HasPrefix(line, "processor") {
				count++
			}
		}
	}
	if count <= 0 {
		count = 1
	}
	return count
}

// cpuIdentifierName 对应 processor.getProcessorIdentifier().getName()。
func cpuIdentifierName() string {
	if model := cpuModelName(); model != "" {
		return model
	}
	return "unknown"
}

func cpuModelName() string {
	content, err := os.ReadFile("/proc/cpuinfo")
	if err != nil {
		return ""
	}
	for _, line := range strings.Split(string(content), "\n") {
		key, value, found := strings.Cut(line, ":")
		if found && strings.TrimSpace(key) == "model name" {
			return strings.TrimSpace(value)
		}
	}
	return ""
}

// cpuModel 近似 Oshi CentralProcessor#toString() 的内容
// （型号 + 物理/逻辑核数 + ProcessorIdentifier 的 Family/Model/Stepping）。
func cpuModel() string {
	model := cpuModelName()
	if model == "" {
		model = "unknown"
	}
	logical := cpuCount()
	physical := logical
	processorID := map[string]string{}
	if content, err := os.ReadFile("/proc/cpuinfo"); err == nil {
		for _, line := range strings.Split(string(content), "\n") {
			key, value, found := strings.Cut(line, ":")
			if !found {
				continue
			}
			key = strings.TrimSpace(key)
			value = strings.TrimSpace(value)
			switch key {
			case "cpu cores":
				if parsed, err := strconv.Atoi(value); err == nil && parsed > 0 {
					physical = parsed
				}
			case "vendor_id", "cpu family", "model", "stepping":
				if _, exists := processorID[key]; !exists {
					processorID[key] = value
				}
			}
		}
	}
	packages := 1
	if physical > 0 && logical%physical == 0 && physical < logical {
		packages = logical / physical
	}
	result := fmt.Sprintf("%s\n %d physical CPU package(s)\n %d physical CPU core(s)\n %d logical CPU(s)",
		model, packages, physical, logical)
	if vendor := processorID["vendor_id"]; vendor != "" {
		family := valueOrDefault(processorID["cpu family"], "?")
		modelValue := valueOrDefault(processorID["model"], "?")
		stepping := valueOrDefault(processorID["stepping"], "?")
		result += fmt.Sprintf("\nIdentifier: %s Family %s Model %s Stepping %s", vendor, family, modelValue, stepping)
	}
	return result
}

func valueOrDefault(value, fallback string) string {
	if strings.TrimSpace(value) == "" {
		return fallback
	}
	return strings.TrimSpace(value)
}

func readMemory() (memoryStat, error) {
	content, err := os.ReadFile("/proc/meminfo")
	if err != nil {
		return memoryStat{}, err
	}
	values := map[string]uint64{}
	for _, line := range strings.Split(string(content), "\n") {
		key, value, found := strings.Cut(line, ":")
		if !found {
			continue
		}
		fields := strings.Fields(value)
		if len(fields) == 0 {
			continue
		}
		parsed, err := strconv.ParseUint(fields[0], 10, 64)
		if err != nil {
			continue
		}
		values[strings.TrimSpace(key)] = parsed * 1024
	}
	stat := memoryStat{
		Total:     values["MemTotal"],
		Free:      values["MemFree"],
		Buffers:   values["Buffers"],
		Cached:    values["Cached"],
		SwapTotal: values["SwapTotal"],
		SwapFree:  values["SwapFree"],
	}
	stat.Available = values["MemAvailable"]
	if stat.Available == 0 {
		stat.Available = stat.Free + stat.Buffers + stat.Cached
	}
	if stat.Total < stat.Available {
		stat.Available = stat.Total
	}
	// virtualMax/virtualInUse 对齐 Oshi 的 VirtualMemory 语义（不含可回收缓存）。
	stat.VirtualMax = stat.Total
	if stat.Total > stat.Free+stat.Buffers+stat.Cached {
		stat.VirtualInUse = stat.Total - stat.Free - stat.Buffers - stat.Cached
	}
	return stat, nil
}

func readUptime() (int64, error) {
	content, err := os.ReadFile("/proc/uptime")
	if err != nil {
		return 0, err
	}
	fields := strings.Fields(string(content))
	if len(fields) == 0 {
		return 0, fmt.Errorf("parse /proc/uptime failed")
	}
	value, err := strconv.ParseFloat(fields[0], 64)
	if err != nil {
		return 0, err
	}
	return int64(value), nil
}

func readHostName() (string, error) {
	name, err := os.Hostname()
	if err != nil || strings.TrimSpace(name) == "" {
		if content, readErr := os.ReadFile("/proc/sys/kernel/hostname"); readErr == nil {
			return strings.TrimSpace(string(content)), nil
		}
		if err != nil {
			return "", err
		}
	}
	return name, nil
}

func osVersion() string {
	name, version := "", ""
	if content, err := os.ReadFile("/etc/os-release"); err == nil {
		for _, line := range strings.Split(string(content), "\n") {
			key, value, found := strings.Cut(line, "=")
			if !found {
				continue
			}
			value = strings.Trim(strings.TrimSpace(value), "\"")
			switch key {
			case "NAME":
				name = value
			case "VERSION":
				version = value
			case "PRETTY_NAME":
				if name == "" {
					name = value
				}
			}
		}
	}
	release := "unknown"
	var uname syscall.Utsname
	if err := syscall.Uname(&uname); err == nil {
		release = charsToString(uname.Release[:])
	}
	parts := []string{"GNU/Linux"}
	if name != "" {
		parts = append(parts, name)
	}
	if version != "" {
		parts = append(parts, version)
	}
	parts = append(parts, "build", release)
	return strings.Join(parts, " ")
}

func charsToString(chars []int8) string {
	builder := make([]byte, 0, len(chars))
	for _, char := range chars {
		if char == 0 {
			break
		}
		builder = append(builder, byte(char))
	}
	return string(builder)
}

func hardwareVersion() string {
	vendor := readTrimmed("/sys/devices/virtual/dmi/id/sys_vendor")
	product := readTrimmed("/sys/devices/virtual/dmi/id/product_name")
	if vendor == "" && product == "" {
		return ""
	}
	result := strings.TrimSpace(vendor + " " + product)
	if version := readTrimmed("/sys/devices/virtual/dmi/id/product_version"); version != "" {
		result += " (version: " + version + ")"
	}
	return result
}

func localIPv4s() []string {
	ifaces, err := net.Interfaces()
	if err != nil {
		return []string{}
	}
	list := make([]string, 0, 8)
	for _, iface := range ifaces {
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
	content, err := os.ReadFile("/proc/loadavg")
	if err != nil {
		return []float64{0, 0, 0}
	}
	fields := strings.Fields(string(content))
	result := []float64{0, 0, 0}
	for index := 0; index < len(fields) && index < 3; index++ {
		value, err := strconv.ParseFloat(fields[index], 64)
		if err != nil {
			continue
		}
		result[index] = round2(value)
	}
	return result
}

var localFilesystemTypes = map[string]struct{}{
	"ext2": {}, "ext3": {}, "ext4": {}, "xfs": {}, "btrfs": {}, "zfs": {},
	"f2fs": {}, "jfs": {}, "reiserfs": {}, "vfat": {}, "fat": {}, "fat32": {},
	"ntfs": {}, "ntfs3": {}, "exfat": {}, "iso9660": {}, "udf": {}, "hfs": {},
	"hfsplus": {}, "apfs": {}, "ufs": {}, "zfs_member": {},
}

func readMounts() []mountInfo {
	content, err := os.ReadFile("/proc/self/mounts")
	if err != nil {
		return nil
	}
	var mounts []mountInfo
	for _, line := range strings.Split(string(content), "\n") {
		fields := strings.Fields(line)
		if len(fields) < 4 {
			continue
		}
		mounts = append(mounts, mountInfo{
			Device:  unescapeMount(fields[0]),
			Mount:   unescapeMount(fields[1]),
			Type:    fields[2],
			Options: fields[3],
		})
	}
	return mounts
}

func unescapeMount(value string) string {
	replacer := strings.NewReplacer("\040", " ", "\011", "\t", "\012", "\n", "\134", "\\")
	return replacer.Replace(value)
}

func listFileStores() ([]fileStore, error) {
	mounts := readMounts()
	uuidByDevice := uuidDeviceMap()
	stores := make([]fileStore, 0, len(mounts))
	for _, mount := range mounts {
		if _, ok := localFilesystemTypes[mount.Type]; !ok {
			continue
		}
		var stat syscall.Statfs_t
		if err := syscall.Statfs(mount.Mount, &stat); err != nil {
			continue
		}
		blockSize := uint64(stat.Bsize)
		name := mount.Device
		if mount.Mount == "/" {
			name = "/"
		}
		logicalVolume := ""
		if strings.HasPrefix(mount.Device, "/dev/mapper/") {
			if resolved, err := filepath.EvalSymlinks(mount.Device); err == nil {
				logicalVolume = resolved
			}
		}
		uuid := ""
		if resolved, err := filepath.EvalSymlinks(mount.Device); err == nil {
			uuid = uuidByDevice[resolved]
		}
		if uuid == "" {
			uuid = uuidByDevice[mount.Device]
		}
		stores = append(stores, fileStore{
			Name:          name,
			Volume:        mount.Device,
			LogicalVolume: logicalVolume,
			Mount:         mount.Mount,
			Label:         "",
			Description:   "Local Disk",
			Type:          mount.Type,
			Options:       mount.Options,
			UUID:          uuid,
			FreeInodes:    stat.Ffree,
			TotalInodes:   stat.Files,
			FreeSpace:     stat.Bfree * blockSize,
			TotalSpace:    stat.Blocks * blockSize,
			UsableSpace:   stat.Bavail * blockSize,
		})
	}
	return stores, nil
}

// uuidDeviceMap 建立 “真实设备路径 -> UUID” 映射（读取 /dev/disk/by-uuid）。
func uuidDeviceMap() map[string]string {
	result := map[string]string{}
	entries, err := os.ReadDir("/dev/disk/by-uuid")
	if err != nil {
		return result
	}
	for _, entry := range entries {
		target, err := filepath.EvalSymlinks(filepath.Join("/dev/disk/by-uuid", entry.Name()))
		if err != nil {
			continue
		}
		result[target] = entry.Name()
	}
	return result
}

func listDiskStores() ([]diskStore, error) {
	entries, err := os.ReadDir("/sys/block")
	if err != nil {
		return nil, err
	}
	mounts := readMounts()
	deviceMount := map[string]mountInfo{}
	for _, mount := range mounts {
		deviceMount[mount.Device] = mount
	}
	lvmSlaves := lvmSlaveMap()
	stores := make([]diskStore, 0, len(entries))
	for _, entry := range entries {
		name := entry.Name()
		if strings.HasPrefix(name, "loop") || strings.HasPrefix(name, "ram") ||
			strings.HasPrefix(name, "sr") || strings.HasPrefix(name, "fd") {
			continue
		}
		base := filepath.Join("/sys/block", name)
		disk := diskStore{
			Name:   filepath.Join("/dev", name),
			Model:  readTrimmed(filepath.Join(base, "device", "model")),
			Serial: readTrimmed(filepath.Join(base, "device", "serial")),
		}
		if disk.Model == "" {
			disk.Model = readTrimmed(filepath.Join(base, "device", "name"))
		}
		if size, err := readUintFile(filepath.Join(base, "size")); err == nil {
			disk.Size = size * 512
		}
		if strings.HasPrefix(name, "dm-") {
			disk.Model = "Logical Volume Group"
			if uuid := readTrimmed(filepath.Join(base, "dm", "uuid")); uuid != "" {
				disk.Serial = uuid
			}
		}
		if disk.Serial == "" {
			disk.Serial = readPrintableVpd(filepath.Join(base, "device", "vpd_pg80"))
		}
		readDiskStat(filepath.Join(base, "stat"), &disk)
		disk.Partitions = readPartitions(name, base, deviceMount, lvmSlaves)
		stores = append(stores, disk)
	}
	return stores, nil
}

func readDiskStat(path string, disk *diskStore) {
	content, err := os.ReadFile(path)
	if err != nil {
		return
	}
	fields := strings.Fields(string(content))
	if len(fields) < 11 {
		return
	}
	values := make([]uint64, len(fields))
	for index, field := range fields {
		parsed, err := strconv.ParseUint(field, 10, 64)
		if err != nil {
			continue
		}
		values[index] = parsed
	}
	disk.Reads = values[0]
	disk.ReadBytes = values[2] * 512
	disk.Writes = values[4]
	disk.WriteBytes = values[6] * 512
	disk.CurrentQueueLength = values[9]
	disk.TransferTime = values[3] + values[7]
}

// lvmSlaveMap 返回 物理分区 -> {dm 名称}，用于把 LVM PV 标记为 LVM2_member。
func lvmSlaveMap() map[string]string {
	result := map[string]string{}
	entries, err := os.ReadDir("/sys/block")
	if err != nil {
		return result
	}
	for _, entry := range entries {
		name := entry.Name()
		if !strings.HasPrefix(name, "dm-") {
			continue
		}
		slavesDir := filepath.Join("/sys/block", name, "slaves")
		slaves, err := os.ReadDir(slavesDir)
		if err != nil {
			continue
		}
		for _, slave := range slaves {
			result[filepath.Join("/dev", slave.Name())] = name
		}
	}
	return result
}

func readPartitions(diskName, base string, deviceMount map[string]mountInfo, lvmSlaves map[string]string) []diskPartition {
	entries, err := os.ReadDir(base)
	if err != nil {
		return []diskPartition{}
	}
	partitions := make([]diskPartition, 0, len(entries))
	for _, entry := range entries {
		if !strings.HasPrefix(entry.Name(), diskName) || entry.Name() == diskName {
			continue
		}
		partBase := filepath.Join(base, entry.Name())
		if _, err := os.Stat(filepath.Join(partBase, "partition")); err != nil {
			continue
		}
		device := filepath.Join("/dev", entry.Name())
		partition := diskPartition{
			Identification: device,
			Name:           partBase,
			Type:           "",
			MountPoint:     "",
		}
		if resolved, err := filepath.EvalSymlinks(partBase); err == nil {
			partition.Name = resolved
		}
		if major, minor, err := readDeviceNumbers(filepath.Join("/sys/class/block", entry.Name(), "dev")); err == nil {
			partition.Major = major
			partition.Minor = minor
		}
		if size, err := readUintFile(filepath.Join(partBase, "size")); err == nil {
			partition.Size = size * 512
		}
		if mount, ok := deviceMount[device]; ok {
			partition.Type = mount.Type
			partition.MountPoint = mount.Mount
		}
		if dmName, ok := lvmSlaves[device]; ok {
			partition.Type = "LVM2_member"
			partition.MountPoint = dmName
		}
		if uuid, ok := uuidDeviceMap()[device]; ok {
			partition.UUID = uuid
		}
		partitions = append(partitions, partition)
	}
	// LVM 逻辑卷（dm-*）在 /sys 下没有子分区，Oshi 会把它自身作为一个分区返回。
	if len(partitions) == 0 && strings.HasPrefix(diskName, "dm-") {
		if partition, ok := dmPartition(diskName, base, deviceMount); ok {
			partitions = append(partitions, partition)
		}
	}
	return partitions
}

// dmPartition 为 LVM 逻辑卷合成一个 HWPartition（对齐 Java DiskStore.partition 输出）。
func dmPartition(diskName, base string, deviceMount map[string]mountInfo) (diskPartition, bool) {
	dmName := readTrimmed(filepath.Join(base, "dm", "name"))
	if dmName == "" {
		return diskPartition{}, false
	}
	device := filepath.Join("/dev/mapper", dmName)
	partition := diskPartition{
		Identification: "/dev/" + strings.ReplaceAll(dmName, "--", "-"),
		Name:           base,
		MountPoint:     device,
	}
	if resolved, err := filepath.EvalSymlinks(base); err == nil {
		partition.Name = resolved
	}
	if major, minor, err := readDeviceNumbers(filepath.Join("/sys/class/block", diskName, "dev")); err == nil {
		partition.Major = major
		partition.Minor = minor
	}
	if size, err := readUintFile(filepath.Join(base, "size")); err == nil {
		partition.Size = size * 512
	}
	resolved := device
	if target, err := filepath.EvalSymlinks(device); err == nil {
		resolved = target
	}
	for _, mount := range deviceMount {
		mountResolved := mount.Device
		if target, err := filepath.EvalSymlinks(mount.Device); err == nil {
			mountResolved = target
		}
		if mountResolved == resolved {
			partition.Type = mount.Type
			partition.MountPoint = mount.Mount
			break
		}
	}
	if uuid, ok := uuidDeviceMap()[resolved]; ok {
		partition.UUID = uuid
	}
	return partition, true
}

func readDeviceNumbers(path string) (int, int, error) {
	content, err := os.ReadFile(path)
	if err != nil {
		return 0, 0, err
	}
	majorStr, minorStr, found := strings.Cut(strings.TrimSpace(string(content)), ":")
	if !found {
		return 0, 0, fmt.Errorf("parse device number failed: %s", path)
	}
	major, err := strconv.Atoi(majorStr)
	if err != nil {
		return 0, 0, err
	}
	minor, err := strconv.Atoi(minorStr)
	return major, minor, err
}

func readPrintableVpd(path string) string {
	content, err := os.ReadFile(path)
	if err != nil {
		return ""
	}
	builder := make([]byte, 0, len(content))
	for _, char := range content {
		if char >= 0x20 && char < 0x7f {
			builder = append(builder, char)
		}
	}
	return strings.TrimSpace(string(builder))
}

func readUintFile(path string) (uint64, error) {
	content, err := os.ReadFile(path)
	if err != nil {
		return 0, err
	}
	return strconv.ParseUint(strings.TrimSpace(string(content)), 10, 64)
}

func readTrimmed(path string) string {
	content, err := os.ReadFile(path)
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(content))
}

func readIntFile(path string, def int) int {
	value := readTrimmed(path)
	if value == "" {
		return def
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return def
	}
	return parsed
}

// readNetStats 汇总 /proc/net/dev 中符合过滤条件的网卡累计流量。
func readNetStats(filter *NetworkFilter, withNames bool) (netStatSum, error) {
	content, err := os.ReadFile("/proc/net/dev")
	if err != nil {
		return netStatSum{}, err
	}
	var sum netStatSum
	names := make([]string, 0, 8)
	for _, line := range strings.Split(string(content), "\n") {
		key, value, found := strings.Cut(line, ":")
		if !found {
			continue
		}
		name := strings.TrimSpace(key)
		if name == "" || strings.HasPrefix(name, "Inter-") || strings.HasPrefix(name, "face") {
			continue
		}
		if filter != nil && !filter.Match(name) {
			continue
		}
		fields := strings.Fields(value)
		if len(fields) < 16 {
			continue
		}
		values := make([]uint64, 16)
		for index := 0; index < 16; index++ {
			parsed, err := strconv.ParseUint(fields[index], 10, 64)
			if err != nil {
				continue
			}
			values[index] = parsed
		}
		sum.BytesRecv += values[0]
		sum.PacketsRecv += values[1]
		sum.BytesSent += values[8]
		sum.PacketsSent += values[9]
		names = append(names, name)
	}
	if withNames {
		sum.IfsNames = strings.Join(names, ",")
	}
	return sum, nil
}

func listNetworkInterfaces() ([]netIface, error) {
	interfaces, err := net.Interfaces()
	if err != nil {
		return nil, err
	}
	stats, err := readNetDevMap()
	if err != nil {
		return nil, err
	}
	result := make([]netIface, 0, len(interfaces))
	for _, iface := range interfaces {
		sysBase := filepath.Join("/sys/class/net", iface.Name)
		item := netIface{
			Name:         iface.Name,
			DisplayName:  iface.Name,
			Index:        iface.Index,
			MacAddr:      iface.HardwareAddr.String(),
			IfAlias:      readTrimmed(filepath.Join(sysBase, "ifalias")),
			IfType:       readIntFile(filepath.Join(sysBase, "type"), 1),
			IfOperStatus: strings.ToUpper(readTrimmed(filepath.Join(sysBase, "operstate"))),
			MTU:          uint64(iface.MTU),
			Speed:        uint64(readIntFile(filepath.Join(sysBase, "speed"), 0)),
		}
		if item.IfOperStatus == "" {
			item.IfOperStatus = "UNKNOWN"
		}
		if item.MacAddr == "" || item.MacAddr == "00:00:00:00:00:00" {
			item.MacAddr = "unknown"
		}
		if displayName := networkDisplayName(sysBase, iface.Name); displayName != "" {
			item.DisplayName = displayName
		}
		item.IPv4, item.IPv6, item.SubnetMasks, item.PrefixLengths = interfaceAddresses(iface)
		if stat, ok := stats[iface.Name]; ok {
			item.BytesRecv = stat[0]
			item.PacketsRecv = stat[1]
			item.InErrors = stat[2]
			item.InDrops = stat[3]
			item.BytesSent = stat[8]
			item.PacketsSent = stat[9]
			item.OutErrors = stat[10]
			item.Collisions = stat[13]
		}
		result = append(result, item)
	}
	sort.SliceStable(result, func(i, j int) bool { return result[i].Index > result[j].Index })
	return result, nil
}

// knownPCINetworkNames 是常见虚拟化网卡 PCI ID -> 描述（对齐 Oshi 的 displayName 展示）。
var knownPCINetworkNames = map[string]string{
	"1af4:1000": "Red Hat, Inc. Virtio network device",
	"1af4:1041": "Red Hat, Inc. Virtio 1.0 network device",
	"15ad:07b0": "VMware VMXNET3 Ethernet Controller",
	"8086:100e": "Intel 82540EM Gigabit Ethernet Controller",
	"8086:10d3": "Intel 82574L Gigabit Network Connection",
	"10ec:8139": "Realtek RTL-8100/8101L/8139 PCI Fast Ethernet Adapter",
}

// networkDisplayName 读取 /sys/class/net/<if>/device/uevent 的 PCI_ID/DRIVER，
// 映射为 Java Oshi NetworkIF.getDisplayName() 类似的描述。
func networkDisplayName(sysBase, fallback string) string {
	content, err := os.ReadFile(filepath.Join(sysBase, "device", "uevent"))
	if err != nil {
		return fallback
	}
	pciID := ""
	for _, line := range strings.Split(string(content), "\n") {
		if value, found := strings.CutPrefix(line, "PCI_ID="); found {
			pciID = strings.ToLower(strings.TrimSpace(value))
			break
		}
	}
	if name, ok := knownPCINetworkNames[pciID]; ok {
		return name
	}
	return fallback
}

func readDriverName(ueventPath string) string {
	content, err := os.ReadFile(ueventPath)
	if err != nil {
		return ""
	}
	for _, line := range strings.Split(string(content), "\n") {
		if value, found := strings.CutPrefix(line, "DRIVER="); found {
			return strings.TrimSpace(value)
		}
	}
	return ""
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

func readNetDevMap() (map[string][16]uint64, error) {
	content, err := os.ReadFile("/proc/net/dev")
	if err != nil {
		return nil, err
	}
	result := map[string][16]uint64{}
	for _, line := range strings.Split(string(content), "\n") {
		key, value, found := strings.Cut(line, ":")
		if !found {
			continue
		}
		name := strings.TrimSpace(key)
		if name == "" || strings.HasPrefix(name, "Inter-") || strings.HasPrefix(name, "face") {
			continue
		}
		fields := strings.Fields(value)
		if len(fields) < 16 {
			continue
		}
		var values [16]uint64
		for index := 0; index < 16; index++ {
			parsed, err := strconv.ParseUint(fields[index], 10, 64)
			if err != nil {
				continue
			}
			values[index] = parsed
		}
		result[name] = values
	}
	return result, nil
}

func killPID(pid int) error {
	return syscall.Kill(pid, syscall.SIGTERM)
}

// listeningPorts 返回进程监听的端口（Java 端通过 netstat + PID_PORT 缓存实现），
// 只统计绑定在通配地址（0.0.0.0 / :: / *）上的端口，多个用英文逗号连接，无端口返回 "-"。
func listeningPorts(pid int) string {
	ports := pidListeningPorts(pid)
	if len(ports) == 0 {
		return "-"
	}
	values := make([]string, 0, len(ports))
	for _, port := range ports {
		values = append(values, strconv.Itoa(port))
	}
	return strings.Join(values, ",")
}

func pidListeningPorts(pid int) []int {
	inodes, err := socketInodeMap()
	if err != nil || len(inodes) == 0 {
		return nil
	}
	seen := map[int]struct{}{}
	ports := make([]int, 0, 4)
	fdDir := filepath.Join("/proc", strconv.Itoa(pid), "fd")
	entries, err := os.ReadDir(fdDir)
	if err != nil {
		return nil
	}
	for _, entry := range entries {
		target, err := os.Readlink(filepath.Join(fdDir, entry.Name()))
		if err != nil {
			continue
		}
		inode, found := strings.CutPrefix(target, "socket:[")
		if !found {
			continue
		}
		inode = strings.TrimSuffix(inode, "]")
		for _, port := range inodes[inode] {
			if _, ok := seen[port]; ok {
				continue
			}
			seen[port] = struct{}{}
			ports = append(ports, port)
		}
	}
	sort.Ints(ports)
	return ports
}

// socketInodeMap 解析 /proc/net/tcp{,6} 的 LISTEN 套接字：inode -> 端口列表。
func socketInodeMap() (map[string][]int, error) {
	result := map[string][]int{}
	for _, path := range []string{"/proc/net/tcp", "/proc/net/tcp6"} {
		content, err := os.ReadFile(path)
		if err != nil {
			continue
		}
		for _, line := range strings.Split(string(content), "\n") {
			fields := strings.Fields(line)
			if len(fields) < 10 || fields[0] == "sl" {
				continue
			}
			// 状态 0A 表示 LISTEN
			if fields[3] != "0A" {
				continue
			}
			ipPort := strings.Split(fields[1], ":")
			if len(ipPort) != 2 {
				continue
			}
			localAddr := ipPort[0]
			if !strings.EqualFold(localAddr, "00000000") && !strings.EqualFold(localAddr, "00000000000000000000000000000000") {
				continue
			}
			port, err := strconv.ParseInt(ipPort[1], 16, 32)
			if err != nil {
				continue
			}
			inode := fields[9]
			result[inode] = append(result[inode], int(port))
		}
	}
	return result, nil
}
