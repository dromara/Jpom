//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package projectfile

import (
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"
	"unicode/utf16"

	"github.com/dromara/Jpom/go-agent/internal/project"
)

// Project file version snapshots (build release triggered, used for fast
// rollback) are a separate capability from the file backup: a version is a full
// directory snapshot taken after a build release, while backups are reverse
// diff snapshots triggered by file operations.
//
// The layout, the meta fields, the manifest algorithm and the cleanup rules are
// kept byte compatible with the Java agent ProjectFileVersionService so the same
// server and web UI can drive both implementations:
//
//	<root>/<versionId>/      full snapshot content
//	<root>/<versionId>.json  meta (versionId, source, build ids, size, manifest)
//	<root>/current.txt       id of the active version
const (
	// versionDirName is the default version snapshot root inside the agent data
	// dir (data/project_file_version/<projectId>).
	versionDirName = "project_file_version"
	// versionMetaSuffix is the meta file suffix, stored next to the version
	// directory so the metadata never pollutes the snapshot content.
	versionMetaSuffix = ".json"
	// versionCurrentFile stores the id of the active version.
	versionCurrentFile = "current.txt"
	// versionTempSuffix marks a temporary directory while a snapshot is built.
	versionTempSuffix = ".tmp"
	// versionSourceBaseline is the source of the first (baseline) snapshot,
	// versionSourceBuild the source of a build release snapshot and
	// versionSourceAuto the source of the safety snapshot taken before a switch.
	versionSourceBaseline = "baseline"
	versionSourceBuild    = "build"
	versionSourceAuto     = "auto"
)

// versionIgnoreNames are the top level names excluded from a snapshot, from the
// switch cleanup and from the manifest (Java IGNORE_NAMES).
var versionIgnoreNames = []string{"logs", "temp"}

// VersionMeta is one version snapshot meta, the same JSON object the Java agent
// writes to <root>/<versionId>.json and returns in version-list.
type VersionMeta struct {
	VersionID     string  `json:"versionId"`
	Source        string  `json:"source"`
	BuildNumberID *int    `json:"buildNumberId,omitempty"`
	BuildDataID   *string `json:"buildDataId,omitempty"`
	CreateTime    int64   `json:"createTime"`
	FileCount     int     `json:"fileCount"`
	FileSize      int64   `json:"fileSize"`
	Manifest      string  `json:"manifest"`
	// VersionDir and Exist are added by version-list only, the Java agent adds
	// them after parsing the meta file (they are never persisted).
	VersionDir string `json:"versionDir,omitempty"`
	Exist      *bool  `json:"exist,omitempty"`
}

// VersionCapability is the payload of /manage/file/version-capability.
type VersionCapability struct {
	Support      bool   `json:"support"`
	Enabled      bool   `json:"enabled"`
	VersionCount int    `json:"versionCount"`
	Path         string `json:"path"`
}

// VersionBeginResult is the payload of /manage/file/version-begin.
type VersionBeginResult struct {
	Enabled      bool
	VersionCount int
	// Baseline is set when the first call created the baseline snapshot.
	Baseline *VersionMeta
	// Current is the active version id, nil when no version has been committed.
	Current *string
	// Count is the number of versions after the cleanup, nil when disabled.
	Count *int
}

// MarshalJSON mirrors the Java JSONObject built by ProjectFileVersionService#begin:
// the disabled branch only carries enabled/versionCount while the enabled branch
// always carries current (null when nothing was committed yet) and count.
func (r VersionBeginResult) MarshalJSON() ([]byte, error) {
	body := map[string]any{
		"enabled":      r.Enabled,
		"versionCount": r.VersionCount,
	}
	if r.Baseline != nil {
		body["baseline"] = r.Baseline
	}
	if r.Count != nil {
		body["count"] = *r.Count
		if r.Current != nil {
			body["current"] = *r.Current
		} else {
			body["current"] = nil
		}
	}
	return json.Marshal(body)
}

