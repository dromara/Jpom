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
	"archive/zip"
	"bytes"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestReadAndUpdateConfigFile(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeFile(t, filepath.Join(lib, "app.yml"), "name: old")

	content, err := service.ReadFile("P1", "", "app.yml")
	if err != nil || content != "name: old" {
		t.Fatalf("read file: %q %v", content, err)
	}
	if err := service.UpdateConfigFile("P1", "", "app.yml", "name: new"); err != nil {
		t.Fatalf("update file: %v", err)
	}
	content, err = service.ReadFile("P1", "", "app.yml")
	if err != nil || content != "name: new" {
		t.Fatalf("read updated file: %q %v", content, err)
	}
	// Disallowed suffix and traversal must both be validation errors.
	if _, err := service.ReadFile("P1", "", "app.bin"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("disallowed suffix should be 405: %v", err)
	}
	if err := service.UpdateConfigFile("P1", "../../", "app.yml", "x"); err == nil {
		t.Fatal("expected traversal to be rejected")
	}
}

func TestCopyCompressNewFolderRenameDelete(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeFile(t, filepath.Join(lib, "app.txt"), "hello")
	writeFile(t, filepath.Join(lib, "cfg", "a.yml"), "a: 1")

	if err := service.Copy("P1", "", "app.txt"); err != nil {
		t.Fatalf("copy file: %v", err)
	}
	if err := service.Copy("P1", "", "app.txt"); err != nil {
		t.Fatalf("copy file twice: %v", err)
	}
	if !exists(filepath.Join(lib, "app(1).txt")) || !exists(filepath.Join(lib, "app(2).txt")) {
		t.Fatal("copied files should use (N) suffix")
	}

	if err := service.CompressFileFolder("P1", "", "cfg", "zip"); err != nil {
		t.Fatalf("compress directory: %v", err)
	}
	if !exists(filepath.Join(lib, "cfg.zip")) {
		t.Fatal("cfg.zip should be created")
	}
	if err := service.CompressFileFolder("P1", "", "cfg", "zip"); err != nil {
		t.Fatalf("compress directory again: %v", err)
	}
	if !exists(filepath.Join(lib, "cfg(1).zip")) {
		t.Fatal("second archive should use (1) suffix")
	}
	if err := service.CompressFileFolder("P1", "", "app.txt", "zip"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("compressing a file should be a 405: %v", err)
	}
	if err := service.CompressFileFolder("P1", "", "cfg", "rar"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("unsupported archive type should be a 405: %v", err)
	}

	// unFolder semantics: unset/false creates a directory, true creates a file.
	if err := service.NewFileFolder("P1", "", "dir1", ""); err != nil {
		t.Fatalf("new folder: %v", err)
	}
	if !isDir(filepath.Join(lib, "dir1")) {
		t.Fatal("dir1 should be a directory")
	}
	if err := service.NewFileFolder("P1", "", "file1", "true"); err != nil {
		t.Fatalf("new file: %v", err)
	}
	if isDir(filepath.Join(lib, "file1")) || !exists(filepath.Join(lib, "file1")) {
		t.Fatal("file1 should be a file")
	}
	if err := service.NewFileFolder("P1", "", "file1", "true"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("existing path should be a 405: %v", err)
	}

	if err := service.Rename("P1", "", "app.txt", "main.txt"); err != nil {
		t.Fatalf("rename: %v", err)
	}
	if !exists(filepath.Join(lib, "main.txt")) || exists(filepath.Join(lib, "app.txt")) {
		t.Fatal("rename should move the file")
	}
	if err := service.Rename("P1", "", "missing.txt", "x.txt"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing source should be a 405: %v", err)
	}

	if _, err := service.DeleteFile("P1", DeleteOptions{Type: "clear", LevelName: "cfg"}); err != nil {
		t.Fatalf("clear directory: %v", err)
	}
	if entries, _ := os.ReadDir(filepath.Join(lib, "cfg")); len(entries) != 0 {
		t.Fatal("clear should remove directory content")
	}
	message, err := service.DeleteFile("P1", DeleteOptions{Filename: "main.txt"})
	if err != nil || message.Key != "i18n.delete_success.0007" {
		t.Fatalf("delete file: %+v %v", message, err)
	}
	if exists(filepath.Join(lib, "main.txt")) {
		t.Fatal("file should be deleted")
	}
}

