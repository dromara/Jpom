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
	"context"
	"crypto/md5"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"sort"
	"strconv"
	"strings"
	"time"

	"jpom.io/go-agent/internal/app"
)

// 升级包校验/替换说明（与 Java 端 SystemUpdateController 的分片协议一致）：
//  1. upload-jar-sharding 按 sliceId 把分片存到 data/temp/<date>/slice/<sliceId>/items/；
//  2. upload-jar-sharding-merge 按分片扩展名序号合并、校验 md5；
//  3. Go 端升级包为包含 agent 可执行文件的 zip（Java 的 Agent-*.jar 升级包无法直接使用，
//     会返回明确的限制说明）。

// SaveJarSharding 对应 /system/upload-jar-sharding 的分片保存逻辑。
func ValidateShardingMeta(fileSumMD5, sliceID string, totalSlice, nowSlice int) error {
	// 与 Java BaseJpomController.uploadSharding 的 Assert 顺序保持一致：
	// fileSumMd5 -> sliceId -> totalSlice -> nowSlice -> 范围。
	if strings.TrimSpace(fileSumMD5) == "" {
		return paramErr(405, "i18n.file_signature_info_not_found.83bf")
	}
	if strings.TrimSpace(sliceID) == "" {
		return paramErr(405, "i18n.no_shard_id_info.30f8")
	}
	if totalSlice <= 0 {
		return paramErr(405, "i18n.incomplete_upload_info_total_slice.7e85")
	}
	if nowSlice < 0 {
		return paramErr(405, "i18n.incomplete_upload_info_now_slice.34aa")
	}
	if totalSlice < nowSlice {
		return paramErr(405, "i18n.current_upload_chunk_info_incorrect.900e")
	}
	return nil
}

func SaveJarSharding(a *app.App, originalFilename string, content io.Reader, sliceID string, totalSlice, nowSlice int, fileSumMD5 string) error {
	if err := ValidateShardingMeta(fileSumMD5, sliceID, totalSlice, nowSlice); err != nil {
		return err
	}
	if strings.TrimSpace(originalFilename) == "" {
		return paramErr(405, "i18n.no_uploaded_file.07ef")
	}
	// Java 端把最后一段分片序号去掉后校验扩展名（jar/zip）。
	realName := originalFilename
	if index := strings.LastIndex(originalFilename, "."); index > 0 {
		realName = originalFilename[:index]
	}
	extName := strings.TrimPrefix(strings.ToLower(filepath.Ext(realName)), ".")
	if extName != "jar" && extName != "zip" {
		return paramErrAppend(405, "i18n.file_type_not_supported2.d497", extName)
	}
	// Java 的分片目录使用 AgentConfig.getFixedTempPathName()（data/temp，不含日期目录）。
	sliceDir := filepath.Join(a.TempRoot(), "slice", sliceID, "items")
	if err := os.MkdirAll(sliceDir, 0o755); err != nil {
		return fmt.Errorf("create sharding directory failed: %w", err)
	}
	target := filepath.Join(sliceDir, filepath.Base(originalFilename))
	output, err := os.Create(target)
	if err != nil {
		return fmt.Errorf("save sharding piece failed: %w", err)
	}
	defer func() { _ = output.Close() }()
	if _, err := io.Copy(output, content); err != nil {
		return fmt.Errorf("save sharding piece failed: %w", err)
	}
	return nil
}