// VersionCommitResult is the payload of /manage/file/version-commit.
type VersionCommitResult struct {
	Enabled    bool   `json:"enabled"`
	VersionID  string `json:"versionId,omitempty"`
	Manifest   string `json:"manifest,omitempty"`
	FileSize   *int64 `json:"fileSize,omitempty"`
	FileCount  *int   `json:"fileCount,omitempty"`
	CreateTime *int64 `json:"createTime,omitempty"`
	Count      *int   `json:"count,omitempty"`
}

// VersionListResult is the payload of /manage/file/version-list.
type VersionListResult struct {
	Path         string         `json:"path"`
	Enabled      bool           `json:"enabled"`
	VersionCount int            `json:"versionCount"`
	Current      *string        `json:"current"`
	List         []*VersionMeta `json:"list"`
}

// VersionSwitchResult is the payload of /manage/file/version-switch.
type VersionSwitchResult struct {
	VersionID         string  `json:"versionId"`
	PreviousVersionID *string `json:"previousVersionId"`
	SafetyVersionID   *string `json:"safetyVersionId"`
	Restarted         bool    `json:"restarted"`
}

// versionSettings are the effective version snapshot options of one project.
type versionSettings struct {
	// ID is the project id owning the version directory (the linked project for
	// runMode=Link projects).
	ID string
	// Path is the custom version root from the DSL config, empty when unset.
	Path string
	// Count is the number of versions to keep, <=0 disables snapshots.
	Count int
}

// versionSettings resolves the effective version options, same as
// ProjectFileVersionService#resolveVersionCount/#pathProjectVersion.
func (s *Service) versionSettings(info map[string]any) (versionSettings, error) {
	model, err := s.ResolveModel(info)
	if err != nil {
		return versionSettings{}, err
	}
	settings := versionSettings{
		ID:    project.String(model, "id"),
		Path:  strings.TrimSpace(s.app.Config.Jpom.Project.VersionPath),
		Count: s.app.Config.Jpom.Project.VersionCount,
	}
	if settings.ID == "" {
		settings.ID = project.String(info, "id")
	}
	dsl, dslErr := s.dsl(model)
	if dslErr != nil {
		return versionSettings{}, dslErr
	}
	if dsl != nil && dsl.File != nil {
		if dsl.File.VersionCount != nil {
			settings.Count = *dsl.File.VersionCount
		}
		if path := strings.TrimSpace(dsl.File.VersionPath); path != "" {
			settings.Path = path
		}
	}
	return settings, nil
}

// versionRoot returns the directory holding every version snapshot of a project.
func (s *Service) versionRoot(settings versionSettings) string {
	if settings.Path == "" {
		return filepath.Join(s.app.DataDir, versionDirName, settings.ID)
	}
	return filepath.Join(settings.Path, settings.ID)
}

// resolveKeepCount resolves the cleanup count of one call: an explicit positive
// keepCount wins over the project/agent configuration.
func resolveKeepCount(configured int, keepCount *int) int {
	if keepCount != nil && *keepCount > 0 {
		return *keepCount
	}
	return configured
}

// VersionCapability implements /manage/file/version-capability.
func (s *Service) VersionCapability(id string) (*VersionCapability, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	settings, err := s.versionSettings(info)
	if err != nil {
		return nil, err
	}
	return &VersionCapability{
		Support:      true,
		Enabled:      settings.Count > 0,
		VersionCount: settings.Count,
		Path:         absolutePath(s.versionRoot(settings)),
	}, nil
}

