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
	"net/url"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
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
	return installAgentPackageFor(a, packagePath, currentPlatform())
}

// installAgentPackageFor 以指定目标平台校验/安装升级包；正式流程始终使用
// runtime.GOOS/runtime.GOARCH，显式平台仅用于跨架构单测。
func installAgentPackageFor(a *app.App, packagePath string, target targetPlatform) (string, error) {
	plan, err := prepareAgentUpgrade(a, packagePath, target)
	if err != nil {
		return "", err
	}
	if err := replaceAgentBinary(a, plan.binaryPath, plan.version); err != nil {
		return "", err
	}
	restartAfterUpgrade(a, 2*time.Second)
	return UpgradeMessage, nil
}

// restartAfterUpgrade 是重启的注入点，便于单测完整验证“校验-替换-重启”链路
// 而不真的重启测试进程；正式流程固定调用 App.RestartAfter。
var restartAfterUpgrade = func(a *app.App, delay time.Duration) { a.RestartAfter(delay) }

// agentUpgradePlan 是已通过全部校验、等待替换的升级包。
type agentUpgradePlan struct {
	binaryPath string
	version    string
}

// prepareAgentUpgrade 执行升级包的全部校验（平台/版本/容器内文件精确定位/
// sha256/可执行文件 machine），只有全部通过才返回待替换二进制。
// 任何失败都不会改动当前二进制，也不会写 upgrade-pending.json（CONTRACT §3.4）。
func prepareAgentUpgrade(a *app.App, packagePath string, target targetPlatform) (*agentUpgradePlan, error) {
	ext := strings.ToLower(filepath.Ext(packagePath))
	if ext != ".zip" && ext != ".jar" {
		return nil, paramErr(405, "i18n.not_jpom_install_package.2cca")
	}
	reader, err := zip.OpenReader(packagePath)
	if err != nil {
		return nil, paramErr(405, "i18n.not_jpom_install_package.2cca")
	}
	defer func() { _ = reader.Close() }()
	var (
		entry   *zip.File
		version string
		sha256  string
	)
	if info := inspectGoAgentPackage(reader, target); info != nil {
		if err := checkPackagePlatform(info.Platform, target); err != nil {
			return nil, err
		}
		if info.MinVersion != "" && compareVersion(a.Version, info.MinVersion) < 0 {
			return nil, paramErr(405, "i18n.incompatible_program_versions.5291", a.Version, info.MinVersion)
		}
		if info.File == nil {
			// CONTRACT §3.2：容器内必须精确存在当前平台可执行文件，禁止 fallback。
			return nil, paramErrAppend(405, "i18n.go_upgrade_missing_binary", " "+target.containerBinaryPath())
		}
		entry = info.File
		version = info.Version
		sha256 = info.SHA256
	} else {
		if ext == ".jar" {
			return nil, paramErr(405, javaPackageLimitMessage)
		}
		entry = findLegacyBinary(reader)
		if entry == nil {
			return nil, paramErr(405, javaPackageLimitMessage)
		}
	}
	binaryPath, err := extractEntry(a, entry, target)
	if err != nil {
		return nil, err
	}
	if sha256 != "" {
		actual, err := fileSHA256(binaryPath)
		if err != nil {
			return nil, err
		}
		if !strings.EqualFold(actual, sha256) {
			return nil, paramErr(405, "i18n.go_upgrade_sha256_mismatch")
		}
	}
	if version == "" {
		version, err = binaryVersion(a, binaryPath)
		if err != nil {
			return nil, err
		}
	}
	if err := checkUpgradeVersion(a, version); err != nil {
		return nil, err
	}
	return &agentUpgradePlan{binaryPath: binaryPath, version: version}, nil
}

// checkPackagePlatform 校验 agent.json.platform 与目标平台完全一致（大小写归一化）。
// platform 为空视为旧格式：仅允许 linux/amd64 节点（CONTRACT §3.1/§3.2）。
func checkPackagePlatform(packagePlatform string, target targetPlatform) error {
	if strings.TrimSpace(packagePlatform) == "" {
		if target.isLegacyLinuxAmd64() {
			return nil
		}
		// 旧格式容器缺少 platform：仅 linux/amd64 允许，其余平台给出明确提示。
		return paramErr(405, "i18n.go_upgrade_platform_mismatch", fmt.Sprintf("missing platform (expected %s)", target.String()))
	}
	if !platformEqual(packagePlatform, target.String()) {
		return paramErr(405, "i18n.go_upgrade_platform_mismatch", fmt.Sprintf("%s (expected %s)", packagePlatform, target.String()))
	}
	return nil
}

const javaPackageLimitMessage = "i18n.go_java_package_not_supported"

