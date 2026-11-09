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
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"jpom.io/go-agent/internal/project"
)

func backupProject(t *testing.T, service *Service, base string) string {
	t.Helper()
	lib := createProject(t, service, base, "P1", "proj", map[string]any{
		"dslContent": "description: demo\nfile:\n  backupCount: 2\n  backupSuffix: ['.jar']\n  diffBackup: false\n",
	})
	writeFile(t, filepath.Join(lib, "app.jar"), "first")
	writeFile(t, filepath.Join(lib, "readme.txt"), "readme")
	writeFile(t, filepath.Join(lib, "sub", "lib.jar"), "lib")
	return lib
}

func TestBackupDiffAndList(t *testing.T) {
	service, base := testService(t)
	lib := backupProject(t, service, base)
	info, err := service.Project("P1")
	if err != nil {
		t.Fatal(err)
	}
	settings, err := service.backupSettings(info)
	if err != nil {
		t.Fatal(err)
	}
	if settings.Count != 2 || len(settings.Suffix) != 1 || settings.Suffix[0] != ".jar" || settings.DiffBackup {
		t.Fatalf("unexpected DSL backup settings: %+v", settings)
	}
	backupID, err := service.Backup(info)
	if err != nil || backupID == "" {
		t.Fatalf("backup: %q %v", backupID, err)
	}
	root := service.backupRoot(settings)
	itemPath := filepath.Join(root, backupID)
	for _, name := range []string{"app.jar", "readme.txt", filepath.Join("sub", "lib.jar")} {
		if !exists(filepath.Join(itemPath, name)) {
			t.Fatalf("backup snapshot missing %s", name)
		}
	}

	// Suffix filter must drop readme.txt but keep the jars.
	if err := service.checkDiff(info, backupID); err != nil {
		t.Fatalf("check diff: %v", err)
	}
	if exists(filepath.Join(itemPath, "readme.txt")) {
		t.Fatal("readme.txt should be filtered out by backupSuffix")
	}
	if !exists(filepath.Join(itemPath, "app.jar")) || !exists(filepath.Join(itemPath, "sub", "lib.jar")) {
		t.Fatal("jar files should be kept")
	}

	list, err := service.ListBackup("P1")
	if err != nil || list == nil {
		t.Fatalf("list backup: %+v %v", list, err)
	}
	if len(list.List) != 1 || list.List[0].Filename != backupID {
		t.Fatalf("unexpected backup list: %+v", list.List)
	}
	if !filepath.IsAbs(list.Path) {
		t.Fatalf("backup path must be absolute: %s", list.Path)
	}

	files, err := service.BackupItemFiles("P1", "", backupID)
	if err != nil {
		t.Fatalf("backup item files: %v", err)
	}
	if len(files) != 2 {
		t.Fatalf("expected app.jar and sub, got %+v", files)
	}
	for _, item := range files {
		if item.LevelName != "" {
			t.Fatalf("backup item levelName should be empty: %+v", item)
		}
	}

	// File restore
	writeFile(t, filepath.Join(lib, "app.jar"), "changed")
	if err := service.BackupRecover("P1", backupID, "", "", "app.jar"); err != nil {
		t.Fatalf("recover file: %v", err)
	}
	content, _ := os.ReadFile(filepath.Join(lib, "app.jar"))
	if string(content) != "first" {
		t.Fatalf("file not restored: %q", content)
	}

	// Directory restore with clear
	writeFile(t, filepath.Join(lib, "sub", "extra.txt"), "extra")
	if err := service.BackupRecover("P1", backupID, "clear", "sub", ""); err != nil {
		t.Fatalf("recover directory: %v", err)
	}
	if exists(filepath.Join(lib, "sub", "extra.txt")) {
		t.Fatal("clear directory restore should remove extra files")
	}
	if !exists(filepath.Join(lib, "sub", "lib.jar")) {
		t.Fatal("directory restore should restore lib.jar")
	}

	// Delete one backup file
	if err := service.BackupDelete("P1", backupID, "sub", "lib.jar"); err != nil {
		t.Fatalf("delete backup file: %v", err)
	}
	if exists(filepath.Join(itemPath, "sub", "lib.jar")) {
		t.Fatal("backup file should be deleted")
	}

	// live Java 2.11.13: @ValidatorItem(msg=备份id缺失) -> code 400 备份id缺失.
	if _, err := service.BackupItemFiles("P1", "", ""); err == nil || HTTPCodeOf(err) != 400 ||
		!strings.Contains(err.Error(), "备份id缺失") {
		t.Fatalf("missing backupId must be a 400 备份id缺失 error, got %v", err)
	}
}