// VersionBegin implements /manage/file/version-begin: the release preparation
// step which creates the baseline snapshot on the first enabled release.
func (s *Service) VersionBegin(id string, buildNumberID *int, keepCount *int) (*VersionBeginResult, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	settings, err := s.versionSettings(info)
	if err != nil {
		return nil, err
	}
	versionCount := resolveKeepCount(settings.Count, keepCount)
	result := &VersionBeginResult{Enabled: versionCount > 0, VersionCount: versionCount}
	if versionCount <= 0 {
		return result, nil
	}
	root := absolutePath(s.versionRoot(settings))
	if err := os.MkdirAll(root, 0o755); err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	lib, err := s.LibPath(info)
	if err != nil {
		return nil, err
	}
	if len(s.listVersionMeta(root)) == 0 && isDir(lib) && dirHasFile(lib) {
		baseline, snapErr := s.snapshotVersion(root, lib, versionSourceBaseline+"-"+newBackupID(),
			versionSourceBaseline, buildNumberID, nil)
		if snapErr != nil {
			return nil, snapErr
		}
		result.Baseline = baseline
		s.Log().Debug("project file version baseline created", "project", id, "versionId", baseline.VersionID)
	}
	s.clearOldVersion(root, "", versionCount)
	result.Current = s.readVersionCurrent(root)
	count := len(s.listVersionMeta(root))
	result.Count = &count
	return result, nil
}

// VersionCommit implements /manage/file/version-commit: register the snapshot of
// a successful build release and make it the active version.
func (s *Service) VersionCommit(id string, buildNumberID *int, buildDataID string, keepCount *int) (*VersionCommitResult, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	settings, err := s.versionSettings(info)
	if err != nil {
		return nil, err
	}
	versionCount := resolveKeepCount(settings.Count, keepCount)
	result := &VersionCommitResult{Enabled: versionCount > 0}
	if versionCount <= 0 {
		return result, nil
	}
	lib, err := s.LibPath(info)
	if err != nil {
		return nil, err
	}
	if !isDir(lib) || !dirHasFile(lib) {
		return nil, invalidf("i18n.version_project_file_empty.71d4")
	}
	root := absolutePath(s.versionRoot(settings))
	if err := os.MkdirAll(root, 0o755); err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	var versionID string
	if buildNumberID != nil {
		versionID = strconv.Itoa(*buildNumberID)
	} else {
		// Java: buildNumberId == null -> "build-" + now()
		versionID = versionSourceBuild + "-" + newBackupID()
	}
	if exists(filepath.Join(root, versionID)) {
		versionID = versionID + "-" + newBackupID()
	}
	var buildDataValue *string
	if strings.TrimSpace(buildDataID) != "" {
		value := buildDataID
		buildDataValue = &value
	}
	meta, err := s.snapshotVersion(root, lib, versionID, versionSourceBuild, buildNumberID, buildDataValue)
	if err != nil {
		return nil, err
	}
	if err := s.writeVersionCurrent(root, versionID); err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	s.clearOldVersion(root, "", versionCount)
	count := len(s.listVersionMeta(root))
	result.VersionID = meta.VersionID
	result.Manifest = meta.Manifest
	result.FileSize = &meta.FileSize
	result.FileCount = &meta.FileCount
	result.CreateTime = &meta.CreateTime
	result.Count = &count
	return result, nil
}

// VersionList implements /manage/file/version-list.
func (s *Service) VersionList(id string) (*VersionListResult, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	settings, err := s.versionSettings(info)
	if err != nil {
		return nil, err
	}
	root := absolutePath(s.versionRoot(settings))
	return &VersionListResult{
		Path:         root,
		Enabled:      settings.Count > 0,
		VersionCount: settings.Count,
		Current:      s.readVersionCurrent(root),
		List:         s.listVersionMeta(root),
	}, nil
}

