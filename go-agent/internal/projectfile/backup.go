/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package projectfile

import (
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"

	"jpom.io/go-agent/internal/project"
)

// BackupList is the response of /manage/file/list-backup: the absolute backup
// directory plus the existing backup snapshots (newest first).
type BackupList struct {
	Path string     `json:"path"`
	List []FileInfo `json:"list"`
}

// newBackupID returns a snapshot id with the Java DatePattern.PURE_DATETIME_MS_FORMAT
// (yyyyMMddHHmmssSSS).
func newBackupID() string {
	now := time.Now()
	return now.Format("20060102150405") + fmt.Sprintf("%03d", now.Nanosecond()/int(time.Millisecond))
}

// Backup copies the current project directory into a new snapshot and returns
// the backup id. An empty id means backups are disabled or the project directory
// does not exist (ProjectFileBackupService#backup).
func (s *Service) Backup(info map[string]any) (string, error) {
	settings, err := s.backupSettings(info)
	if err != nil {
		return "", err
	}
	if settings.Count <= 0 {
		return "", nil
	}
	lib, err := s.LibPath(info)
	if err != nil {
		return "", err
	}
	if !exists(lib) {
		return "", nil
	}
	backupID := newBackupID()
	itemPath := filepath.Join(s.backupRoot(settings), backupID)
	if exists(itemPath) {
		return "", invalidf("备份目录冲突：%s", backupID)
	}
	if err := copyContent(lib, itemPath, true); err != nil {
		return "", fmt.Errorf("备份项目文件失败:%w", err)
	}
	return backupID, nil
}

// CheckDiff schedules the asynchronous backup comparison that runs after every
// file change, mirroring ProjectFileBackupService#checkDiff.
func (s *Service) CheckDiff(info map[string]any, backupID string) {
	if strings.TrimSpace(backupID) == "" {
		return
	}
	s.async("file-backup-diff", func() {
		if err := s.checkDiff(info, backupID); err != nil {
			s.Log().Warn("project file backup diff failed", "backupId", backupID, "error", err)
		}
	})
}

// checkDiff implements the diff backup: unchanged files are removed from the
// snapshot, optional suffix filtering is applied, empty directories are removed,
// old snapshots are pruned and the legacy backup directory is merged.
func (s *Service) checkDiff(info map[string]any, backupID string) error {
	settings, err := s.backupSettings(info)
	if err != nil {
		return err
	}
	root := s.backupRoot(settings)
	itemPath := filepath.Join(root, backupID)
	backupFiles, err := snapshotFiles(itemPath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}
	if settings.DiffBackup {
		lib, libErr := s.LibPath(info)
		if libErr != nil {
			return libErr
		}
		nowFiles, nowErr := snapshotFiles(lib)
		if nowErr == nil {
			for key, backupFile := range backupFiles {
				if _, present := nowFiles[key]; present {
					_ = os.RemoveAll(backupFile)
					delete(backupFiles, key)
				}
			}
		}
	}
	if len(settings.Suffix) > 0 {
		for key, file := range backupFiles {
			if !matchBackupSuffix(settings.Suffix, filepath.Base(file)) {
				_ = os.RemoveAll(file)
				delete(backupFiles, key)
			}
		}
	}
	loopCleanEmptyDir(itemPath)
	s.clearOldBackup(root, settings.Count)
	model, modelErr := s.ResolveModel(info)
	if modelErr != nil {
		return modelErr
	}
	return s.mergeBackupPath(info, project.String(model, "id"))
}

// snapshotFiles maps sha1+"-"+relativePath to the absolute file path, the same
// key ProjectFileBackupService#listFiles builds.
func snapshotFiles(root string) (map[string]string, error) {
	result := map[string]string{}
	err := filepath.WalkDir(root, func(path string, entry os.DirEntry, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		if entry.IsDir() {
			return nil
		}
		stat, statErr := os.Stat(path)
		if statErr != nil || stat.IsDir() {
			return nil
		}
		sha1Value, hashErr := fileSHA1(path)
		if hashErr != nil {
			return nil
		}
		relative, relErr := filepath.Rel(root, path)
		if relErr != nil {
			return nil
		}
		result[sha1Value+"-"+filepath.ToSlash(relative)] = path
		return nil
	})
	if err != nil {
		return nil, err
	}
	return result, nil
}

