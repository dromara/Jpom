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
	if err != nil || message != "删除成功" {
		t.Fatalf("delete file: %q %v", message, err)
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
	if err != nil || message == "" {
		t.Fatalf("remote download: %q %v", message, err)
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
	if err := service.UploadToPath("", merged2, "", 0); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing path should be a 405: %v", err)
	}
}

func TestDownloadPathValidation(t *testing.T) {
	service, base := testService(t)
	createProject(t, service, base, "P1", "proj", nil)
	if _, err := service.DownloadPath("P1", "", ""); err == nil || HTTPCodeOf(err) != 405 {
		t.Fatalf("missing filename should be a 405: %v", err)
	}
	if _, err := service.DownloadPath("P1", "", "../secret"); err == nil {
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
		if _, err := ValidateUploadPath(candidate); err == nil || HTTPCodeOf(err) != 405 {
			t.Fatalf("path %q must be a 405 validation error: %v", candidate, err)
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