// buildZip returns an in-memory zip archive.
func buildZip(t *testing.T, entries map[string]string) []byte {
	t.Helper()
	var buffer bytes.Buffer
	writer := zip.NewWriter(&buffer)
	for name, content := range entries {
		entry, err := writer.Create(name)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := entry.Write([]byte(content)); err != nil {
			t.Fatal(err)
		}
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	return buffer.Bytes()
}

func TestRemoteDownload(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	archive := buildZip(t, map[string]string{"bundle/x.txt": "x", "bundle/y.txt": "y"})
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/files/report.txt":
			w.Header().Set("Content-Disposition", "attachment; filename=\"report.txt\"")
			_, _ = w.Write([]byte("report"))
		case "/files/bundle.zip":
			_, _ = w.Write(archive)
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()

	message, err := service.RemoteDownload("P1", server.URL+"/files/report.txt", "", "false", 0)
	if err != nil || message.Key != "i18n.download_file_size.d4de" || message.Suffix == "" {
		t.Fatalf("remote download: %+v %v", message, err)
	}
	content, err := os.ReadFile(filepath.Join(lib, "report.txt"))
	if err != nil || string(content) != "report" {
		t.Fatalf("remote file content: %q %v", content, err)
	}

	if _, err := service.RemoteDownload("P1", server.URL+"/files/bundle.zip", "", "true", 0); err != nil {
		t.Fatalf("remote download unzip: %v", err)
	}
	if !exists(filepath.Join(lib, "bundle", "x.txt")) || !exists(filepath.Join(lib, "bundle", "y.txt")) {
		t.Fatal("remote archive should be extracted")
	}
	if _, err := service.RemoteDownload("P1", "ftp://example.com/a.txt", "", "false", 0); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("invalid scheme should be a 405: %v", err)
	}
}

// isFastUUIDHex reports whether value looks like Hutool IdUtil.fastSimpleUUID()
// (32 lowercase hex characters, no dashes).
func isFastUUIDHex(value string) bool {
	if len(value) != 32 {
		return false
	}
	for index := 0; index < len(value); index++ {
		char := value[index]
		if (char >= '0' && char <= '9') || (char >= 'a' && char <= 'f') {
			continue
		}
		return false
	}
	return true
}

// findFastUUIDFile returns the single 32-hex file inside dir (unix + windows
// style), or "" when not found.
func findFastUUIDFile(t *testing.T, dir string) string {
	t.Helper()
	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatalf("read dir %s: %v", dir, err)
	}
	for _, entry := range entries {
		if !entry.IsDir() && isFastUUIDHex(entry.Name()) {
			return filepath.Join(dir, entry.Name())
		}
	}
	return ""
}

// TestResolveRemoteFileName 校验 remote_download 落盘文件名解析：
// URL basename（不读 Content-Disposition）；空/非法名回退 32 位随机名称。
func TestResolveRemoteFileName(t *testing.T) {
	cases := map[string]string{
		"https://example.com/path/report.txt":   "report.txt",
		"https://example.com/a/file%20name.jar": "file name.jar",
		"https://example.com/a/中文报告.zip":        "中文报告.zip",
	}
	for rawURL, want := range cases {
		if got := resolveRemoteFileName(rawURL); got != want {
			t.Errorf("resolveRemoteFileName(%q) = %q, want %q", rawURL, got, want)
		}
	}
	invalid := []string{
		"",
		"not-a-url",
		"ftp://example.com/a.txt",
		"https://example.com",
		"https://example.com/",
		"https://example.com/dir/",
		"https://example.com/a/..",
		"https://example.com/a/bad:name.txt",
		"https://example.com/a/bad*.txt",
		"https://example.com/a/trailing.",
	}
	for _, rawURL := range invalid {
		if got := resolveRemoteFileName(rawURL); !isFastUUIDHex(got) {
			t.Errorf("resolveRemoteFileName(%q) = %q, want 32 hex uuid", rawURL, got)
		}
	}
}