// matchBackupSuffix reports whether a file name is kept by the backup suffix
// configuration (regex or case sensitive suffix), mirroring checkDiff.
func matchBackupSuffix(patterns []string, name string) bool {
	for _, pattern := range patterns {
		pattern = strings.TrimSpace(pattern)
		if pattern == "" {
			continue
		}
		if strings.HasSuffix(name, pattern) {
			return true
		}
		expression, err := regexp.Compile("^(?:" + pattern + ")$")
		if err != nil {
			continue
		}
		if expression.MatchString(name) {
			return true
		}
	}
	return false
}

// clearOldBackup keeps only the newest count snapshot directories.
func (s *Service) clearOldBackup(root string, count int) {
	entries, err := os.ReadDir(root)
	if err != nil {
		return
	}
	directories := make([]os.DirEntry, 0, len(entries))
	for _, entry := range entries {
		if entry.IsDir() {
			directories = append(directories, entry)
		}
	}
	if len(directories) <= count {
		return
	}
	sort.SliceStable(directories, func(i, j int) bool {
		left, leftErr := directories[i].Info()
		right, rightErr := directories[j].Info()
		if leftErr != nil || rightErr != nil {
			return false
		}
		return left.ModTime().Before(right.ModTime())
	})
	for index := 0; index < len(directories)-count; index++ {
		_ = os.RemoveAll(filepath.Join(root, directories[index].Name()))
	}
}

// loopCleanEmptyDir removes empty directories below path.
func loopCleanEmptyDir(path string) {
	stat, err := os.Stat(path)
	if err != nil || !stat.IsDir() {
		return
	}
	entries, err := os.ReadDir(path)
	if err != nil {
		return
	}
	for _, entry := range entries {
		loopCleanEmptyDir(filepath.Join(path, entry.Name()))
	}
	if remaining, err := os.ReadDir(path); err == nil && len(remaining) == 0 {
		_ = os.Remove(path)
	}
}

// MergeBackupPath moves the legacy backup directory of a project to its current
// backup directory, mirroring ProjectFileBackupService#margeBackupPath.
func (s *Service) MergeBackupPath(info map[string]any) error {
	return s.mergeBackupPath(info, project.String(info, "id"))
}

// mergeBackupPath moves data/project_file_backup/<legacyID> into the effective
// backup directory when the two differ.
func (s *Service) mergeBackupPath(info map[string]any, legacyID string) error {
	settings, err := s.backupSettings(info)
	if err != nil {
		return err
	}
	root := s.backupRoot(settings)
	legacy := s.defaultBackupRoot(legacyID)
	if legacyID == "" || filepath.Clean(legacy) == filepath.Clean(root) {
		return nil
	}
	if !isDir(legacy) {
		return nil
	}
	if err := moveContent(legacy, root, true); err != nil {
		return err
	}
	return os.RemoveAll(legacy)
}

// backupItemPath returns the directory of one backup snapshot.
func (s *Service) backupItemPath(info map[string]any, backupID string) (string, error) {
	if strings.TrimSpace(backupID) == "" {
		return "", invalidf("备份id缺失")
	}
	settings, err := s.backupSettings(info)
	if err != nil {
		return "", err
	}
	return secureJoin(s.backupRoot(settings), backupID)
}

