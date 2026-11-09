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
	"path/filepath"
	"runtime"
	"testing"
)

// TestSimpleStatusStructure 校验 /get-stat-info 的 simpleStatus 字段名与类型
// （long 必须是字符串，double 必须是数值）。
func TestSimpleStatusStructure(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Skip("需要 /proc 与 /sys")
	}
	status, err := SimpleStatus(nil)
	if err != nil {
		t.Fatalf("SimpleStatus: %v", err)
	}
	if _, ok := status["time"].(string); !ok {
		t.Errorf("time should be string, got %T", status["time"])
	}
	cpuInfo, ok := status["cpuInfo"].(map[string]any)
	if !ok {
		t.Fatalf("cpuInfo = %T", status["cpuInfo"])
	}
	for _, key := range []string{"cpuNum", "toTal", "sys", "user", "wait", "free", "cpuModel", "ticks", "used"} {
		if _, ok := cpuInfo[key]; !ok {
			t.Errorf("cpuInfo missing %q", key)
		}
	}
	if _, ok := cpuInfo["cpuNum"].(int); !ok {
		t.Errorf("cpuNum should be int, got %T", cpuInfo["cpuNum"])
	}
	if _, ok := cpuInfo["toTal"].(float64); !ok {
		t.Errorf("toTal should be float64, got %T", cpuInfo["toTal"])
	}
	ticks, ok := cpuInfo["ticks"].(map[string]any)
	if !ok {
		t.Fatalf("ticks = %T", cpuInfo["ticks"])
	}
	for _, key := range []string{"idle", "nice", "irq", "softIrq", "steal", "cSys", "user", "ioWait"} {
		if _, ok := ticks[key].(string); !ok {
			t.Errorf("ticks.%s should be string, got %T", key, ticks[key])
		}
	}
	for _, key := range []string{"memory", "disk"} {
		if _, ok := status[key].(float64); !ok {
			t.Errorf("%s should be float64, got %T", key, status[key])
		}
	}
	for _, key := range []string{"netTxBytes", "netRxBytes"} {
		if _, ok := status[key].(string); !ok {
			t.Errorf("%s should be string, got %T", key, status[key])
		}
	}
	if _, ok := status["monitorIfsNames"].(string); !ok {
		t.Errorf("monitorIfsNames should be string, got %T", status["monitorIfsNames"])
	}
	if SystemSleep != 2000 {
		t.Errorf("SystemSleep = %d, want 2000", SystemSleep)
	}
}

func TestSystemInfoStructure(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Skip("需要 /proc")
	}
	info, err := SystemInfo()
	if err != nil {
		t.Fatalf("SystemInfo: %v", err)
	}
	for _, key := range []string{"systemUptime", "osMoneyTotal", "osSwapTotal", "osVirtualMax", "osFileStoreTotal"} {
		if _, ok := info[key].(string); !ok {
			t.Errorf("%s should be string, got %T", key, info[key])
		}
	}
	if _, ok := info["osCpuCores"].(int); !ok {
		t.Errorf("osCpuCores should be int, got %T", info["osCpuCores"])
	}
	if _, ok := info["osLoadAverage"].([]float64); !ok {
		t.Errorf("osLoadAverage should be []float64, got %T", info["osLoadAverage"])
	}
}

func TestDiskAndNetworkStructure(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Skip("需要 /proc 与 /sys")
	}
	stores, err := FileStoresJSON()
	if err != nil {
		t.Fatalf("FileStoresJSON: %v", err)
	}
	for _, store := range stores {
		for _, key := range []string{"name", "volume", "logicalVolume", "mount", "label", "description", "type", "options", "uuid"} {
			if _, ok := store[key].(string); !ok {
				t.Errorf("fileStore.%s should be string, got %T", key, store[key])
			}
		}
		for _, key := range []string{"freeInodes", "totalInodes", "freeSpace", "totalSpace", "usableSpace"} {
			if _, ok := store[key].(string); !ok {
				t.Errorf("fileStore.%s should be string, got %T", key, store[key])
			}
		}
	}
	ifaces, err := NetworkInterfacesJSON()
	if err != nil {
		t.Fatalf("NetworkInterfacesJSON: %v", err)
	}
	for _, item := range ifaces {
		if _, ok := item["index"].(int); !ok {
			t.Errorf("network.index should be int, got %T", item["index"])
		}
		if _, ok := item["ifType"].(int); !ok {
			t.Errorf("network.ifType should be int, got %T", item["ifType"])
		}
		for _, key := range []string{"mtu", "speed", "bytesRecv", "bytesSent", "tnDrops", "inErrors", "outErrors", "collisions", "timeStamp"} {
			if _, ok := item[key].(string); !ok {
				t.Errorf("network.%s should be string, got %T", key, item[key])
			}
		}
		if _, ok := item["subnetMasks"].([]int); !ok {
			t.Errorf("network.subnetMasks should be []int, got %T", item["subnetMasks"])
		}
	}
	disks, err := DiskStoresJSON()
	if err != nil {
		t.Fatalf("DiskStoresJSON: %v", err)
	}
	for _, disk := range disks {
		for _, key := range []string{"size", "readBytes", "reads", "writeBytes", "writes", "currentQueueLength", "transferTime", "timeStamp"} {
			if _, ok := disk[key].(string); !ok {
				t.Errorf("disk.%s should be string, got %T", key, disk[key])
			}
		}
		if _, ok := disk["partition"].([]map[string]any); !ok {
			t.Errorf("disk.partition should be an array, got %T", disk["partition"])
		}
	}
}

func TestProcessListJSON(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Skip("需要 /proc")
	}
	list, err := ProcessListJSON("java", 2)
	if err != nil {
		t.Fatalf("ProcessListJSON: %v", err)
	}
	for _, item := range list {
		for _, key := range []string{"hardOpenFileLimit", "softOpenFileLimit", "openFiles", "bytesWritten", "bytesRead", "startTime", "upTime", "userTime", "virtualSize", "residentSetSize"} {
			if _, ok := item[key].(string); !ok {
				t.Errorf("process.%s should be string, got %T", key, item[key])
			}
		}
		if _, ok := item["processId"].(int); !ok {
			t.Errorf("process.processId should be int, got %T", item["processId"])
		}
		if _, ok := item["port"].(string); !ok {
			t.Errorf("process.port should be string, got %T", item["port"])
		}
	}
}

func TestCleanTempFiles(t *testing.T) {
	a := newTestApp(t)
	oldDir := filepath.Join(a.TempRoot(), "2020-01-01")
	newDir := filepath.Join(a.TempRoot(), "2099-01-01")
	for _, dir := range []string{oldDir, newDir} {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			t.Fatal(err)
		}
	}
	CleanTempFiles(a)
	if _, err := os.Stat(oldDir); !os.IsNotExist(err) {
		t.Error("old temp directory should be removed")
	}
	if _, err := os.Stat(newDir); err != nil {
		t.Error("recent temp directory should be kept")
	}
}
