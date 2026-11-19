//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package api

import (
	"encoding/json"
	"net/http/httptest"
	"net/url"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/project"
)

// versionTestServer builds a router with the project file endpoints and one
// project whose version snapshot count is enabled.
func versionTestServer(t *testing.T) (*httptest.Server, *app.App, string) {
	t.Helper()
	instance := apiTestApp(t)
	lib := filepath.Join(instance.BaseDir, "proj")
	if err := os.MkdirAll(lib, 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(lib, "app.jar"), []byte("v1"), 0o644); err != nil {
		t.Fatal(err)
	}
	projects := map[string]any{
		"P1": map[string]any{
			"id":                 "P1",
			"name":               "proj",
			"whitelistDirectory": instance.BaseDir,
			"lib":                "/proj",
			"runMode":            "File",
		},
	}
	raw, err := json.Marshal(projects)
	if err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(filepath.Join(instance.DataDir, "project.json"), raw, 0o644); err != nil {
		t.Fatal(err)
	}
	if err := project.WriteWhitelist(instance.BaseDir, project.Whitelist{Project: []string{instance.BaseDir}}); err != nil {
		t.Fatal(err)
	}
	instance.Config.Jpom.Project.VersionCount = 2
	router := httpx.New(instance)
	registerProjectFile(router, instance)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)
	return server, instance, lib
}

func TestProjectFileVersionEndpoints(t *testing.T) {
	server, instance, _ := versionTestServer(t)

	capability := postForm(t, server, "/manage/file/version-capability", url.Values{"id": {"P1"}})
	if capability.Code != 200 || capability.Msg != "查询成功" {
		t.Fatalf("unexpected capability envelope: %+v", capability)
	}
	capabilityData := map[string]any{}
	if err := json.Unmarshal(capability.Data, &capabilityData); err != nil {
		t.Fatal(err)
	}
	if capabilityData["support"] != true || capabilityData["enabled"] != true {
		t.Fatalf("unexpected capability data: %v", capabilityData)
	}
	path, _ := capabilityData["path"].(string)
	if !strings.HasSuffix(filepath.ToSlash(path), "project_file_version/P1") {
		t.Fatalf("unexpected version path: %q", path)
	}

	begin := postForm(t, server, "/manage/file/version-begin", url.Values{"id": {"P1"}, "keepCount": {"2"}})
	if begin.Code != 200 || begin.Msg != "查询成功" {
		t.Fatalf("unexpected begin envelope: %+v", begin)
	}
	beginData := map[string]any{}
	if err := json.Unmarshal(begin.Data, &beginData); err != nil {
		t.Fatal(err)
	}
	baseline, ok := beginData["baseline"].(map[string]any)
	if !ok || !strings.HasPrefix(baseline["versionId"].(string), "baseline-") {
		t.Fatalf("begin must create the baseline: %v", beginData)
	}
	if beginData["current"] != nil {
		t.Fatalf("begin current must be null before the first commit: %v", beginData["current"])
	}

	list := postForm(t, server, "/manage/file/version-list", url.Values{"id": {"P1"}})
	listData := map[string]any{}
	if err := json.Unmarshal(list.Data, &listData); err != nil {
		t.Fatal(err)
	}
	if items, ok := listData["list"].([]any); !ok || len(items) != 1 {
		t.Fatalf("version list: %v", listData)
	}
	first := listData["list"].([]any)[0].(map[string]any)
	if first["source"] != "baseline" || first["exist"] != true || first["versionDir"] == "" {
		t.Fatalf("unexpected version item: %v", first)
	}

	// @ValidatorItem(versionId) -> code 400 before the project is resolved.
	missing := postForm(t, server, "/manage/file/version-switch", url.Values{"id": {"P1"}})
	if missing.Code != 400 || missing.Msg != "请选择版本" {
		t.Fatalf("missing versionId: %+v", missing)
	}
	unknown := postForm(t, server, "/manage/file/version-delete", url.Values{"id": {"P1"}, "versionId": {"nope"}})
	if unknown.Code != 405 || !strings.Contains(unknown.Msg, "版本不存在") {
		t.Fatalf("unknown version: %+v", unknown)
	}

	// The version root lives below data/project_file_version/<projectId>.
	stat, err := os.Stat(filepath.Join(instance.DataDir, "project_file_version", "P1"))
	if err != nil || !stat.IsDir() {
		t.Fatalf("version root must live below data/project_file_version: %v", err)
	}
}
