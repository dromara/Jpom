//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package app

import (
	"path/filepath"
	"strconv"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/config"
)

// TestManifestBuildCapability 覆盖 /info manifest 的 capabilities 声明（只增字段，
// 服务端据此判断节点能否承担构建；旧字段保持原样）。
func TestManifestBuildCapability(t *testing.T) {
	base := t.TempDir()
	instance := &App{
		Version:    "dev",
		BaseDir:    base,
		Config:     config.Default(),
		DataDir:    filepath.Join(base, "data"),
		Pid:        4242,
		StartAt:    time.Now(),
		Executable: filepath.Join(base, "bin", "agent"),
	}
	manifest := instance.Manifest()
	capabilities, ok := manifest["capabilities"].([]string)
	if !ok {
		t.Fatalf("capabilities 类型异常: %T", manifest["capabilities"])
	}
	found := false
	for _, item := range capabilities {
		if item == "build" {
			found = true
		}
	}
	if !found {
		t.Fatalf("capabilities 缺少 build: %v", capabilities)
	}
	// 旧字段保持不变。
	if manifest["engine"] != "go" || manifest["type"] != "Agent" {
		t.Fatalf("旧字段被改动: %+v", manifest)
	}
}

// TestManifestUpTimeMilliseconds 锁定 upTime 单位：Java 插件端是 RuntimeMXBean#getUptime()
// 的毫秒值，在线升级页「运行时间」列按毫秒格式化；Go 端曾上报秒导致显示被缩小 1000 倍。
func TestManifestUpTimeMilliseconds(t *testing.T) {
	base := t.TempDir()
	instance := &App{
		Version:    "dev",
		BaseDir:    base,
		Config:     config.Default(),
		DataDir:    filepath.Join(base, "data"),
		Pid:        4242,
		StartAt:    time.Now().Add(-2 * time.Second),
		Executable: filepath.Join(base, "bin", "agent"),
	}
	raw, ok := instance.Manifest()["upTime"].(string)
	if !ok {
		t.Fatalf("upTime 必须是字符串（Jackson long→string）")
	}
	value, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		t.Fatalf("upTime 解析失败: %v", err)
	}
	if value < 1500 || value > 60000 {
		t.Fatalf("upTime = %d，应约为 2000 毫秒（秒单位会被判定为 2）", value)
	}
}
