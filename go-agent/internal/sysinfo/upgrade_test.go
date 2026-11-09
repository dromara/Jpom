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
	"archive/zip"
	"bytes"
	"crypto/sha256"
	"encoding/binary"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/i18n"
)

// upgradeTestApp 构造带“当前二进制”的升级测试 App，并禁用真实重启。
func upgradeTestApp(t *testing.T) *app.App {
	t.Helper()
	a := newTestApp(t)
	if err := os.MkdirAll(filepath.Dir(a.Executable), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(a.Executable, []byte("old-binary"), 0o755); err != nil {
		t.Fatal(err)
	}
	previous := restartAfterUpgrade
	restartAfterUpgrade = func(*app.App, time.Duration) {}
	t.Cleanup(func() { restartAfterUpgrade = previous })
	return a
}

// buildZipEntries 把条目写成 zip（key 为 zip 内路径）。
func buildZipEntries(t *testing.T, entries map[string][]byte) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), "upgrade.zip")
	file, err := os.Create(path)
	if err != nil {
		t.Fatal(err)
	}
	writer := zip.NewWriter(file)
	for name, content := range entries {
		entry, err := writer.Create(name)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := entry.Write(content); err != nil {
			t.Fatal(err)
		}
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	if err := file.Close(); err != nil {
		t.Fatal(err)
	}
	return path
}

func sha256Hex(content []byte) string {
	sum := sha256.Sum256(content)
	return hex.EncodeToString(sum[:])
}

func goUpgradeContainer(t *testing.T, version, platform, binaryEntry string, binary []byte) string {
	t.Helper()
	manifest, err := json.Marshal(map[string]string{
		"version":    version,
		"timeStamp":  "2026-11-09T00:00:00Z",
		"minVersion": "1.0.0",
		"platform":   platform,
		"sha256":     sha256Hex(binary),
	})
	if err != nil {
		t.Fatal(err)
	}
	entries := map[string][]byte{"go-agent/agent.json": manifest}
	if binaryEntry != "" {
		entries[binaryEntry] = binary
	}
	return buildZipEntries(t, entries)
}

// fakeELF 构造最小 ELF64 头（仅包含平台校验需要的字段）。
func fakeELF(machine uint16) []byte {
	header := make([]byte, 64)
	header[0] = 0x7f
	header[1] = 'E'
	header[2] = 'L'
	header[3] = 'F'
	header[4] = 2 // ELFCLASS64
	header[5] = 1 // little endian
	binary.LittleEndian.PutUint16(header[18:20], machine)
	return header
}

// fakePE 构造最小 PE 头（MZ + e_lfanew + PE 签名 + Machine）。
func fakePE(machine uint16) []byte {
	header := make([]byte, 128)
	header[0] = 'M'
	header[1] = 'Z'
	binary.LittleEndian.PutUint32(header[0x3c:0x40], 64)
	copy(header[64:68], []byte{'P', 'E', 0, 0})
	binary.LittleEndian.PutUint16(header[68:70], machine)
	return header
}

// fakeMachO 构造最小 64 位小端 Mach-O 头（magic 字节 CF FA ED FE）。
func fakeMachO(cpuType uint32) []byte {
	header := make([]byte, 32)
	binary.BigEndian.PutUint32(header[0:4], machCigam64)
	binary.LittleEndian.PutUint32(header[4:8], cpuType)
	return header
}

func requireParamError(t *testing.T, err error, key string) *ParamError {
	t.Helper()
	if err == nil {
		t.Fatalf("期望返回错误（key=%s），实际成功", key)
	}
	paramError := asParamError(t, err)
	if paramError.Code != 405 {
		t.Errorf("code = %d, want 405", paramError.Code)
	}
	if key != "" && paramError.Key != key {
		t.Errorf("key = %q, want %q (msg=%s)", paramError.Key, key, paramError.Msg)
	}
	return paramError
}

// assertUpgradeNotApplied 断言校验失败时当前二进制未被替换、未写 pending。
func assertUpgradeNotApplied(t *testing.T, a *app.App) {
	t.Helper()
	content, err := os.ReadFile(a.Executable)
	if err != nil {
		t.Fatalf("读取当前二进制失败: %v", err)
	}
	if string(content) != "old-binary" {
		t.Errorf("校验失败不应改动当前二进制, content=%q", string(content))
	}
	if _, err := os.Stat(upgradePendingPath(a)); !os.IsNotExist(err) {
		t.Errorf("校验失败不应写 upgrade-pending.json, err=%v", err)
	}
}

