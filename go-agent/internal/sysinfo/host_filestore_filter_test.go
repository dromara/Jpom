//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package sysinfo

import (
	"testing"
)

// TestShouldCountFileStore 校验挂载点过滤规则：
// macOS 的 APFS 同容器伪卷、伪文件系统不计入，根卷与外接卷计入。
func TestShouldCountFileStore(t *testing.T) {
	cases := []struct {
		name  string
		goos  string
		fstyp string
		mount string
		want  bool
	}{
		// macOS：同一 APFS 容器会挂多个卷，statfs 都返回容器总容量，只保留根卷
		{"darwin 根卷", "darwin", "apfs", "/", true},
		{"darwin 数据卷（同容器）", "darwin", "apfs", "/System/Volumes/Data", false},
		{"darwin Preboot（同容器）", "darwin", "apfs", "/System/Volumes/Preboot", false},
		{"darwin Update（同容器）", "darwin", "apfs", "/System/Volumes/Update", false},
		{"darwin VM（同容器）", "darwin", "apfs", "/System/Volumes/VM", false},
		{"darwin 容器根路径", "darwin", "apfs", "/System/Volumes", false},
		{"darwin 外接卷", "darwin", "apfs", "/Volumes/Backup", true},
		{"darwin 外接卷尾部斜杠", "darwin", "apfs", "/Volumes/Backup/", true},
		{"darwin devfs", "darwin", "devfs", "/dev", false},
		{"darwin automount", "darwin", "autofs", "/net", false},
		// Windows：固定盘全部保留
		{"windows C 盘", "windows", "NTFS", "C:\\", true},
		{"windows D 盘", "windows", "NTFS", "D:\\", true},
		// Linux 路径不走本函数，规则本身仍应为 true（防止误删其它平台）
		{"linux 数据盘", "linux", "ext4", "/data", true},
		{"linux tmpfs", "linux", "tmpfs", "/run", true},
		// 空值防御
		{"空文件系统类型", "darwin", "", "/", false},
		{"空挂载点", "darwin", "apfs", "", false},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := shouldCountFileStore(c.goos, c.fstyp, c.mount); got != c.want {
				t.Fatalf("shouldCountFileStore(%q,%q,%q) = %v, want %v", c.goos, c.fstyp, c.mount, got, c.want)
			}
		})
	}
}

// TestFilterFileStoresMacOSAPFSContainer 复现群反馈现场：Mac mini M4 512GB（实际 494.38 GB），
// APFS 同容器 6 个挂载点被逐个累加后显示成 2.70TB。
// 过滤后必须只剩根卷，总量等于单个容器容量。
func TestFilterFileStoresMacOSAPFSContainer(t *testing.T) {
	const volume = uint64(494384795648) // 494.38 GB（十进制）
	stores := []fileStore{
		{Name: "/", Volume: "/dev/disk3s1s1", Mount: "/", Type: "apfs", TotalSpace: volume, FreeSpace: 49271386112, UsableSpace: 49271386112},
		{Name: "/dev/disk3s5", Volume: "/dev/disk3s5", Mount: "/System/Volumes/Data", Type: "apfs", TotalSpace: volume, FreeSpace: 49271386112, UsableSpace: 49271386112},
		{Name: "/dev/disk3s2", Volume: "/dev/disk3s2", Mount: "/System/Volumes/Preboot", Type: "apfs", TotalSpace: volume, FreeSpace: volume, UsableSpace: volume},
		{Name: "/dev/disk3s4", Volume: "/dev/disk3s4", Mount: "/System/Volumes/Update", Type: "apfs", TotalSpace: volume, FreeSpace: volume, UsableSpace: volume},
		{Name: "/dev/disk3s6", Volume: "/dev/disk3s6", Mount: "/System/Volumes/VM", Type: "apfs", TotalSpace: volume, FreeSpace: volume, UsableSpace: volume},
		{Name: "/dev/disk3s7", Volume: "/dev/disk3s7", Mount: "/System/Volumes/xarts", Type: "apfs", TotalSpace: volume, FreeSpace: volume, UsableSpace: volume},
	}
	var naiveTotal uint64
	for _, store := range stores {
		naiveTotal += store.TotalSpace
	}
	if naiveTotal != volume*6 {
		t.Fatalf("用例自身不成立：累加总量 %d，期望 %d", naiveTotal, volume*6)
	}

	filtered := filterFileStores(stores, "darwin")
	if len(filtered) != 1 {
		t.Fatalf("过滤后应只剩 1 个挂载点，实际 %d 个：%+v", len(filtered), filtered)
	}
	if filtered[0].Mount != "/" {
		t.Fatalf("过滤后应保留根卷，实际 %q", filtered[0].Mount)
	}
	var total uint64
	for _, store := range filtered {
		total += store.TotalSpace
	}
	if total != volume {
		t.Fatalf("过滤后总量 %d，期望单容器容量 %d（过滤前为 %d，即 2.70TB 的来源）", total, volume, naiveTotal)
	}
}

// TestFilterFileStoresMacOSKeepsExternalVolume 校验外接卷仍然计入（避免把正常盘一起过滤掉）。
func TestFilterFileStoresMacOSKeepsExternalVolume(t *testing.T) {
	stores := []fileStore{
		{Name: "/", Mount: "/", Type: "apfs", TotalSpace: 494384795648},
		{Name: "/dev/disk3s5", Mount: "/System/Volumes/Data", Type: "apfs", TotalSpace: 494384795648},
		{Name: "/dev/disk4s1", Mount: "/Volumes/Backup", Type: "apfs", TotalSpace: 2000398934016},
		{Name: "devfs", Mount: "/dev", Type: "devfs", TotalSpace: 0},
	}
	filtered := filterFileStores(stores, "darwin")
	if len(filtered) != 2 {
		t.Fatalf("应保留根卷与外接卷共 2 个，实际 %d：%+v", len(filtered), filtered)
	}
	var total uint64
	for _, store := range filtered {
		total += store.TotalSpace
	}
	want := uint64(494384795648) + 2000398934016
	if total != want {
		t.Fatalf("总量 %d，期望 %d", total, want)
	}
}

// TestFilterFileStoresWindowsKeepsFixedDisks Windows 多盘不回归。
func TestFilterFileStoresWindowsKeepsFixedDisks(t *testing.T) {
	stores := []fileStore{
		{Name: "C:", Mount: "C:\\", Type: "NTFS", TotalSpace: 256060514304},
		{Name: "D:", Mount: "D:\\", Type: "NTFS", TotalSpace: 1000215212032},
	}
	filtered := filterFileStores(stores, "windows")
	if len(filtered) != 2 {
		t.Fatalf("Windows 固定盘应全部保留，实际 %d：%+v", len(filtered), filtered)
	}
}

// TestFilterFileStoresDedupSameMount 同一挂载点被重复返回时只算一次。
func TestFilterFileStoresDedupSameMount(t *testing.T) {
	stores := []fileStore{
		{Name: "/dev/sda1", Mount: "/data", Type: "ext4", TotalSpace: 100},
		{Name: "/dev/sda1", Mount: "/data/", Type: "ext4", TotalSpace: 100},
	}
	filtered := filterFileStores(stores, "linux")
	if len(filtered) != 1 {
		t.Fatalf("重复挂载点应去重，实际 %d：%+v", len(filtered), filtered)
	}
}