// VersionSwitch implements /manage/file/version-switch (fast rollback): take an
// optional safety snapshot of the current directory, replace the project content
// with the version content, verify the manifest and optionally restart.
//
// Any failure restores the safety snapshot and reports i18n.version_switch_failed.
func (s *Service) VersionSwitch(id, versionID string, after int, snapshotCurrent *bool, keepCount *int) (*VersionSwitchResult, error) {
	if strings.TrimSpace(versionID) == "" {
		return nil, invalidf("i18n.version_id_required.0a3e")
	}
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	settings, err := s.versionSettings(info)
	if err != nil {
		return nil, err
	}
	root := absolutePath(s.versionRoot(settings))
	target, err := secureJoin(root, versionID)
	if err != nil {
		return nil, err
	}
	if !isDir(target) || !dirHasFile(target) {
		return nil, invalidSuffix("i18n.version_not_exists.5d90", versionID)
	}
	lib, err := s.LibPath(info)
	if err != nil {
		return nil, err
	}
	previous := s.readVersionCurrent(root)
	var safety *VersionMeta
	if (snapshotCurrent == nil || *snapshotCurrent) && isDir(lib) && dirHasFile(lib) {
		safety, err = s.snapshotVersion(root, lib, versionSourceAuto+"-"+newBackupID(),
			versionSourceAuto, nil, nil)
		if err != nil {
			return nil, err
		}
	}
	if applyErr := s.applyVersion(info, lib, root, target, versionID, after); applyErr != nil {
		s.restoreVersion(lib, root, safety)
		return nil, failSuffix("i18n.version_switch_failed.9c31", applyErr.Error())
	}
	if err := s.writeVersionCurrent(root, versionID); err != nil {
		s.restoreVersion(lib, root, safety)
		return nil, failSuffix("i18n.version_switch_failed.9c31", err.Error())
	}
	s.clearOldVersion(root, versionID, resolveKeepCount(settings.Count, keepCount))
	result := &VersionSwitchResult{VersionID: versionID, PreviousVersionID: previous, Restarted: after > 0}
	if safety != nil {
		safetyID := safety.VersionID
		result.SafetyVersionID = &safetyID
	}
	return result, nil
}

// VersionDelete implements /manage/file/version-delete: the active version and
// unknown versions are rejected.
func (s *Service) VersionDelete(id, versionID string) error {
	if strings.TrimSpace(versionID) == "" {
		return invalidf("i18n.version_id_required.0a3e")
	}
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	settings, err := s.versionSettings(info)
	if err != nil {
		return err
	}
	root := absolutePath(s.versionRoot(settings))
	if current := s.readVersionCurrent(root); current != nil && *current == versionID {
		return invalidf("i18n.version_current_delete_forbidden.2e84")
	}
	target, err := secureJoin(root, versionID)
	if err != nil {
		return err
	}
	if !exists(target) {
		return invalidSuffix("i18n.version_not_exists.5d90", versionID)
	}
	_ = os.RemoveAll(target)
	if metaPath, metaErr := secureJoin(root, versionID+versionMetaSuffix); metaErr == nil {
		_ = os.RemoveAll(metaPath)
	}
	return nil
}

// applyVersion replaces the project content with the version content, verifies
// the manifest and restarts the project when after > 0.
func (s *Service) applyVersion(info map[string]any, lib, root, target, versionID string, after int) error {
	if err := clearVersionLib(lib); err != nil {
		return err
	}
	if err := copyVersionContent(target, lib); err != nil {
		return err
	}
	if meta := s.readVersionMeta(root, versionID); meta != nil && strings.TrimSpace(meta.Manifest) != "" {
		manifest, manifestErr := versionManifest(lib)
		if manifestErr != nil {
			return manifestErr
		}
		if manifest != meta.Manifest {
			return invalidSuffix("i18n.version_verify_failed.d3a9", versionID)
		}
	}
	if after > 0 {
		if Hooks.RestartProject == nil {
			s.Log().Warn("version switch requested a restart but no project commander is installed",
				"project", info["id"])
			return invalidSuffix("i18n.version_restart_failed.6b05", "")
		}
		success, msg, _ := Hooks.RestartProject(info)
		if !success {
			return invalidSuffix("i18n.version_restart_failed.6b05", msg)
		}
	}
	return nil
}