// MergeJarSharding 对应 /system/upload-jar-sharding-merge。
func MergeJarSharding(a *app.App, sliceID string, totalSlice int, fileSumMD5 string) (string, error) {
	if strings.TrimSpace(fileSumMD5) == "" {
		return "", paramErr(405, "i18n.file_signature_info_not_found.83bf")
	}
	if strings.TrimSpace(sliceID) == "" {
		return "", paramErr(405, "i18n.no_shard_id_info.30f8")
	}
	if totalSlice <= 0 {
		return "", paramErr(405, "i18n.incomplete_upload_info_total_slice.7e85")
	}
	slicePath := filepath.Join(a.TempRoot(), "slice", sliceID)
	itemsDir := filepath.Join(slicePath, "items")
	entries, err := os.ReadDir(itemsDir)
	if err != nil {
		return "", paramErr(405, "i18n.file_upload_failure_due_to_missing_chunks.1865", 0, totalSlice)
	}
	names := make([]string, 0, len(entries))
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		names = append(names, entry.Name())
	}
	if len(names) != totalSlice {
		return "", paramErr(405, "i18n.file_upload_failure_due_to_missing_chunks.1865", len(names), totalSlice)
	}
	sort.SliceStable(names, func(i, j int) bool {
		return sliceIndex(names[i]) < sliceIndex(names[j])
	})
	baseName := names[0]
	if index := strings.LastIndex(baseName, "."); index > 0 {
		baseName = baseName[:index]
	}
	merged := filepath.Join(slicePath, baseName)
	output, err := os.Create(merged)
	if err != nil {
		return "", fmt.Errorf("create merged file failed: %w", err)
	}
	for _, name := range names {
		input, err := os.Open(filepath.Join(itemsDir, name))
		if err != nil {
			_ = output.Close()
			return "", fmt.Errorf("read sharding piece failed: %w", err)
		}
		_, copyErr := io.Copy(output, input)
		_ = input.Close()
		if copyErr != nil {
			_ = output.Close()
			return "", fmt.Errorf("merge sharding pieces failed: %w", copyErr)
		}
	}
	if err := output.Close(); err != nil {
		return "", err
	}
	_ = os.RemoveAll(itemsDir)
	newMD5, err := fileMD5(merged)
	if err != nil {
		return "", err
	}
	if !strings.EqualFold(newMD5, fileSumMD5) {
		a.Log().Warn("file merge mismatch", "file", merged, "md5", newMD5, "expect", fileSumMD5)
		return "", paramErr(405, "i18n.file_merge_error.f32f")
	}
	return installAgentPackage(a, merged)
}

func sliceIndex(name string) int {
	index := strings.LastIndex(name, ".")
	if index < 0 {
		return 0
	}
	value, err := strconv.Atoi(name[index+1:])
	if err != nil {
		return 0
	}
	return value
}

func fileMD5(path string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer func() { _ = file.Close() }()
	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return "", err
	}
	return hex.EncodeToString(hash.Sum(nil)), nil
}

// installAgentPackage 校验并安装 Go 插件端升级包，返回给服务端的提示语。
/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

// InstallUploadedPackage 是 ws.UpgradeHandler 的实现入口（/node_update 上传完成后调用）。
func InstallUploadedPackage(a *app.App, packagePath string) (string, error) {
	return installAgentPackage(a, packagePath)
}

// goAgentPackageInfo 描述升级容器 jar 内的 go-agent 元信息（与 Server 侧约定的容器格式）。
type goAgentPackageInfo struct {
	Version    string
	TimeStamp  string
	MinVersion string
	Platform   string
	SHA256     string
	File       *zip.File
}

// installAgentPackage 校验并安装 Go 插件端升级包，返回给服务端的提示语。
//
// 升级包格式（jar 仅作为运输容器，方便复用 Java 的升级链路）：
//
//	go-agent/agent.json                    {version,timeStamp,minVersion,platform,sha256}
//	go-agent/bin/go-agent-<os>-<arch>      真正的可执行文件
//
// 兼容旧格式：zip 内直接放 agent / jpom-agent 可执行文件（执行 -version 读取版本）。
func installAgentPackage(a *app.App, packagePath string) (string, error) {
	ext := strings.ToLower(filepath.Ext(packagePath))
	if ext != ".zip" && ext != ".jar" {
		return "", paramErr(405, "i18n.not_jpom_install_package.2cca")
	}
	reader, err := zip.OpenReader(packagePath)
	if err != nil {
		return "", paramErr(405, "i18n.not_jpom_install_package.2cca")
	}
	defer func() { _ = reader.Close() }()
	var (
		entry   *zip.File
		version string
		sha256  string
	)
	if info := inspectGoAgentPackage(reader); info != nil {
		if info.Platform != "" && !strings.Contains(strings.ToLower(info.Platform), runtime.GOOS) {
			return "", paramErr(405, "i18n.go_upgrade_platform_mismatch", info.Platform)
		}
		if info.MinVersion != "" && compareVersion(a.Version, info.MinVersion) < 0 {
			return "", paramErr(405, "i18n.incompatible_program_versions.5291", a.Version, info.MinVersion)
		}
		entry = info.File
		version = info.Version
		sha256 = info.SHA256
	} else {
		if ext == ".jar" {
			return "", paramErr(405, javaPackageLimitMessage)
		}
		entry = findLegacyBinary(reader)
	}
	binaryPath, err := extractEntry(a, entry)
	if err != nil {
		return "", err
	}
	if sha256 != "" {
		actual, err := fileSHA256(binaryPath)
		if err != nil {
			return "", err
		}
		if !strings.EqualFold(actual, sha256) {
			return "", paramErr(405, "i18n.go_upgrade_sha256_mismatch")
		}
	}
	if version == "" {
		version, err = binaryVersion(a, binaryPath)
		if err != nil {
			return "", err
		}
	}
	if err := checkUpgradeVersion(a, version); err != nil {
		return "", err
	}
	if err := replaceAgentBinary(a, binaryPath, version); err != nil {
		return "", err
	}
	a.RestartAfter(2 * time.Second)
	return UpgradeMessage, nil
}