func TestClearOldBackupKeepsNewest(t *testing.T) {
	service, _ := testService(t)
	root := filepath.Join(t.TempDir(), "backups")
	if err := os.MkdirAll(root, 0o755); err != nil {
		t.Fatal(err)
	}
	now := time.Now()
	for index, name := range []string{"oldest", "older", "newer", "newest"} {
		path := filepath.Join(root, name)
		if err := os.MkdirAll(path, 0o755); err != nil {
			t.Fatal(err)
		}
		stamp := now.Add(time.Duration(index-4) * time.Hour)
		if err := os.Chtimes(path, stamp, stamp); err != nil {
			t.Fatal(err)
		}
	}
	service.clearOldBackup(root, 2)
	for _, name := range []string{"oldest", "older"} {
		if exists(filepath.Join(root, name)) {
			t.Fatalf("old backup %s should be pruned", name)
		}
	}
	for _, name := range []string{"newer", "newest"} {
		if !exists(filepath.Join(root, name)) {
			t.Fatalf("recent backup %s should be kept", name)
		}
	}
}

func TestMergeBackupPathFromLegacyDirectory(t *testing.T) {
	service, base := testService(t)
	customPath := filepath.Join(base, "custom-backups")
	lib := createProject(t, service, base, "P1", "proj", map[string]any{
		"dslContent": "file:\n  backupPath: " + customPath + "\n  backupCount: 1\n",
	})
	writeFile(t, filepath.Join(lib, "app.jar"), "first")
	legacy := service.defaultBackupRoot("P1")
	writeFile(t, filepath.Join(legacy, "20260101000000001", "app.jar"), "legacy")
	info, err := service.Project("P1")
	if err != nil {
		t.Fatal(err)
	}
	if err := service.MergeBackupPath(info); err != nil {
		t.Fatalf("merge backup path: %v", err)
	}
	if !exists(filepath.Join(customPath, "P1", "20260101000000001", "app.jar")) {
		t.Fatal("legacy backup directory should be moved to the configured path")
	}
	if exists(legacy) {
		t.Fatal("legacy backup directory should be removed after merge")
	}
	if _, err := project.ReadWhitelist(base); err != nil {
		t.Fatalf("whitelist should stay readable: %v", err)
	}
}
func TestMatchBackupSuffixDefaults(t *testing.T) {
	// Default agent configuration (jpom.project.file-backup-suffix).
	patterns := []string{".jar", ".html", `^.+\.(?i)(txt)$`}
	cases := map[string]bool{
		"config.txt": true,
		"a.TXT":      true,
		"e2eapp.jar": true,
		"index.html": true,
		"run.sh":     false,
		"app.log":    false,
	}
	for name, want := range cases {
		if got := matchBackupSuffix(patterns, name); got != want {
			t.Fatalf("matchBackupSuffix(%q) = %v, want %v", name, got, want)
		}
	}
}