// ListBackup implements /manage/file/list-backup.
func (s *Service) ListBackup(id string) (*BackupList, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	_ = s.MergeBackupPath(info)
	settings, err := s.backupSettings(info)
	if err != nil {
		return nil, err
	}
	root := s.backupRoot(settings)
	entries, err := os.ReadDir(root)
	if err != nil {
		return nil, nil
	}
	directories := make([]os.DirEntry, 0, len(entries))
	for _, entry := range entries {
		if entry.IsDir() {
			directories = append(directories, entry)
		}
	}
	if len(directories) == 0 {
		return nil, nil
	}
	absolute, absErr := filepath.Abs(root)
	if absErr != nil {
		absolute = root
	}
	return &BackupList{
		Path: absolute,
		List: s.parseInfo(root, root, directories, true, DisableScanDir(info)),
	}, nil
}

// BackupItemFiles implements /manage/file/backup-item-files.
func (s *Service) BackupItemFiles(id, relativePath, backupID string) ([]FileInfo, error) {
	info, err := s.Project(id)
	if err != nil {
		return nil, err
	}
	root, err := s.backupItemPath(info, backupID)
	if err != nil {
		return nil, err
	}
	dir, err := secureJoin(root, relativePath)
	if err != nil {
		return nil, err
	}
	entries, err := os.ReadDir(dir)
	if err != nil {
		return []FileInfo{}, nil
	}
	return s.parseInfo(dir, root, entries, false, DisableScanDir(info)), nil
}

// BackupDownloadPath resolves a file of one backup snapshot for
// /manage/file/backup-download.
func (s *Service) BackupDownloadPath(id, backupID, levelName, filename string) (string, error) {
	if strings.TrimSpace(backupID) == "" {
		return "", invalidf("备份id缺失")
	}
	if strings.TrimSpace(filename) == "" {
		return "", invalidf("请选择文件")
	}
	info, err := s.Project(id)
	if err != nil {
		return "", err
	}
	root, err := s.backupItemPath(info, backupID)
	if err != nil {
		return "", err
	}
	return secureJoin(root, levelName, filename)
}

// BackupDelete implements /manage/file/backup-delete (best effort delete like
// CommandUtil#systemFastDel).
func (s *Service) BackupDelete(id, backupID, levelName, filename string) error {
	if strings.TrimSpace(backupID) == "" {
		return invalidf("备份id缺失")
	}
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	root, err := s.backupItemPath(info, backupID)
	if err != nil {
		return err
	}
	target, err := secureJoin(root, levelName, filename)
	if err != nil {
		return err
	}
	_ = os.RemoveAll(target)
	return nil
}

// BackupRecover restores files of a backup snapshot into the project,
// mirroring ProjectFileBackupController#recoverFile.
func (s *Service) BackupRecover(id, backupID, clearType, levelName, filename string) error {
	info, err := s.Project(id)
	if err != nil {
		return err
	}
	backupRoot, err := s.backupItemPath(info, backupID)
	if err != nil {
		return err
	}
	projectRoot, err := s.LibPath(info)
	if err != nil {
		return err
	}
	if strings.TrimSpace(filename) == "" {
		backupFile, err := secureJoin(backupRoot, levelName)
		if err != nil {
			return err
		}
		if !exists(backupFile) {
			return invalidf("对应的文件不存在")
		}
		projectFile, err := secureJoin(projectRoot, levelName)
		if err != nil {
			return err
		}
		if err := os.MkdirAll(projectFile, 0o755); err != nil {
			return err
		}
		if strings.EqualFold(strings.TrimSpace(clearType), "clear") {
			if err := cleanDir(projectFile); err != nil {
				return err
			}
		}
		if err := copyContent(backupFile, projectFile, true); err != nil {
			return fmt.Errorf("还原失败:%w", err)
		}
		return nil
	}
	backupFile, err := secureJoin(backupRoot, levelName, filename)
	if err != nil {
		return err
	}
	if !exists(backupFile) {
		return invalidf("对应的文件不存在")
	}
	projectFile, err := secureJoin(projectRoot, levelName, filename)
	if err != nil {
		return err
	}
	if err := copyFile(backupFile, projectFile, true); err != nil {
		return fmt.Errorf("还原失败:%w", err)
	}
	return nil
}
