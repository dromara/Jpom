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
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/project"
)

// testApp builds an agent runtime in a temporary directory.
func testApp(t *testing.T) *app.App {
	t.Helper()
	base := t.TempDir()
	if err := os.MkdirAll(filepath.Join(base, "conf"), 0o755); err != nil {
		t.Fatalf("create conf dir: %v", err)
	}
	if err := os.WriteFile(filepath.Join(base, "conf", "application.yml"), []byte("# test configuration\n"), 0o644); err != nil {
		t.Fatalf("write application.yml: %v", err)
	}
	runtime, err := app.New("", base, "test", "2026-01-01T00:00:00Z")
	if err != nil {
		t.Fatalf("create agent runtime: %v", err)
	}
	t.Cleanup(runtime.Close)
	return runtime
}

// testService builds a project file service over a temporary agent runtime and
// returns the agent base directory as well.
func testService(t *testing.T) (*Service, string) {
	t.Helper()
	runtime := testApp(t)
	return New(runtime), runtime.BaseDir
}

// writeProject stores a project into data/project.json.
func writeProject(t *testing.T, base, id string, fields map[string]any) {
	t.Helper()
	info := map[string]any{"id": id}
	for key, value := range fields {
		info[key] = value
	}
	all := map[string]any{}
	path := filepath.Join(base, "data", "project.json")
	if raw, err := os.ReadFile(path); err == nil && len(raw) > 0 {
		if err := json.Unmarshal(raw, &all); err != nil {
			t.Fatalf("decode project.json: %v", err)
		}
	}
	all[id] = info
	raw, err := json.Marshal(all)
	if err != nil {
		t.Fatalf("encode project.json: %v", err)
	}
	if err := os.WriteFile(path, raw, 0o644); err != nil {
		t.Fatalf("write project.json: %v", err)
	}
}

// writeWhitelist stores the authorization and editable suffix configuration.
func writeWhitelist(t *testing.T, base string, projectDirs, allowEditSuffix []string) {
	t.Helper()
	whitelist := project.Whitelist{Project: projectDirs, AllowEditSuffix: allowEditSuffix}
	if err := project.WriteWhitelist(base, whitelist); err != nil {
		t.Fatalf("write whitelist: %v", err)
	}
}

// createProject wires a project whose lib directory is <base>/<name>.
func createProject(t *testing.T, service *Service, base, id, name string, extra map[string]any) string {
	t.Helper()
	lib := filepath.Join(base, name)
	if err := os.MkdirAll(lib, 0o755); err != nil {
		t.Fatalf("create project dir: %v", err)
	}
	fields := map[string]any{
		"name":               name,
		"whitelistDirectory": base,
		"lib":                "/" + name,
		"runMode":            "File",
	}
	for key, value := range extra {
		fields[key] = value
	}
	writeProject(t, base, id, fields)
	writeWhitelist(t, base, []string{base}, []string{"txt", "jar", "html", "log", "yml"})
	return lib
}

// writeFile writes content creating the parent directory.
func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatalf("write %s: %v", path, err)
	}
}

func TestSecureJoinRejectsTraversalAndSymlinkEscape(t *testing.T) {
	base := t.TempDir()
	inside := filepath.Join(base, "project")
	if err := os.MkdirAll(inside, 0o755); err != nil {
		t.Fatal(err)
	}
	writeFile(t, filepath.Join(inside, "ok.txt"), "ok")

	if _, err := secureJoin(inside, "ok.txt"); err != nil {
		t.Fatalf("expected valid path: %v", err)
	}
	if target, err := secureJoin(inside, "/"); err != nil || target != inside {
		t.Fatalf("root fragment should resolve to base: %q %v", target, err)
	}
	for _, candidate := range []string{"..", "../other", "dir/../../other", "a/../../../etc/passwd"} {
		if _, err := secureJoin(inside, candidate); err == nil {
			t.Fatalf("expected traversal %q to be rejected", candidate)
		}
	}
	outside := t.TempDir()
	if err := os.Symlink(outside, filepath.Join(inside, "link")); err != nil {
		t.Fatal(err)
	}
	if _, err := secureJoin(inside, "link", "escape.txt"); err == nil {
		t.Fatal("expected symlink escape to be rejected")
	}
}