// TestInstallGoUpgradeCorrectPlatform 正确平台容器通过校验、替换并可重启。
func TestInstallGoUpgradeCorrectPlatform(t *testing.T) {
	a := upgradeTestApp(t)
	binary := fakeELF(elfMachineAMD64)
	container := goUpgradeContainer(t, "2.11.14", "linux/amd64", "go-agent/bin/go-agent-linux-amd64", binary)
	message, err := installAgentPackageFor(a, container, targetPlatform{GOOS: "linux", GOARCH: "amd64"})
	if err != nil {
		t.Fatalf("正确平台容器应通过: %v", err)
	}
	if message != UpgradeMessage {
		t.Errorf("message = %q", message)
	}
	content, err := os.ReadFile(a.Executable)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(content, binary) {
		t.Error("当前二进制未被替换为升级包内容")
	}
	if _, err := os.Stat(upgradePendingPath(a)); err != nil {
		t.Errorf("替换后应写 upgrade-pending.json: %v", err)
	}
	state := readUpgradeState(t, a)
	if state["status"] != "installed" {
		t.Errorf("upgrade.json status = %v", state["status"])
	}
}

// TestInstallGoUpgradeRejectsArm64OnAmd64 arm64 容器在 amd64 节点被拒且不改写。
func TestInstallGoUpgradeRejectsArm64OnAmd64(t *testing.T) {
	a := upgradeTestApp(t)
	container := goUpgradeContainer(t, "2.11.14", "linux/arm64", "go-agent/bin/go-agent-linux-arm64", fakeELF(elfMachineARM64))
	_, err := installAgentPackageFor(a, container, targetPlatform{GOOS: "linux", GOARCH: "amd64"})
	requireParamError(t, err, "i18n.go_upgrade_platform_mismatch")
	assertUpgradeNotApplied(t, a)
}

// TestInstallGoUpgradeRejectsAmd64OnArm64 amd64 容器在 arm64 节点被拒且不改写。
func TestInstallGoUpgradeRejectsAmd64OnArm64(t *testing.T) {
	a := upgradeTestApp(t)
	container := goUpgradeContainer(t, "2.11.14", "linux/amd64", "go-agent/bin/go-agent-linux-amd64", fakeELF(elfMachineAMD64))
	_, err := installAgentPackageFor(a, container, targetPlatform{GOOS: "linux", GOARCH: "arm64"})
	requireParamError(t, err, "i18n.go_upgrade_platform_mismatch")
	assertUpgradeNotApplied(t, a)
}

// TestInstallGoUpgradeRejectsMissingPlatformBinary 无精确平台文件时禁止 fallback。
func TestInstallGoUpgradeRejectsMissingPlatformBinary(t *testing.T) {
	cases := []struct {
		name        string
		binaryEntry string
	}{
		{name: "only arm64 file", binaryEntry: "go-agent/bin/go-agent-linux-arm64"},
		{name: "generic agent file", binaryEntry: "go-agent/bin/agent"},
		{name: "wrong directory", binaryEntry: "bin/go-agent-linux-amd64"},
	}
	for _, testCase := range cases {
		t.Run(testCase.name, func(t *testing.T) {
			a := upgradeTestApp(t)
			container := goUpgradeContainer(t, "2.11.14", "linux/amd64", testCase.binaryEntry, fakeELF(elfMachineAMD64))
			_, err := installAgentPackageFor(a, container, targetPlatform{GOOS: "linux", GOARCH: "amd64"})
			requireParamError(t, err, "i18n.go_upgrade_missing_binary")
			assertUpgradeNotApplied(t, a)
		})
	}
}

// TestInstallGoUpgradeRejectsWrongMachine 文件名/平台正确但 machine 不符时拒绝。
func TestInstallGoUpgradeRejectsWrongMachine(t *testing.T) {
	a := upgradeTestApp(t)
	container := goUpgradeContainer(t, "2.11.14", "linux/amd64", "go-agent/bin/go-agent-linux-amd64", fakeELF(elfMachineARM64))
	_, err := installAgentPackageFor(a, container, targetPlatform{GOOS: "linux", GOARCH: "amd64"})
	requireParamError(t, err, "i18n.go_upgrade_platform_mismatch")
	assertUpgradeNotApplied(t, a)
}