// restoreVersion copies the safety snapshot back after a failed switch
// (ProjectFileVersionService#restore).
func (s *Service) restoreVersion(lib, root string, safety *VersionMeta) {
	if safety == nil {
		return
	}
	safetyDir, err := secureJoin(root, safety.VersionID)
	if err != nil || !isDir(safetyDir) {
		return
	}
	if err := clearVersionLib(lib); err != nil {
		s.Log().Error("project file version switch rollback failed", "path", lib, "error", err)
		return
	}
	if err := copyVersionContent(safetyDir, lib); err != nil {
		s.Log().Error("project file version switch rollback failed", "path", lib, "error", err)
	}
}

// snapshotVersion creates a full version snapshot: the content is first copied
// into a temporary directory inside root and then moved over the target, so a
// failed copy never leaves a half written version behind.
func (s *Service) snapshotVersion(root, lib, versionID, source string, buildNumberID *int, buildDataID *string) (*VersionMeta, error) {
	target, err := secureJoin(root, versionID)
	if err != nil {
		return nil, err
	}
	temp, err := secureJoin(root, versionID+versionTempSuffix+newBackupID())
	if err != nil {
		return nil, err
	}
	if err := os.MkdirAll(temp, 0o755); err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	if err := copyVersionContent(lib, temp); err != nil {
		_ = os.RemoveAll(temp)
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	_ = os.RemoveAll(target)
	if err := movePath(temp, target, true); err != nil {
		_ = os.RemoveAll(temp)
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	fileCount, fileSize, err := versionStats(target)
	if err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	manifest, err := versionManifest(target)
	if err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	meta := &VersionMeta{
		VersionID:     versionID,
		Source:        source,
		BuildNumberID: buildNumberID,
		BuildDataID:   buildDataID,
		CreateTime:    time.Now().UnixMilli(),
		FileCount:     fileCount,
		FileSize:      fileSize,
		Manifest:      manifest,
	}
	if err := s.writeVersionMeta(root, meta); err != nil {
		return nil, failSuffix("i18n.version_snapshot_failed.7b2c", err.Error())
	}
	return meta, nil
}

// copyVersionContent copies the children of src into dest, skipping the ignored
// names (ProjectFileVersionService#copyDirContent).
func copyVersionContent(src, dest string) error {
	entries, err := os.ReadDir(src)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		if versionIgnoreName(entry.Name()) {
			continue
		}
		if err := copyFile(filepath.Join(src, entry.Name()), filepath.Join(dest, entry.Name()), true); err != nil {
			return err
		}
	}
	return nil
}

// clearVersionLib removes the project content while keeping the ignored names.
func clearVersionLib(lib string) error {
	if err := os.MkdirAll(lib, 0o755); err != nil {
		return err
	}
	entries, err := os.ReadDir(lib)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		if versionIgnoreName(entry.Name()) {
			continue
		}
		_ = os.RemoveAll(filepath.Join(lib, entry.Name()))
	}
	return nil
}

// versionIgnoreName reports whether a top level name is excluded from snapshots
// and from the switch cleanup (ProjectFileVersionService#isIgnore).
func versionIgnoreName(name string) bool {
	if strings.Contains(name, versionTempSuffix) {
		return true
	}
	for _, ignored := range versionIgnoreNames {
		if strings.EqualFold(name, ignored) {
			return true
		}
	}
	return false
}

// versionIgnoredPath reports whether a slash separated relative path is excluded
// from the version content fingerprint (ProjectFileVersionUtil#isInIgnorePath in
// the Java plugin): temporary names (anything containing ".tmp") are ignored at
// any depth, and any path segment equal to an ignored top level name excludes the
// whole subtree.
func versionIgnoredPath(relative string) bool {
	base := relative
	if index := strings.LastIndexByte(relative, '/'); index >= 0 {
		base = relative[index+1:]
	}
	if strings.Contains(base, versionTempSuffix) {
		return true
	}
	for _, segment := range strings.Split(relative, "/") {
		for _, ignored := range versionIgnoreNames {
			if strings.EqualFold(segment, ignored) {
				return true
			}
		}
	}
	return false
}