func TestGetFileListFieldsAndEditableSuffix(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeFile(t, filepath.Join(lib, "b.jar"), "jar")
	writeFile(t, filepath.Join(lib, "a.txt"), "text")
	writeFile(t, filepath.Join(lib, "sub", "c.log"), "log")

	files, err := service.GetFileList("P1", "")
	if err != nil {
		t.Fatalf("get file list: %v", err)
	}
	if len(files) != 3 {
		t.Fatalf("expected 3 entries, got %d", len(files))
	}
	if files[0].Filename != "a.txt" || files[1].Filename != "b.jar" || files[2].Filename != "sub" {
		t.Fatalf("unexpected order: %+v", files)
	}
	if files[0].TextFileEdit == nil || !*files[0].TextFileEdit {
		t.Fatalf("a.txt should be editable")
	}
	if files[2].IsDirectory != true {
		t.Fatalf("sub should be a directory")
	}
	if files[0].LevelName != "" {
		t.Fatalf("root levelName should be empty: %q", files[0].LevelName)
	}
	if files[2].FileSizeLong == "" {
		t.Fatalf("directory size should be present when scanning is enabled")
	}
	if files[0].FileSizeLong != "4" {
		t.Fatalf("file size must be a JSON string, got %q", files[0].FileSizeLong)
	}
	if files[0].ModifyTimeLong == "" {
		t.Fatalf("modifyTimeLong must be set")
	}

	children, err := service.GetFileList("P1", "sub")
	if err != nil {
		t.Fatalf("get sub file list: %v", err)
	}
	if len(children) != 1 || children[0].LevelName != "sub" {
		t.Fatalf("unexpected sub levelName: %+v", children)
	}

	// A path outside the authorized directory must be rejected by secureJoin.
	if _, err := service.GetFileList("P1", "../../"); err == nil {
		t.Fatal("expected path traversal to be rejected")
	}
}

func TestGetFileListResolvesSoftLinkProject(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "REAL", "real", nil)
	writeFile(t, filepath.Join(lib, "app.yml"), "name: demo")
	writeProject(t, base, "LINK", map[string]any{
		"runMode": "Link",
		"linkId":  "REAL",
	})
	files, err := service.GetFileList("LINK", "")
	if err != nil {
		t.Fatalf("get file list of linked project: %v", err)
	}
	if len(files) != 1 || files[0].Filename != "app.yml" {
		t.Fatalf("unexpected linked project files: %+v", files)
	}
}

func TestDiffFile(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeFile(t, filepath.Join(lib, "same.txt"), "same")
	writeFile(t, filepath.Join(lib, "changed.txt"), "new")
	writeFile(t, filepath.Join(lib, "local-only.txt"), "local")

	sameSHA1, err := fileSHA1(filepath.Join(lib, "same.txt"))
	if err != nil {
		t.Fatal(err)
	}
	changedSHA1, err := fileSHA1(filepath.Join(lib, "changed.txt"))
	if err != nil {
		t.Fatal(err)
	}
	result, err := service.DiffFile(DiffFileRequest{
		ID: "P1",
		Data: []DiffItem{
			{Name: "same.txt", Sha1: sameSHA1},
			{Name: "changed.txt", Sha1: "old-signature"},
			{Name: "remote-only.txt", Sha1: "remote-signature"},
		},
	})
	if err != nil {
		t.Fatalf("diff file: %v", err)
	}
	if len(result.Diff) != 2 {
		t.Fatalf("expected 2 files to upload, got %+v", result.Diff)
	}
	if result.Diff[0].Name != "changed.txt" || result.Diff[0].Sha1 != "old-signature" {
		t.Fatalf("unexpected diff entry: %+v", result.Diff)
	}
	if result.Diff[1].Name != "remote-only.txt" {
		t.Fatalf("unexpected diff entry: %+v", result.Diff)
	}
	if len(result.Del) != 1 || result.Del[0].Name != "local-only.txt" || result.Del[0].Sha1 == "" {
		t.Fatalf("unexpected delete entries: %+v", result.Del)
	}
	_ = changedSHA1

	if _, err := service.DiffFile(DiffFileRequest{ID: "P1"}); err == nil {
		t.Fatal("expected empty comparison data to fail")
	}
}

func TestWhitelistRejectsProjectOutsideAuthorization(t *testing.T) {
	service, base := testService(t)
	outside := t.TempDir()
	writeProject(t, base, "P1", map[string]any{
		"whitelistDirectory": outside,
		"lib":                "/proj",
		"runMode":            "File",
	})
	writeWhitelist(t, base, []string{base}, []string{"txt"})
	if _, err := service.LibPath(map[string]any{"id": "P1", "whitelistDirectory": outside, "lib": "/proj"}); err == nil {
		t.Fatal("expected project outside the whitelist to be rejected")
	}
}

func TestEditCharsetRules(t *testing.T) {
	charset, err := checkFileSuffix([]string{"txt", "conf@UTF-8", "yml"}, "app.conf")
	if err != nil || charset != "UTF-8" {
		t.Fatalf("unexpected charset: %q %v", charset, err)
	}
	if _, err := checkFileSuffix(nil, "app.txt"); err == nil {
		t.Fatal("expected missing suffix configuration to fail")
	}
	if _, err := checkFileSuffix([]string{"txt"}, "app.bin"); err == nil {
		t.Fatal("expected disallowed suffix to fail")
	}
	if !checkSilentFileSuffix([]string{"txt", "^.+/?(?i)(log)$"}, "server.log") {
		t.Fatal("regex suffix should match")
	}
	if !checkSilentFileSuffix([]string{"*"}, "anything.bin") {
		t.Fatal("wildcard suffix should match every file")
	}
}