// TestRemoteDownloadUsesURLBasename 校验最终文件名取 URL basename（忽略
// Content-Disposition），非法 basename 回退 32 位随机名称，同名目录冲突也回退。
func TestRemoteDownloadUsesURLBasename(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/files/cd-name.bin":
			w.Header().Set("Content-Disposition", "attachment; filename=\"from-header.bin\"")
			_, _ = w.Write([]byte("body"))
		case "/":
			_, _ = w.Write([]byte("root"))
		case "/files/report.txt":
			_, _ = w.Write([]byte("report"))
		default:
			http.NotFound(w, r)
		}
	}))
	defer server.Close()

	if _, err := service.RemoteDownload("P1", server.URL+"/files/cd-name.bin", "", "false", 0); err != nil {
		t.Fatalf("remote download: %v", err)
	}
	if content, err := os.ReadFile(filepath.Join(lib, "cd-name.bin")); err != nil || string(content) != "body" {
		t.Fatalf("URL basename file missing: %q %v", content, err)
	}
	if exists(filepath.Join(lib, "from-header.bin")) {
		t.Fatal("Content-Disposition filename must not be used")
	}

	// 非法 basename（URL path 为 /）-> 32 位随机名称。
	if _, err := service.RemoteDownload("P1", server.URL+"/", "", "false", 0); err != nil {
		t.Fatalf("remote download fallback: %v", err)
	}
	fallbackFile := findFastUUIDFile(t, lib)
	if fallbackFile == "" {
		t.Fatalf("invalid basename should fall back to 32 hex uuid, files=%v", listNames(t, lib))
	}
	if content, err := os.ReadFile(fallbackFile); err != nil || string(content) != "root" {
		t.Fatalf("fallback file content: %q %v", content, err)
	}

	// 同名目录已存在时使用随机名称（Java: targetFile.isDirectory()）。
	lib2 := createProject(t, service, base, "P2", "proj2", nil)
	if err := os.MkdirAll(filepath.Join(lib2, "report.txt"), 0o755); err != nil {
		t.Fatal(err)
	}
	if _, err := service.RemoteDownload("P2", server.URL+"/files/report.txt", "", "false", 0); err != nil {
		t.Fatalf("remote download into collision dir: %v", err)
	}
	if stat, err := os.Stat(filepath.Join(lib2, "report.txt")); err != nil || !stat.IsDir() {
		t.Fatalf("existing directory must be kept: %v", err)
	}
	collisionFile := findFastUUIDFile(t, lib2)
	if collisionFile == "" {
		t.Fatalf("directory collision should fall back to 32 hex uuid, files=%v", listNames(t, lib2))
	}
	if content, err := os.ReadFile(collisionFile); err != nil || string(content) != "report" {
		t.Fatalf("collision fallback content: %q %v", content, err)
	}
}

// listNames returns the entry names of dir for diagnostics.
func listNames(t *testing.T, dir string) []string {
	t.Helper()
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil
	}
	names := make([]string, 0, len(entries))
	for _, entry := range entries {
		names = append(names, entry.Name())
	}
	return names
}