// versionStats returns the file count (ignored top level names excluded, like
// ProjectFileVersionService#countFiles) and the total size of every file below
// dir (FileUtil#size).
func versionStats(dir string) (int, int64, error) {
	count := 0
	var size int64
	err := filepath.WalkDir(dir, func(path string, entry os.DirEntry, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		info, infoErr := entry.Info()
		if infoErr != nil {
			return infoErr
		}
		if entry.IsDir() {
			return nil
		}
		size += info.Size()
		relative, relErr := filepath.Rel(dir, path)
		if relErr != nil {
			return nil
		}
		if versionIgnoredPath(filepath.ToSlash(relative)) {
			return nil
		}
		count++
		return nil
	})
	if err != nil {
		return 0, 0, err
	}
	return count, size, nil
}

// versionManifest computes the fingerprint of a directory content: the sha1 of
// the sorted "relative path|size|file sha1" lines (one per file).
func versionManifest(dir string) (string, error) {
	lines := make([]string, 0, 16)
	err := filepath.WalkDir(dir, func(path string, entry os.DirEntry, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		if entry.IsDir() {
			return nil
		}
		relative, relErr := filepath.Rel(dir, path)
		if relErr != nil {
			return nil
		}
		relative = filepath.ToSlash(relative)
		if versionIgnoredPath(relative) {
			return nil
		}
		info, infoErr := entry.Info()
		if infoErr != nil {
			return infoErr
		}
		fileSum, sumErr := fileSHA1(path)
		if sumErr != nil {
			return sumErr
		}
		lines = append(lines, fmt.Sprintf("%s|%d|%s", relative, info.Size(), fileSum))
		return nil
	})
	if err != nil {
		return "", err
	}
	// Java sorts the manifest lines with String#compareTo (UTF-16 code unit
	// order), which differs from the Go byte order for names outside the BMP.
	sort.Slice(lines, func(i, j int) bool { return javaStringLess(lines[i], lines[j]) })
	var builder strings.Builder
	for _, line := range lines {
		builder.WriteString(line)
		builder.WriteString("\n")
	}
	sum := sha1.Sum([]byte(builder.String()))
	return hex.EncodeToString(sum[:]), nil
}

// javaStringLess compares two strings the way Java String#compareTo does: by
// UTF-16 code unit, so a manifest stays identical to the Java agent for file
// names outside the BMP (emoji, rare CJK extensions).
func javaStringLess(left, right string) bool {
	leftUnits := utf16.Encode([]rune(left))
	rightUnits := utf16.Encode([]rune(right))
	for index := 0; index < len(leftUnits) && index < len(rightUnits); index++ {
		if leftUnits[index] != rightUnits[index] {
			return leftUnits[index] < rightUnits[index]
		}
	}
	return len(leftUnits) < len(rightUnits)
}

// clearOldVersion removes the versions beyond the keep count, always keeping the
// active version and keepAliveID (ProjectFileVersionService#clearOldVersion).
func (s *Service) clearOldVersion(root, keepAliveID string, versionCount int) {
	if versionCount <= 0 {
		// 未启用（或调用方没有指定保留个数）时不清理，避免把历史版本误删成只剩当前版本
		return
	}
	count := versionCount
	metas := s.listVersionMeta(root)
	current := s.readVersionCurrent(root)
	for index := count; index < len(metas); index++ {
		versionID := metas[index].VersionID
		if versionID == "" || versionID == keepAliveID {
			continue
		}
		if current != nil && versionID == *current {
			continue
		}
		if target, err := secureJoin(root, versionID); err == nil {
			_ = os.RemoveAll(target)
		}
		if metaPath, err := secureJoin(root, versionID+versionMetaSuffix); err == nil {
			_ = os.RemoveAll(metaPath)
		}
	}
}