const javaPackageLimitMessage = "i18n.go_java_package_not_supported"

// inspectGoAgentPackage 读取 go-agent/agent.json 并定位当前平台的可执行文件。
func inspectGoAgentPackage(reader *zip.ReadCloser) *goAgentPackageInfo {
	var manifestFile *zip.File
	for _, file := range reader.File {
		name := strings.ToLower(strings.TrimPrefix(file.Name, "./"))
		if file.FileInfo().IsDir() || strings.Contains(name, "..") {
			continue
		}
		if name == "go-agent/agent.json" {
			manifestFile = file
		}
	}
	if manifestFile == nil {
		return nil
	}
	source, err := manifestFile.Open()
	if err != nil {
		return nil
	}
	data, err := io.ReadAll(source)
	_ = source.Close()
	if err != nil {
		return nil
	}
	var meta map[string]any
	if jsonUnmarshal(data, &meta) != nil {
		return nil
	}
	info := &goAgentPackageInfo{
		Version:    strings.TrimSpace(toString(meta["version"])),
		TimeStamp:  strings.TrimSpace(toString(meta["timeStamp"])),
		MinVersion: strings.TrimSpace(toString(meta["minVersion"])),
		Platform:   strings.TrimSpace(toString(meta["platform"])),
		SHA256:     strings.ToLower(strings.TrimSpace(toString(meta["sha256"]))),
	}
	wanted := runtime.GOOS + "-" + runtime.GOARCH
	for _, file := range reader.File {
		name := strings.ToLower(strings.TrimPrefix(file.Name, "./"))
		if file.FileInfo().IsDir() || strings.Contains(name, "..") {
			continue
		}
		if !strings.HasPrefix(name, "go-agent/") {
			continue
		}
		base := strings.ToLower(filepath.Base(name))
		if strings.HasSuffix(base, ".json") || strings.HasSuffix(base, ".md") || strings.HasSuffix(base, ".txt") {
			continue
		}
		if info.File == nil {
			info.File = file
		}
		if strings.Contains(base, wanted) || (strings.Contains(base, runtime.GOOS) && strings.Contains(base, runtime.GOARCH)) {
			info.File = file
			break
		}
	}
	return info
}

// findLegacyBinary 兼容旧 zip：直接查找 agent / jpom-agent 可执行文件。
func findLegacyBinary(reader *zip.ReadCloser) *zip.File {
	for _, file := range reader.File {
		if file.FileInfo().IsDir() {
			continue
		}
		name := strings.ToLower(strings.TrimPrefix(file.Name, "./"))
		if strings.Contains(name, "..") {
			continue
		}
		base := strings.ToLower(filepath.Base(name))
		if base == "agent" || base == "jpom-agent" || strings.HasPrefix(base, "jpom-agent-") {
			return file
		}
	}
	return nil
}