func TestUploadProjectFileAndHooks(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	previous := Hooks
	stopped, restarted, webhooked := false, false, false
	Hooks.StopProject = func(info map[string]any) error { stopped = true; return nil }
	Hooks.RestartProject = func(info map[string]any) (bool, string, any) {
		restarted = true
		return true, "ok", map[string]any{"success": true}
	}
	Hooks.Webhook = func(info map[string]any, eventType string, values map[string]any) {
		webhooked = true
	}
	t.Cleanup(func() { Hooks = previous })

	merged := filepath.Join(t.TempDir(), "note.txt")
	writeFile(t, merged, "note")
	result, err := service.UploadProjectFile("P1", merged, UploadOptions{
		LevelName:  "deploy",
		CloseFirst: true,
		After:      "restart",
	})
	if err != nil {
		t.Fatalf("upload project file: %v", err)
	}
	if !stopped || !restarted || !webhooked {
		t.Fatalf("hooks not called: stop=%v restart=%v webhook=%v", stopped, restarted, webhooked)
	}
	if !result.Restarted || !result.Success {
		t.Fatalf("unexpected upload result: %+v", result)
	}
	if !exists(filepath.Join(lib, "deploy", "note.txt")) {
		t.Fatal("uploaded file should be moved into the destination")
	}

	// unzip upload
	archive := buildZip(t, map[string]string{"pkg/a.txt": "a", "pkg/b.txt": "b"})
	zipPath := filepath.Join(t.TempDir(), "pkg.zip")
	writeFile(t, zipPath, string(archive))
	if _, err := service.UploadProjectFile("P1", zipPath, UploadOptions{Type: "unzip"}); err != nil {
		t.Fatalf("upload unzip: %v", err)
	}
	if !exists(filepath.Join(lib, "pkg", "a.txt")) {
		t.Fatal("unzip upload should extract into the project")
	}
	if exists(zipPath) {
		t.Fatal("merged temporary file should be deleted after extraction")
	}

	// file2 upload to an explicit directory
	targetDir := filepath.Join(base, "release-dir")
	merged2 := filepath.Join(t.TempDir(), "release.bin")
	writeFile(t, merged2, "bin")
	if err := service.UploadToPath(targetDir, merged2, "", 0); err != nil {
		t.Fatalf("upload to path: %v", err)
	}
	if !exists(filepath.Join(targetDir, "release.bin")) {
		t.Fatal("file2 upload should move the file into the destination directory")
	}
	if err := service.UploadToPath("", merged2, "", 0); err == nil || HTTPCodeOf(err) != 400 {
		t.Fatalf("missing path should be a 400 (@ValidatorItem): %v", err)
	}
}

func TestDownloadPathValidation(t *testing.T) {
	service, base := testService(t)
	createProject(t, service, base, "P1", "proj", nil)
	if _, _, err := service.DownloadPath("P1", "", ""); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing filename should be a 405: %v", err)
	}
	if _, _, err := service.DownloadPath("P1", "", "../secret"); err == nil {
		t.Fatal("expected traversal to be rejected")
	}
	if _, err := service.Project("missing"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing project should be a 405: %v", err)
	}
}

func TestReadFileMissingMessageAlignedWithJava(t *testing.T) {
	service, base := testService(t)
	createProject(t, service, base, "P1", "proj", nil)
	_, err := service.ReadFile("P1", "", "missing.txt")
	if err == nil {
		t.Fatal("expected missing file error")
	}
	if HTTPCodeOf(err) != 500 {
		t.Fatalf("missing file must be a 500 like Java: %v", err)
	}
	if !strings.HasPrefix(err.Error(), "服务异常：File not exist: ") {
		t.Fatalf("unexpected message: %q", err.Error())
	}
}

func TestValidateUploadPathAndPreflight(t *testing.T) {
	service, base := testService(t)
	createProject(t, service, base, "P1", "proj", nil)
	for _, candidate := range []string{"", "relative/dir", "bad\x00path"} {
		// @ValidatorItem(msg=文件目录缺失) -> code 400 in the live Java agent.
		if _, err := ValidateUploadPath(candidate); err == nil || HTTPCodeOf(err) != 400 ||
			!strings.Contains(err.Error(), "文件目录缺失") {
			t.Fatalf("path %q must be a 400 文件目录缺失 validation error: %v", candidate, err)
		}
	}
	if abs, err := ValidateUploadPath("/tmp/some-dir"); err != nil || abs != "/tmp/some-dir" {
		t.Fatalf("absolute path should pass: %q %v", abs, err)
	}
	if err := service.PreflightProjectUpload("P1", ""); err != nil {
		t.Fatalf("valid project should pass preflight: %v", err)
	}
	if err := service.PreflightProjectUpload("", ""); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing id must be a 405: %v", err)
	}
	if err := service.PreflightProjectUpload("NOPE", ""); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing project must be a 405: %v", err)
	}
	if err := service.PreflightProjectUpload("P1", "../../"); err == nil {
		t.Fatalf("traversal levelName must be rejected: %v", err)
	}
}

