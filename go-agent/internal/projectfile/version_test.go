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
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// versionTestSetup builds a project with a configured version snapshot count.
func versionTestSetup(t *testing.T, count int) (*Service, string, string) {
	t.Helper()
	service, base := testService(t)
	service.app.Config.Jpom.Project.VersionCount = count
	lib := createProject(t, service, base, "P1", "proj", nil)
	return service, base, lib
}

// versionRootOf resolves the version root of the test project.
func versionRootOf(t *testing.T, service *Service) string {
	t.Helper()
	info, err := service.Project("P1")
	if err != nil {
		t.Fatalf("project: %v", err)
	}
	settings, err := service.versionSettings(info)
	if err != nil {
		t.Fatalf("version settings: %v", err)
	}
	return absolutePath(service.versionRoot(settings))
}

// versionSleep keeps the createTime of two snapshots distinct so the newest
// first ordering is deterministic.
func versionSleep() { time.Sleep(3 * time.Millisecond) }

func TestVersionManifestStable(t *testing.T) {
	root := t.TempDir()
	writeFile(t, filepath.Join(root, "b.txt"), "two")
	writeFile(t, filepath.Join(root, "a", "c.txt"), "three")
	first, err := versionManifest(root)
	if err != nil {
		t.Fatalf("manifest: %v", err)
	}
	again, err := versionManifest(root)
	if err != nil || again != first {
		t.Fatalf("manifest must be stable: %q %q %v", first, again, err)
	}
	// logs/temp are not part of the manifest.
	writeFile(t, filepath.Join(root, "logs", "app.log"), "log")
	writeFile(t, filepath.Join(root, "temp", "upload.tmp"), "temp")
	if ignored, err := versionManifest(root); err != nil || ignored != first {
		t.Fatalf("logs/temp must not change the manifest: %q %v", ignored, err)
	}
	// Temporary names (anything containing ".tmp") are ignored at any depth, so
	// Java and Go compute the same fingerprint for the same directory.
	writeFile(t, filepath.Join(root, "app.jar.tmp123"), "tmp")
	writeFile(t, filepath.Join(root, "a", "b.tmp456"), "tmp")
	if ignored, err := versionManifest(root); err != nil || ignored != first {
		t.Fatalf(".tmp files must not change the manifest: %q %v", ignored, err)
	}
	if count, _, err := versionStats(root); err != nil || count != 2 {
		t.Fatalf(".tmp files must not be counted: %d %v", count, err)
	}
	// The creation order does not matter, the content does.
	other := t.TempDir()
	writeFile(t, filepath.Join(other, "a", "c.txt"), "three")
	writeFile(t, filepath.Join(other, "b.txt"), "two")
	if same, err := versionManifest(other); err != nil || same != first {
		t.Fatalf("manifest must not depend on the creation order: %q %v", same, err)
	}
	writeFile(t, filepath.Join(root, "b.txt"), "changed")
	if changed, err := versionManifest(root); err != nil || changed == first {
		t.Fatalf("manifest must change with the content: %q %v", changed, err)
	}
}

func TestVersionManifestJavaOrdering(t *testing.T) {
	// Java String#compareTo orders UTF-16 code units, so a supplementary
	// character (surrogate pair, e.g. emoji) sorts before U+FFFF while the Go
	// rune order would place it after.
	if !javaStringLess("😀", "\uFFFF") {
		t.Fatal("manifest lines must be sorted by UTF-16 code unit like Java")
	}
	if javaStringLess("b", "a") || javaStringLess("a", "a") || !javaStringLess("a", "ab") {
		t.Fatal("javaStringLess basic ordering")
	}
}