// extractEntry 把 zip 条目解压到临时目录并校验可执行文件头。
func extractEntry(a *app.App, entry *zip.File) (string, error) {
	if entry == nil {
		return "", paramErr(405, javaPackageLimitMessage)
	}
	if err := os.MkdirAll(a.TempPath(), 0o755); err != nil {
		return "", err
	}
	target := filepath.Join(a.TempPath(), ".agent-upgrade-binary")
	output, err := os.Create(target)
	if err != nil {
		return "", err
	}
	source, err := entry.Open()
	if err != nil {
		_ = output.Close()
		return "", err
	}
	_, copyErr := io.Copy(output, source)
	_ = source.Close()
	if copyErr != nil {
		_ = output.Close()
		return "", copyErr
	}
	if err := output.Sync(); err != nil {
		_ = output.Close()
		return "", err
	}
	if err := output.Close(); err != nil {
		return "", err
	}
	if err := os.Chmod(target, 0o755); err != nil {
		return "", err
	}
	if err := verifyExecutable(target); err != nil {
		return "", err
	}
	return target, nil
}

// fileSHA256 计算文件 sha256（小写 hex）。
func fileSHA256(path string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer func() { _ = file.Close() }()
	hash := sha256.New()
	if _, err := io.Copy(hash, file); err != nil {
		return "", err
	}
	return hex.EncodeToString(hash.Sum(nil)), nil
}

// verifyExecutable 校验文件头是否与当前操作系统匹配。
func verifyExecutable(path string) error {
	header := make([]byte, 4)
	file, err := os.Open(path)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	if _, err := io.ReadFull(file, header); err != nil {
		return paramErr(405, "i18n.go_upgrade_invalid_executable")
	}
	switch runtime.GOOS {
	case "windows":
		if header[0] != 'M' || header[1] != 'Z' {
			return paramErr(405, "i18n.go_upgrade_invalid_windows_executable")
		}
	case "darwin":
		if !(header[0] == 0xfe && header[1] == 0xed && header[2] == 0xfa && header[3] == 0xce) &&
			!(header[0] == 0xfe && header[1] == 0xed && header[2] == 0xfa && header[3] == 0xcf) &&
			!(header[0] == 0xca && header[1] == 0xfe && header[2] == 0xba && header[3] == 0xbe) {
			return paramErr(405, "i18n.go_upgrade_invalid_macos_executable")
		}
	default:
		if header[0] != 0x7f || header[1] != 'E' || header[2] != 'L' || header[3] != 'F' {
			return paramErr(405, "i18n.go_upgrade_invalid_linux_executable")
		}
	}
	return nil
}

// binaryVersion 执行可执行文件的 -version 参数读取版本号。
func binaryVersion(a *app.App, path string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	output, err := exec.CommandContext(ctx, path, "-version").Output()
	if err != nil {
		return "", paramErr(405, "i18n.go_upgrade_read_version_failed", err.Error())
	}
	version := strings.TrimSpace(strings.SplitN(strings.TrimSpace(string(output)), "\n", 2)[0])
	if version == "" || len(version) > 64 {
		return "", paramErr(405, "i18n.go_upgrade_invalid_version")
	}
	a.Log().Info("parse upgrade package version", "version", version)
	return version, nil
}

// checkUpgradeVersion 与 Java checkJpomJar 的版本检查语义保持一致。
func checkUpgradeVersion(a *app.App, version string) error {
	if version == a.Version {
		return paramErr(405, "i18n.new_package_same_as_running_package.e25a")
	}
	if !a.Config.Jpom.System.AllowedDowngrade && compareVersion(version, a.Version) < 0 {
		return paramErr(405, "i18n.online_upgrade_cannot_downgrade.d419")
	}
	return nil
}