// TestInstallGoUpgradeLegacyPackageWithoutPlatform 旧容器（无 platform）仅 linux/amd64 可用。
func TestInstallGoUpgradeLegacyPackageWithoutPlatform(t *testing.T) {
	a := upgradeTestApp(t)
	container := goUpgradeContainer(t, "2.11.14", "", "go-agent/bin/go-agent-linux-amd64", fakeELF(elfMachineAMD64))
	if _, err := installAgentPackageFor(a, container, targetPlatform{GOOS: "linux", GOARCH: "amd64"}); err != nil {
		t.Fatalf("linux/amd64 应兼容旧格式容器: %v", err)
	}
	other := upgradeTestApp(t)
	container = goUpgradeContainer(t, "2.11.14", "", "go-agent/bin/go-agent-linux-arm64", fakeELF(elfMachineARM64))
	_, err := installAgentPackageFor(other, container, targetPlatform{GOOS: "linux", GOARCH: "arm64"})
	requireParamError(t, err, "i18n.go_upgrade_platform_mismatch")
	assertUpgradeNotApplied(t, other)
}

// TestVerifyExecutableMachines 覆盖 ELF/PE/Mach-O 的架构 machine/cputype 校验。
func TestVerifyExecutableMachines(t *testing.T) {
	dir := t.TempDir()
	write := func(name string, content []byte) string {
		t.Helper()
		path := filepath.Join(dir, name)
		if err := os.WriteFile(path, content, 0o755); err != nil {
			t.Fatal(err)
		}
		return path
	}
	elfAMD64 := write("elf-amd64", fakeELF(elfMachineAMD64))
	elfARM64 := write("elf-arm64", fakeELF(elfMachineARM64))
	if err := verifyExecutableFor(elfAMD64, targetPlatform{GOOS: "linux", GOARCH: "amd64"}); err != nil {
		t.Errorf("amd64 ELF 应通过: %v", err)
	}
	if err := verifyExecutableFor(elfARM64, targetPlatform{GOOS: "linux", GOARCH: "amd64"}); err == nil {
		t.Error("arm64 ELF 不应在 amd64 通过")
	}
	if err := verifyExecutableFor(elfARM64, targetPlatform{GOOS: "linux", GOARCH: "arm64"}); err != nil {
		t.Errorf("arm64 ELF 应通过: %v", err)
	}
	peAMD64 := write("pe-amd64", fakePE(peMachineAMD64))
	peARM64 := write("pe-arm64", fakePE(peMachineARM64))
	if err := verifyExecutableFor(peAMD64, targetPlatform{GOOS: "windows", GOARCH: "amd64"}); err != nil {
		t.Errorf("amd64 PE 应通过: %v", err)
	}
	if err := verifyExecutableFor(peARM64, targetPlatform{GOOS: "windows", GOARCH: "amd64"}); err == nil {
		t.Error("arm64 PE 不应在 amd64 通过")
	}
	machARM64 := write("mach-arm64", fakeMachO(machCPUTypeARM64))
	machAMD64 := write("mach-amd64", fakeMachO(machCPUTypeAMD64))
	if err := verifyExecutableFor(machARM64, targetPlatform{GOOS: "darwin", GOARCH: "arm64"}); err != nil {
		t.Errorf("arm64 Mach-O 应通过: %v", err)
	}
	if err := verifyExecutableFor(machAMD64, targetPlatform{GOOS: "darwin", GOARCH: "arm64"}); err == nil {
		t.Error("amd64 Mach-O 不应在 arm64 通过")
	}
	invalid := write("invalid", []byte("not an executable"))
	requireParamError(t, verifyExecutableFor(invalid, targetPlatform{GOOS: "linux", GOARCH: "amd64"}), "i18n.go_upgrade_invalid_linux_executable")
	requireParamError(t, verifyExecutableFor(invalid, targetPlatform{GOOS: "windows", GOARCH: "amd64"}), "i18n.go_upgrade_invalid_windows_executable")
	requireParamError(t, verifyExecutableFor(invalid, targetPlatform{GOOS: "darwin", GOARCH: "arm64"}), "i18n.go_upgrade_invalid_macos_executable")
}

// TestVerifyExecutableCurrentProcess 用真实运行中的二进制验证 ELF 解析。
func TestVerifyExecutableCurrentProcess(t *testing.T) {
	executable, err := os.Executable()
	if err != nil {
		t.Fatal(err)
	}
	if err := verifyExecutable(executable); err != nil {
		t.Fatalf("当前进程二进制应通过平台校验: %v", err)
	}
}