func TestVersionCapabilityAndDisabledBegin(t *testing.T) {
	service, _, _ := versionTestSetup(t, 2)
	capability, err := service.VersionCapability("P1")
	if err != nil {
		t.Fatalf("capability: %v", err)
	}
	if !capability.Support || !capability.Enabled || capability.VersionCount != 2 {
		t.Fatalf("unexpected capability: %+v", capability)
	}
	if !filepath.IsAbs(capability.Path) || !strings.HasSuffix(filepath.ToSlash(capability.Path), "project_file_version/P1") {
		t.Fatalf("version path must be the absolute data directory: %s", capability.Path)
	}

	// versionCount <= 0 keeps only enabled/versionCount in the response.
	service.app.Config.Jpom.Project.VersionCount = 0
	begin, err := service.VersionBegin("P1", nil, nil)
	if err != nil {
		t.Fatalf("begin: %v", err)
	}
	if begin.Enabled || begin.Count != nil {
		t.Fatalf("disabled begin must not snapshot: %+v", begin)
	}
	raw, err := json.Marshal(begin)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	if string(raw) != `{"enabled":false,"versionCount":0}` {
		t.Fatalf("disabled begin payload: %s", raw)
	}
	commit, err := service.VersionCommit("P1", nil, "", nil)
	if err != nil || commit.Enabled {
		t.Fatalf("disabled commit must be a no-op: %+v %v", commit, err)
	}
}

func TestVersionBeginCreatesBaseline(t *testing.T) {
	service, _, lib := versionTestSetup(t, 2)
	writeFile(t, filepath.Join(lib, "app.jar"), "v1")
	writeFile(t, filepath.Join(lib, "conf", "app.yml"), "name: demo")
	writeFile(t, filepath.Join(lib, "logs", "app.log"), "console log")

	result, err := service.VersionBegin("P1", nil, nil)
	if err != nil {
		t.Fatalf("begin: %v", err)
	}
	if !result.Enabled || result.VersionCount != 2 {
		t.Fatalf("unexpected begin result: %+v", result)
	}
	if result.Baseline == nil || result.Baseline.Source != versionSourceBaseline ||
		!strings.HasPrefix(result.Baseline.VersionID, "baseline-") {
		t.Fatalf("baseline snapshot missing: %+v", result.Baseline)
	}
	if result.Count == nil || *result.Count != 1 {
		t.Fatalf("baseline count: %+v", result.Count)
	}
	if result.Current != nil {
		t.Fatalf("no version committed yet, current must be null: %q", *result.Current)
	}
	root := versionRootOf(t, service)
	baselineDir := filepath.Join(root, result.Baseline.VersionID)
	if !exists(filepath.Join(baselineDir, "app.jar")) || !exists(filepath.Join(baselineDir, "conf", "app.yml")) {
		t.Fatalf("baseline content incomplete: %v", snapshotNames(t, baselineDir))
	}
	if exists(filepath.Join(baselineDir, "logs")) {
		t.Fatal("baseline must skip logs/temp")
	}
	if !exists(filepath.Join(root, result.Baseline.VersionID+versionMetaSuffix)) {
		t.Fatal("baseline meta file missing")
	}

	// The enabled payload always carries current/count keys.
	payload, err := json.Marshal(result)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	if !strings.Contains(string(payload), `"current":null`) || !strings.Contains(string(payload), `"count":1`) {
		t.Fatalf("begin payload: %s", payload)
	}

	// A second begin must not create another baseline.
	again, err := service.VersionBegin("P1", nil, nil)
	if err != nil {
		t.Fatalf("second begin: %v", err)
	}
	if again.Baseline != nil || again.Count == nil || *again.Count != 1 {
		t.Fatalf("second begin must reuse the baseline: %+v", again)
	}
}