// replaceAgentBinary 备份旧二进制并用新二进制替换自身，同时写入 upgrade.json/run.bin。
func replaceAgentBinary(a *app.App, newBinary, version string) error {
	runDir := a.RunDir
	if err := os.MkdirAll(runDir, 0o755); err != nil {
		return err
	}
	oldJars := a.OldJarsPath()
	if err := os.MkdirAll(oldJars, 0o755); err != nil {
		return err
	}
	backupName := fmt.Sprintf("%s-%s-%s", filepath.Base(a.Executable), a.Version,
		time.Now().Format("20060102150405"))
	backupPath := filepath.Join(oldJars, backupName)
	if err := copyFile(a.Executable, backupPath); err != nil {
		a.Log().Warn("backup current executable failed", "error", err)
	}
	staged := a.Executable + ".upgrade"
	if err := copyFile(newBinary, staged); err != nil {
		return fmt.Errorf("write new version file failed: %w", err)
	}
	if err := os.Chmod(staged, 0o755); err != nil {
		return err
	}
	if err := os.Rename(staged, a.Executable); err != nil {
		return fmt.Errorf("replace executable failed: %w", err)
	}
	upgradeCount := 0
	upgradeFile := filepath.Join(runDir, "upgrade.json")
	if content, err := os.ReadFile(upgradeFile); err == nil {
		var existing map[string]any
		if jsonUnmarshal(content, &existing) == nil {
			if count, ok := existing["upgradeCount"]; ok {
				upgradeCount = int(toInt64(count))
			}
		}
	}
	upgradeCount++
	upgradeInfo := map[string]any{
		"beforeJar":     backupName,
		"newJar":        filepath.Base(a.Executable),
		"beforeVersion": a.Version,
		"newVersion":    version,
		"updateTime":    time.Now().Format("2006-01-02 15:04:05"),
		"upgradeCount":  upgradeCount,
		"status":        "installed",
	}
	_ = writeJSONAtomic(upgradeFile, upgradeInfo)
	_ = os.WriteFile(filepath.Join(runDir, "run.bin"), []byte(filepath.Base(a.Executable)), 0o644)
	// G7：写升级待确认标记；重启后健康检查通过才删除，启动失败则自动回滚。
	pending := &upgradePending{
		BeforeJar:     backupName,
		BeforeVersion: a.Version,
		NewVersion:    version,
		UpdateTime:    time.Now().Format("2006-01-02 15:04:05"),
	}
	if err := writeUpgradePending(a, pending); err != nil {
		a.Log().Warn("write upgrade pending marker failed; automatic rollback is unavailable", "error", err)
	}
	cleanOldJars(oldJars, a.Config.Jpom.System.OldJarsCount)
	a.Log().Info("agent upgrade package installed", "version", version, "backup", backupPath)
	return nil
}

// cleanOldJars 按 jpom.system.old-jars-count 保留旧版本文件个数。
func cleanOldJars(dir string, keep int) {
	if keep <= 0 {
		return
	}
	entries, err := os.ReadDir(dir)
	if err != nil {
		return
	}
	type fileInfo struct {
		name    string
		modTime time.Time
	}
	files := make([]fileInfo, 0, len(entries))
	for _, entry := range entries {
		info, err := entry.Info()
		if err != nil || info.IsDir() {
			continue
		}
		files = append(files, fileInfo{name: entry.Name(), modTime: info.ModTime()})
	}
	sort.SliceStable(files, func(i, j int) bool { return files[i].modTime.After(files[j].modTime) })
	for index, file := range files {
		if index < keep {
			continue
		}
		_ = os.Remove(filepath.Join(dir, file.name))
	}
}

// RemoteUpgrade 对应 /system/remote_upgrade.json。
func RemoteUpgrade(a *app.App) (string, error) {
	remote, err := LoadRemoteInfo(a)
	if err != nil || remote == nil {
		return "", paramErr(405, "i18n.no_available_new_version_upgrade.d8f2")
	}
	remoteURL := strings.TrimSpace(toString(remote["agentUrl"]))
	if remoteURL == "" {
		return "", paramErr(405, "i18n.new_version_exists_download_unavailable.4ba7")
	}
	ext := filepath.Ext(remoteURL)
	if index := strings.Index(ext, "?"); index >= 0 {
		ext = ext[:index]
	}
	if ext == "" {
		ext = ".zip"
	}
	target := filepath.Join(a.TempPath(), "remote-upgrade"+ext)
	if err := downloadFile(remoteURL, target); err != nil {
		return "", err
	}
	return installAgentPackage(a, target)
}

func downloadFile(url, target string) error {
	client := &http.Client{Timeout: 10 * time.Minute}
	response, err := client.Get(url)
	if err != nil {
		return paramErr(405, "i18n.go_upgrade_download_failed", err.Error())
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode != http.StatusOK {
		if response.StatusCode == http.StatusForbidden {
			return paramErr(405, "i18n.go_upgrade_download_forbidden")
		}
		return paramErr(405, "i18n.go_upgrade_download_failed", fmt.Sprintf("HTTP %d", response.StatusCode))
	}
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return err
	}
	output, err := os.Create(target)
	if err != nil {
		return err
	}
	defer func() { _ = output.Close() }()
	_, err = io.Copy(output, response.Body)
	return err
}