// listVersionMeta lists every version meta (newest first) and fills the computed
// versionDir/exist fields, like ProjectFileVersionService#listMeta.
func (s *Service) listVersionMeta(root string) []*VersionMeta {
	result := make([]*VersionMeta, 0, 8)
	entries, err := os.ReadDir(root)
	if err != nil {
		return result
	}
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(entry.Name(), versionMetaSuffix) {
			continue
		}
		metaPath := filepath.Join(root, entry.Name())
		raw, readErr := os.ReadFile(metaPath)
		if readErr != nil {
			continue
		}
		meta := &VersionMeta{}
		if err := json.Unmarshal(raw, meta); err != nil {
			s.Log().Warn("parse project file version meta failed", "path", metaPath, "error", err)
			continue
		}
		dir, dirErr := secureJoin(root, meta.VersionID)
		exist := false
		if dirErr != nil {
			dir = ""
		} else {
			dir = absolutePath(dir)
			exist = isDir(dir)
		}
		meta.VersionDir = dir
		meta.Exist = &exist
		result = append(result, meta)
	}
	sort.SliceStable(result, func(i, j int) bool {
		return result[i].CreateTime > result[j].CreateTime
	})
	return result
}

// readVersionMeta reads one meta file, a missing or unparsable file yields nil
// like ProjectFileVersionService#readMeta.
func (s *Service) readVersionMeta(root, versionID string) *VersionMeta {
	if strings.TrimSpace(versionID) == "" {
		return nil
	}
	metaPath, err := secureJoin(root, versionID+versionMetaSuffix)
	if err != nil {
		return nil
	}
	raw, readErr := os.ReadFile(metaPath)
	if readErr != nil {
		return nil
	}
	meta := &VersionMeta{}
	if err := json.Unmarshal(raw, meta); err != nil {
		return nil
	}
	return meta
}

// writeVersionMeta persists one meta file next to the version directory.
func (s *Service) writeVersionMeta(root string, meta *VersionMeta) error {
	if err := os.MkdirAll(root, 0o755); err != nil {
		return err
	}
	metaPath, err := secureJoin(root, meta.VersionID+versionMetaSuffix)
	if err != nil {
		return err
	}
	raw, err := json.Marshal(meta)
	if err != nil {
		return err
	}
	return os.WriteFile(metaPath, raw, 0o644)
}

// readVersionCurrent reads current.txt (trimmed), nil when absent or blank.
func (s *Service) readVersionCurrent(root string) *string {
	raw, err := os.ReadFile(filepath.Join(root, versionCurrentFile))
	if err != nil {
		return nil
	}
	value := strings.TrimSpace(string(raw))
	if value == "" {
		return nil
	}
	return &value
}

// writeVersionCurrent stores the active version id.
func (s *Service) writeVersionCurrent(root, versionID string) error {
	if err := os.MkdirAll(root, 0o755); err != nil {
		return err
	}
	return os.WriteFile(filepath.Join(root, versionCurrentFile), []byte(versionID), 0o644)
}

// dirHasFile reports whether dir contains at least one regular file, the Go
// equivalent of !FileUtil.isDirEmpty (which counts files only).
func dirHasFile(dir string) bool {
	found := false
	_ = filepath.WalkDir(dir, func(path string, entry os.DirEntry, walkErr error) error {
		if walkErr != nil {
			return nil
		}
		if entry.IsDir() {
			return nil
		}
		found = true
		return filepath.SkipAll
	})
	return found
}

// absolutePath returns the absolute form of path, falling back to path itself.
func absolutePath(path string) string {
	absolute, err := filepath.Abs(path)
	if err != nil {
		return path
	}
	return absolute
}