func TestParameterValidationCodes(t *testing.T) {
	service, base := testService(t)
	createProject(t, service, base, "P1", "proj", nil)

	assertCode := func(name string, err error, code int, contains string) {
		t.Helper()
		if err == nil || HTTPCodeOf(err) != code {
			t.Fatalf("%s: want code %d, got %v", name, code, err)
		}
		if contains != "" && !strings.Contains(err.Error(), contains) {
			t.Fatalf("%s: want message containing %q, got %v", name, contains, err)
		}
	}

	// @ValidatorItem validations: code 400 with the annotation message.
	_, err := service.BackupDownloadPath("P1", "", "", "a.txt")
	assertCode("backup-download backupId", err, 400, "备份id缺失")
	_, err = service.BackupDownloadPath("P1", "20260101000000001", "", "")
	assertCode("backup-download filename", err, 400, "参数验证失败")
	assertCode("backup-delete backupId", service.BackupDelete("P1", "", "", "a.txt"), 400, "备份id缺失")
	assertCode("backup-delete filename", service.BackupDelete("P1", "20260101000000001", "", ""), 400, "参数验证失败")
	assertCode("backup-recover backupId", service.BackupRecover("P1", "", "", "", ""), 400, "参数验证失败")
	assertCode("new-file-folder filename", service.NewFileFolder("P1", "", "", ""), 400, "参数验证失败")
	assertCode("rename filename", service.Rename("P1", "", "", "x"), 400, "参数验证失败")
	_, err = ValidateUploadPath("")
	assertCode("file2 path", err, 400, "文件目录缺失")

	// backup-item-files: @ValidatorItem(msg=备份id缺失) -> 400 before project lookup.
	_, err = service.BackupItemFiles("P1", "", "")
	assertCode("backup-item-files backupId", err, 400, "备份id缺失")
	// Missing project id alone still validates the annotated backupId first.
	_, err = service.BackupItemFiles("NOPE", "", "")
	assertCode("backup-item-files backupId precedence", err, 400, "备份id缺失")

	// Business Assert validations stay 405.
	assertCode("missing project", service.NewFileFolder("NOPE", "", "x", ""), 405, "获取项目信息失败")
	_, _, err = service.DownloadPath("P1", "", "")
	assertCode("download filename", err, 405, "请选择文件")
	if _, err := service.MergeSharding("", "", "1"); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("sharding validation must stay 405, got %v", err)
	}
}

func TestJavaStyleErrorPaths(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)

	// Java: FileUtil.file(lib, "/", filename) keeps a double slash.
	if got, want := javaFilePath(lib, "", "nope.txt"), lib+"//nope.txt"; got != want {
		t.Fatalf("root path: want %q got %q", want, got)
	}
	if got, want := javaFilePath(lib, "sub", "nope.txt"), lib+"/sub/nope.txt"; got != want {
		t.Fatalf("sub path: want %q got %q", want, got)
	}

	// read_file missing file: the Java message contains lib//filename.
	_, err := service.ReadFile("P1", "", "nope.txt")
	if err == nil || !strings.Contains(err.Error(), lib+"//nope.txt") {
		t.Fatalf("read_file message should contain the Java style path: %v", err)
	}

	// download missing file: displayPath is the Java style path used in the data field.
	_, display, err := service.DownloadPath("P1", "", "nope.txt")
	if err != nil {
		t.Fatalf("download path resolution: %v", err)
	}
	if want := lib + "//nope.txt"; display != want {
		t.Fatalf("download display path: want %q got %q", want, display)
	}
}