func TestVersionCommitSetsCurrent(t *testing.T) {
	service, _, lib := versionTestSetup(t, 3)
	writeFile(t, filepath.Join(lib, "app.jar"), "v1")
	writeFile(t, filepath.Join(lib, "conf", "app.yml"), "name: demo")
	if _, err := service.VersionBegin("P1", nil, nil); err != nil {
		t.Fatalf("begin: %v", err)
	}
	versionSleep()
	buildNumberID := 42
	result, err := service.VersionCommit("P1", &buildNumberID, "BD-1", nil)
	if err != nil {
		t.Fatalf("commit: %v", err)
	}
	if !result.Enabled || result.VersionID != "42" {
		t.Fatalf("unexpected commit result: %+v", result)
	}
	root := versionRootOf(t, service)
	current := service.readVersionCurrent(root)
	if current == nil || *current != "42" {
		t.Fatalf("current.txt must point to the committed version: %v", current)
	}
	versionDir := filepath.Join(root, "42")
	manifest, err := versionManifest(versionDir)
	if err != nil || manifest != result.Manifest {
		t.Fatalf("commit manifest mismatch: %q %v", manifest, err)
	}
	if *result.FileCount != 2 || *result.FileSize <= 0 {
		t.Fatalf("unexpected file stats: count=%d size=%d", *result.FileCount, *result.FileSize)
	}
	meta := service.readVersionMeta(root, "42")
	if meta == nil || meta.Source != versionSourceBuild || meta.BuildNumberID == nil || *meta.BuildNumberID != 42 {
		t.Fatalf("unexpected meta: %+v", meta)
	}
	if meta.BuildDataID == nil || *meta.BuildDataID != "BD-1" {
		t.Fatalf("buildDataId must be stored: %+v", meta.BuildDataID)
	}

	list, err := service.VersionList("P1")
	if err != nil {
		t.Fatalf("list: %v", err)
	}
	if list.Current == nil || *list.Current != "42" || len(list.List) != 2 {
		t.Fatalf("unexpected version list: %+v", list)
	}
	newest := list.List[0]
	if newest.VersionID != "42" || newest.VersionDir != versionDir || newest.Exist == nil || !*newest.Exist {
		t.Fatalf("version list must be newest first with computed fields: %+v", newest)
	}
}

func TestVersionSwitchRestoresContent(t *testing.T) {
	service, _, lib := versionTestSetup(t, 5)
	writeFile(t, filepath.Join(lib, "app.jar"), "v1")
	writeFile(t, filepath.Join(lib, "conf", "app.yml"), "name: demo")
	if _, err := service.VersionBegin("P1", nil, nil); err != nil {
		t.Fatalf("begin: %v", err)
	}
	versionSleep()
	buildNumberID := 7
	if _, err := service.VersionCommit("P1", &buildNumberID, "", nil); err != nil {
		t.Fatalf("commit: %v", err)
	}

	// A later release replaces the files: the old version must come back.
	writeFile(t, filepath.Join(lib, "app.jar"), "v2")
	writeFile(t, filepath.Join(lib, "extra.txt"), "only in v2")
	writeFile(t, filepath.Join(lib, "logs", "app.log"), "console log")
	result, err := service.VersionSwitch("P1", "7", 0, nil, nil)
	if err != nil {
		t.Fatalf("switch: %v", err)
	}
	if result.VersionID != "7" || result.Restarted {
		t.Fatalf("unexpected switch result: %+v", result)
	}
	if result.PreviousVersionID == nil || *result.PreviousVersionID != "7" {
		t.Fatalf("previous version: %v", result.PreviousVersionID)
	}
	if result.SafetyVersionID == nil || !strings.HasPrefix(*result.SafetyVersionID, "auto-") {
		t.Fatalf("safety snapshot missing: %v", result.SafetyVersionID)
	}
	assertFileContent(t, filepath.Join(lib, "app.jar"), "v1")
	assertFileContent(t, filepath.Join(lib, "conf", "app.yml"), "name: demo")
	if exists(filepath.Join(lib, "extra.txt")) {
		t.Fatal("switch must replace the project content")
	}
	if !exists(filepath.Join(lib, "logs", "app.log")) {
		t.Fatal("logs must survive a switch")
	}
	root := versionRootOf(t, service)
	if current := service.readVersionCurrent(root); current == nil || *current != "7" {
		t.Fatalf("current.txt after switch: %v", current)
	}

	// The safety snapshot keeps the content of the replaced release.
	safety := *result.SafetyVersionID
	if _, err := service.VersionSwitch("P1", safety, 0, boolPointer(false), nil); err != nil {
		t.Fatalf("switch to safety version: %v", err)
	}
	assertFileContent(t, filepath.Join(lib, "app.jar"), "v2")
	assertFileContent(t, filepath.Join(lib, "extra.txt"), "only in v2")
}