func TestBackupMatchesE2EScenario(t *testing.T) {
	service, base := testService(t)
	// file-backup-count=3 in conf/application.yml (E2E configuration).
	service.app.Config.Jpom.Project.FileBackupCount = 3
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeFile(t, filepath.Join(lib, "config.txt"), "hello e2e\n")
	writeFile(t, filepath.Join(lib, "run.sh"), "#!/bin/sh\n")
	writeFile(t, filepath.Join(lib, "e2eapp.jar"), "jar")
	writeFile(t, filepath.Join(lib, "sub", "x.txt"), "x")

	info, err := service.Project("P1")
	if err != nil {
		t.Fatal(err)
	}
	settings, err := service.backupSettings(info)
	if err != nil {
		t.Fatal(err)
	}
	if settings.Count != 3 {
		t.Fatalf("file-backup-count should be read from the config, got %d", settings.Count)
	}

	// update_config_file: backup first, then the file content changes.
	backupID, err := service.Backup(info)
	if err != nil || backupID == "" {
		t.Fatalf("backup: %q %v", backupID, err)
	}
	writeFile(t, filepath.Join(lib, "config.txt"), "hello e2e updated\n")
	if err := service.checkDiff(info, backupID); err != nil {
		t.Fatalf("check diff: %v", err)
	}

	itemPath := filepath.Join(service.backupRoot(settings), backupID)
	if !exists(filepath.Join(itemPath, "config.txt")) {
		t.Fatalf("changed config.txt must be kept in the snapshot, tree=%v", snapshotNames(t, itemPath))
	}
	list, err := service.ListBackup("P1")
	if err != nil || list == nil || len(list.List) == 0 {
		t.Fatalf("list-backup must not be empty after file write: %+v %v", list, err)
	}
	files, err := service.BackupItemFiles("P1", "", backupID)
	if err != nil || len(files) == 0 {
		t.Fatalf("backup-item-files must return the kept file: %+v %v", files, err)
	}

	// backup-recover restores the old content.
	writeFile(t, filepath.Join(lib, "config.txt"), "corrupted")
	if err := service.BackupRecover("P1", backupID, "", "", "config.txt"); err != nil {
		t.Fatalf("backup recover: %v", err)
	}
	if raw, err := os.ReadFile(filepath.Join(lib, "config.txt")); err != nil || string(raw) != "hello e2e\n" {
		t.Fatalf("recovered content: %q %v", raw, err)
	}
}

// snapshotNames lists the relative file names inside a snapshot for failure output.
func snapshotNames(t *testing.T, root string) []string {
	t.Helper()
	var names []string
	_ = filepath.WalkDir(root, func(path string, entry os.DirEntry, err error) error {
		if err != nil || entry.IsDir() {
			return nil
		}
		rel, relErr := filepath.Rel(root, path)
		if relErr == nil {
			names = append(names, rel)
		}
		return nil
	})
	return names
}
func TestBackupDiffKeySemantics(t *testing.T) {
	service, base := testService(t)
	// Java listFiles keys files by sha1 + "-" + relative path, so a file whose
	// path is unchanged but whose content changed must stay in the snapshot,
	// while unchanged files (same sha1 + path) are removed.
	service.app.Config.Jpom.Project.FileBackupCount = 3
	service.app.Config.Jpom.Project.FileBackupSuffix = []string{".jar"}
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeFile(t, filepath.Join(lib, "changed.jar"), "one")
	writeFile(t, filepath.Join(lib, "same.jar"), "same")

	info, err := service.Project("P1")
	if err != nil {
		t.Fatal(err)
	}
	backupID, err := service.Backup(info)
	if err != nil || backupID == "" {
		t.Fatalf("backup: %q %v", backupID, err)
	}
	writeFile(t, filepath.Join(lib, "changed.jar"), "two")
	if err := service.checkDiff(info, backupID); err != nil {
		t.Fatalf("checkDiff: %v", err)
	}
	settings, err := service.backupSettings(info)
	if err != nil {
		t.Fatal(err)
	}
	itemPath := filepath.Join(service.backupRoot(settings), backupID)
	if !exists(filepath.Join(itemPath, "changed.jar")) {
		t.Fatalf("changed file must be kept, tree=%v", snapshotNames(t, itemPath))
	}
	if exists(filepath.Join(itemPath, "same.jar")) {
		t.Fatalf("unchanged file must be removed, tree=%v", snapshotNames(t, itemPath))
	}
}

func TestBackupSuffixFromJavaStyleConfig(t *testing.T) {
	// The production application.yml uses single quotes:
	//   file-backup-suffix: [ '.jar','.html','^.+\.(?i)(txt)$' ]
	// YAML keeps both backslashes, so this regex does NOT match *.txt (the same
	// is true for the Java agent). Only the plain ".jar"/".html" suffixes match.
	service, base := testService(t)
	service.app.Config.Jpom.Project.FileBackupCount = 3
	service.app.Config.Jpom.Project.FileBackupSuffix = []string{".jar", ".html", `^.+.\\.(?i)(txt)$`}
	if matchBackupSuffix(service.app.Config.Jpom.Project.FileBackupSuffix, "config.txt") {
		t.Fatal("java style double-escaped regex must not match txt (parity with Java)")
	}
	if !matchBackupSuffix(service.app.Config.Jpom.Project.FileBackupSuffix, "e2eapp.jar") {
		t.Fatal(".jar suffix must match")
	}
	_ = base
}