// TestGoUpgradeContainerURLs 验证按 agentUrl 推导同版本平台容器 URL。
func TestGoUpgradeContainerURLs(t *testing.T) {
	linuxAmd64 := targetPlatform{GOOS: "linux", GOARCH: "amd64"}
	urls, err := goUpgradeContainerURLs("https://d2.jpom.download/release/2.11.13/agent-2.11.13-release.zip", "v2.11.13", linuxAmd64)
	if err != nil {
		t.Fatal(err)
	}
	want := []string{
		"https://d2.jpom.download/release/2.11.13/agent-go-2.11.13-upgrade-linux-amd64.jar",
		"https://d2.jpom.download/release/2.11.13/agent-go-2.11.13-upgrade.jar",
	}
	if len(urls) != len(want) {
		t.Fatalf("urls = %v, want %v", urls, want)
	}
	for index := range want {
		if urls[index] != want[index] {
			t.Errorf("urls[%d] = %q, want %q", index, urls[index], want[index])
		}
	}
	// 非 linux/amd64 不生成兼容别名。
	urls, err = goUpgradeContainerURLs("https://d2.jpom.download/beta/2.11.13.2/agent-2.11.13.2-release.zip?token=x", "v2.11.13.2", targetPlatform{GOOS: "linux", GOARCH: "arm64"})
	if err != nil {
		t.Fatal(err)
	}
	if len(urls) != 1 || urls[0] != "https://d2.jpom.download/beta/2.11.13.2/agent-go-2.11.13.2-upgrade-linux-arm64.jar" {
		t.Errorf("urls = %v", urls)
	}
	// agentUrl 文件名不含版本时回退 tagName。
	urls, err = goUpgradeContainerURLs("https://example.com/dir/agent.zip", "v2.11.13", targetPlatform{GOOS: "darwin", GOARCH: "arm64"})
	if err != nil {
		t.Fatal(err)
	}
	if len(urls) != 1 || urls[0] != "https://example.com/dir/agent-go-2.11.13-upgrade-darwin-arm64.jar" {
		t.Errorf("urls = %v", urls)
	}
	if _, err := goUpgradeContainerURLs("", "", linuxAmd64); err == nil {
		t.Error("空 agentUrl 应返回错误")
	}
}

// TestCheckPackagePlatformCaseInsensitive 平台比较忽略大小写/分隔符差异。
func TestCheckPackagePlatformCaseInsensitive(t *testing.T) {
	if err := checkPackagePlatform("Linux/AMD64", targetPlatform{GOOS: "linux", GOARCH: "amd64"}); err != nil {
		t.Errorf("大小写归一化后应通过: %v", err)
	}
	requireParamError(t, checkPackagePlatform("linux/amd64", targetPlatform{GOOS: "linux", GOARCH: "arm64"}), "i18n.go_upgrade_platform_mismatch")
}

// TestServiceManagerProcessNames（app 包）在 app 包单测中覆盖，这里仅确保 i18n key 存在。
func TestUpgradePlatformI18nKeys(t *testing.T) {
	for _, key := range []string{"i18n.go_upgrade_platform_mismatch", "i18n.go_upgrade_missing_binary"} {
		if !i18n.Has(key) {
			t.Errorf("缺少 i18n key: %s", key)
		}
	}
}

// TestReplaceExecutableAndCleanup 覆盖 Windows 改名替换策略的通用部分与 .old 清理。
func TestReplaceExecutableAndCleanup(t *testing.T) {
	a := upgradeTestApp(t)
	// 真实运行时 RunDir 是可执行文件所在目录（<base>/bin）。
	a.RunDir = filepath.Dir(a.Executable)
	staged := a.Executable + ".upgrade"
	if err := os.WriteFile(staged, []byte("new-binary"), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := replaceExecutable(staged, a.Executable); err != nil {
		t.Fatalf("replaceExecutable: %v", err)
	}
	content, err := os.ReadFile(a.Executable)
	if err != nil {
		t.Fatal(err)
	}
	if string(content) != "new-binary" {
		t.Errorf("替换后内容 = %q", string(content))
	}
	// Windows 升级会留下 <exe>.old，启动时应自动清理。
	leftover := a.Executable + ".old"
	if err := os.WriteFile(leftover, []byte("old"), 0o755); err != nil {
		t.Fatal(err)
	}
	unrelated := filepath.Join(a.RunDir, "other.old")
	if err := os.WriteFile(unrelated, []byte("keep"), 0o644); err != nil {
		t.Fatal(err)
	}
	cleanupReplacedExecutables(a)
	if _, err := os.Stat(leftover); !os.IsNotExist(err) {
		t.Errorf("<exe>.old 应被清理: %v", err)
	}
	if _, err := os.Stat(unrelated); err != nil {
		t.Errorf("无关 .old 文件不应被删除: %v", err)
	}
}