// inspectGoAgentPackage 读取 go-agent/agent.json 并精确定位目标平台的可执行文件。
//
// CONTRACT §3.2：容器内必须存在 go-agent/bin/go-agent-<os>-<arch>
// （windows 为 go-agent-<os>-<arch>.exe），不存在时 File 为 nil，由调用方返回 405；
// 这里绝不 fallback 到 go-agent/ 下的任意其他文件。
func inspectGoAgentPackage(reader *zip.ReadCloser, target targetPlatform) *goAgentPackageInfo {
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
	expected := strings.ToLower(target.containerBinaryPath())
	for _, file := range reader.File {
		name := strings.ToLower(strings.TrimPrefix(file.Name, "./"))
		if file.FileInfo().IsDir() || strings.Contains(name, "..") {
			continue
		}
		if name == expected {
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

// extractEntry 把 zip 条目解压到临时目录并校验可执行文件头（OS magic + 架构 machine）。
func extractEntry(a *app.App, entry *zip.File, target targetPlatform) (string, error) {
	if entry == nil {
		return "", paramErr(405, javaPackageLimitMessage)
	}
	if err := os.MkdirAll(a.TempPath(), 0o755); err != nil {
		return "", err
	}
	binaryPath := filepath.Join(a.TempPath(), ".agent-upgrade-binary")
	output, err := os.Create(binaryPath)
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
	if err := os.Chmod(binaryPath, 0o755); err != nil {
		return "", err
	}
	if err := verifyExecutableFor(binaryPath, target); err != nil {
		return "", err
	}
	return binaryPath, nil
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
	if err := replaceExecutable(staged, a.Executable); err != nil {
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

// RemoteUpgrade 对应 /system/remote_upgrade.json（节点“远程升级”）。
//
// 从 RemoteVersion.agentUrl 推导“同版本 + 当前平台”的 Go 升级容器 URL：
//
//	<agentUrl 所在目录>/agent-go-<version>-upgrade-<os>-<arch>.jar
//
// linux/amd64 额外回退兼容别名 agent-go-<version>-upgrade.jar（旧命名）。
// 不再直接下载 agentUrl：那是 Java Agent 包，Go 插件端无法使用（CONTRACT §5.15）。
func RemoteUpgrade(a *app.App) (string, error) {
	remote, err := LoadRemoteInfo(a)
	if err != nil || remote == nil {
		return "", paramErr(405, "i18n.no_available_new_version_upgrade.d8f2")
	}
	agentURL := strings.TrimSpace(toString(remote["agentUrl"]))
	if agentURL == "" {
		return "", paramErr(405, "i18n.new_version_exists_download_unavailable.4ba7")
	}
	platform := currentPlatform()
	urls, err := goUpgradeContainerURLs(agentURL, toString(remote["tagName"]), platform)
	if err != nil {
		return "", err
	}
	targetFile := filepath.Join(a.TempPath(), "remote-upgrade-"+platform.GOOS+"-"+platform.GOARCH+".jar")
	var lastErr error
	for _, upgradeURL := range urls {
		a.Log().Info("download go agent upgrade container", "url", upgradeURL)
		status, downloadErr := downloadPackageFile(upgradeURL, targetFile)
		if downloadErr == nil {
			return installAgentPackage(a, targetFile)
		}
		lastErr = downloadErr
		// 仅“文件不存在/被拒绝”时尝试兼容别名；其他错误（网络/写盘）直接返回。
		if status != http.StatusNotFound && status != http.StatusForbidden {
			return "", downloadErr
		}
	}
	if lastErr == nil {
		lastErr = paramErr(405, "i18n.new_version_exists_download_unavailable.4ba7")
	}
	return "", lastErr
}

// goUpgradeContainerURLs 按 RemoteVersion.agentUrl 推导当前平台的 Go 升级容器 URL。
// 返回顺序：精确平台容器 -> linux/amd64 兼容别名（仅 linux/amd64）。
func goUpgradeContainerURLs(agentURL, tagName string, target targetPlatform) ([]string, error) {
	parsed, err := url.Parse(strings.TrimSpace(agentURL))
	if err != nil || parsed.Scheme == "" || parsed.Host == "" {
		return nil, paramErr(405, "i18n.new_version_exists_download_unavailable.4ba7")
	}
	version := agentVersionFromURL(parsed.Path)
	if version == "" {
		version = strings.TrimPrefix(strings.TrimSpace(tagName), "v")
	}
	if version == "" {
		return nil, paramErr(405, "i18n.new_version_exists_download_unavailable.4ba7")
	}
	base := *parsed
	base.Path = path.Dir(parsed.Path)
	base.RawPath = ""
	base.RawQuery = ""
	base.Fragment = ""
	baseURL := strings.TrimSuffix(base.String(), "/")
	urls := []string{baseURL + "/agent-go-" + version + "-upgrade-" + target.GOOS + "-" + target.GOARCH + ".jar"}
	if target.isLegacyLinuxAmd64() {
		urls = append(urls, baseURL+"/agent-go-"+version+"-upgrade.jar")
	}
	return urls, nil
}

// agentVersionFromURL 从 agent-<version>-release.zip 之类的文件名解析版本号。
func agentVersionFromURL(urlPath string) string {
	name := path.Base(urlPath)
	name = strings.TrimPrefix(name, "agent-")
	index := strings.Index(name, "-release.")
	if index <= 0 {
		return ""
	}
	return name[:index]
}

// downloadPackageFile 下载升级包并返回 HTTP 状态码（0 表示未收到响应）。
func downloadPackageFile(url, target string) (int, error) {
	client := &http.Client{Timeout: 10 * time.Minute}
	response, err := client.Get(url)
	if err != nil {
		return 0, paramErr(405, "i18n.go_upgrade_download_failed", err.Error())
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode != http.StatusOK {
		if response.StatusCode == http.StatusForbidden {
			return response.StatusCode, paramErr(405, "i18n.go_upgrade_download_forbidden")
		}
		return response.StatusCode, paramErr(405, "i18n.go_upgrade_download_failed", fmt.Sprintf("HTTP %d", response.StatusCode))
	}
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return response.StatusCode, err
	}
	output, err := os.Create(target)
	if err != nil {
		return response.StatusCode, err
	}
	defer func() { _ = output.Close() }()
	if _, err := io.Copy(output, response.Body); err != nil {
		return response.StatusCode, err
	}
	return response.StatusCode, output.Sync()
}