func TestVersionSwitchMissingVersion(t *testing.T) {
	service, _, _ := versionTestSetup(t, 2)
	_, err := service.VersionSwitch("P1", "not-exists", 0, nil, nil)
	if err == nil || HTTPCodeOf(err) != 405 || !strings.Contains(err.Error(), "版本不存在") {
		t.Fatalf("missing version must be a 405 error, got %v", err)
	}
	_, err = service.VersionSwitch("P1", " ", 0, nil, nil)
	if err == nil || HTTPCodeOf(err) != 405 || !strings.Contains(err.Error(), "请选择版本") {
		t.Fatalf("blank versionId must be a 405 error, got %v", err)
	}
}

func TestVersionDeleteRules(t *testing.T) {
	service, _, lib := versionTestSetup(t, 5)
	writeFile(t, filepath.Join(lib, "app.jar"), "v1")
	if _, err := service.VersionBegin("P1", nil, nil); err != nil {
		t.Fatalf("begin: %v", err)
	}
	versionSleep()
	first := 1
	if _, err := service.VersionCommit("P1", &first, "", nil); err != nil {
		t.Fatalf("commit 1: %v", err)
	}
	versionSleep()
	second := 2
	if _, err := service.VersionCommit("P1", &second, "", nil); err != nil {
		t.Fatalf("commit 2: %v", err)
	}
	root := versionRootOf(t, service)

	// The active version cannot be deleted.
	if err := service.VersionDelete("P1", "2"); err == nil || HTTPCodeOf(err) != 405 ||
		!strings.Contains(err.Error(), "当前运行版本不能删除") {
		t.Fatalf("deleting the current version must be rejected, got %v", err)
	}
	// An unknown version is reported as missing.
	if err := service.VersionDelete("P1", "not-exists"); err == nil || !strings.Contains(err.Error(), "版本不存在") {
		t.Fatalf("deleting an unknown version must fail, got %v", err)
	}
	// A non current version is removed together with its meta.
	if err := service.VersionDelete("P1", "1"); err != nil {
		t.Fatalf("delete version: %v", err)
	}
	if exists(filepath.Join(root, "1")) || exists(filepath.Join(root, "1"+versionMetaSuffix)) {
		t.Fatal("deleted version must remove the directory and the meta")
	}
	if !exists(filepath.Join(root, "2")) {
		t.Fatal("current version must not be touched")
	}
}

func TestVersionCleanupKeepsCurrent(t *testing.T) {
	service, _, lib := versionTestSetup(t, 5)
	writeFile(t, filepath.Join(lib, "app.jar"), "v1")
	begin, err := service.VersionBegin("P1", nil, nil)
	if err != nil || begin.Baseline == nil {
		t.Fatalf("begin: %+v %v", begin, err)
	}
	baseline := begin.Baseline.VersionID
	for _, buildNumberID := range []int{1, 2} {
		versionSleep()
		id := buildNumberID
		if _, err := service.VersionCommit("P1", &id, "", nil); err != nil {
			t.Fatalf("commit %d: %v", id, err)
		}
	}
	root := versionRootOf(t, service)
	// keepCount=1 keeps the newest version plus the active (baseline) one.
	if _, err := service.VersionSwitch("P1", baseline, 0, boolPointer(false), intPointer(1)); err != nil {
		t.Fatalf("switch: %v", err)
	}
	if current := service.readVersionCurrent(root); current == nil || *current != baseline {
		t.Fatalf("current after switch: %v", current)
	}
	if !exists(filepath.Join(root, baseline)) || !exists(filepath.Join(root, baseline+versionMetaSuffix)) {
		t.Fatal("the active version must never be pruned")
	}
	if !exists(filepath.Join(root, "2")) {
		t.Fatal("the newest version must be kept")
	}
	if exists(filepath.Join(root, "1")) || exists(filepath.Join(root, "1"+versionMetaSuffix)) {
		t.Fatal("the oldest version must be pruned")
	}
}

// assertFileContent fails when path does not contain want.
func assertFileContent(t *testing.T, path, want string) {
	t.Helper()
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read %s: %v", path, err)
	}
	if string(raw) != want {
		t.Fatalf("content of %s = %q, want %q", path, raw, want)
	}
}

func boolPointer(value bool) *bool { return &value }

func intPointer(value int) *int { return &value }
