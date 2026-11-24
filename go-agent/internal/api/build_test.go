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
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/httpx"
)

// buildTestServer builds a router with the build endpoints only.
func buildTestServer(t *testing.T) (*httptest.Server, *app.App) {
	t.Helper()
	instance := apiTestApp(t)
	router := httpx.New(instance)
	registerBuild(router, instance)
	server := httptest.NewServer(router)
	t.Cleanup(server.Close)
	return server, instance
}

// TestBuildStatusUnknownJobReportsLost 覆盖未知 jobId → state=lost（PROTOCOL §4）。
func TestBuildStatusUnknownJobReportsLost(t *testing.T) {
	server, _ := buildTestServer(t)
	envelope := postJSON(t, server, "/build/status", map[string]string{"jobId": "missing"})
	if envelope.Code != 200 {
		t.Fatalf("code = %d msg=%s", envelope.Code, envelope.Msg)
	}
	status := map[string]any{}
	if err := json.Unmarshal(envelope.Data, &status); err != nil {
		t.Fatalf("decode data: %v", err)
	}
	if status["state"] != "lost" {
		t.Fatalf("state = %v, want lost", status["state"])
	}
}

// TestBuildSourceExistsEndpoint 覆盖 /build/source-exists 的批量探测。
func TestBuildSourceExistsEndpoint(t *testing.T) {
	server, instance := buildTestServer(t)
	if err := os.MkdirAll(filepath.Join(instance.DataDir, "build", "model-1", "source"), 0o755); err != nil {
		t.Fatal(err)
	}
	envelope := postJSON(t, server, "/build/source-exists", map[string]any{
		"buildModelIds": []string{"model-1", "model-2"},
	})
	if envelope.Code != 200 {
		t.Fatalf("code = %d msg=%s", envelope.Code, envelope.Msg)
	}
	result := map[string]bool{}
	if err := json.Unmarshal(envelope.Data, &result); err != nil {
		t.Fatalf("decode data: %v", err)
	}
	if !result["model-1"] || result["model-2"] {
		t.Fatalf("source-exists = %+v", result)
	}
}

// TestBuildCancelIsIdempotent 覆盖取消幂等（未知 jobId 返回成功，PROTOCOL §5）。
func TestBuildCancelIsIdempotent(t *testing.T) {
	server, _ := buildTestServer(t)
	for index := 0; index < 2; index++ {
		envelope := postJSON(t, server, "/build/cancel", map[string]string{"jobId": "missing"})
		if envelope.Code != 200 {
			t.Fatalf("第 %d 次取消 code = %d msg=%s", index+1, envelope.Code, envelope.Msg)
		}
	}
}

// TestBuildStartUnsupportedRepository 覆盖 svn 等不支持类型 → 501（PROTOCOL §2）。
func TestBuildStartUnsupportedRepository(t *testing.T) {
	server, _ := buildTestServer(t)
	envelope := postJSON(t, server, "/build/start", map[string]any{
		"jobId":        "job-svn",
		"buildModelId": "model-1",
		"repo":         map[string]any{"type": "svn", "url": "svn://example.com/repo"},
	})
	if envelope.Code != http.StatusNotImplemented {
		t.Fatalf("code = %d, want 501 (msg=%s)", envelope.Code, envelope.Msg)
	}
}

// TestBuildDownloadNotReady 覆盖产物未就绪 → HTTP 404（PROTOCOL §6）。
func TestBuildDownloadNotReady(t *testing.T) {
	server, _ := buildTestServer(t)
	request, err := http.NewRequest(http.MethodGet, server.URL+"/build/download?jobId=missing", nil)
	if err != nil {
		t.Fatal(err)
	}
	request.Header.Set("Jpom-Agent-Authorize", apiTestAuthorize)
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode != http.StatusNotFound {
		t.Fatalf("status = %d, want 404", response.StatusCode)
	}
}

// TestBuildDownloadRangeResume 覆盖断点续传：Range 请求返回 206 且只发送剩余字节（PROTOCOL §6）。
// 2026-11-20 真机事故：Server 侧在网络中断后必须能从断点继续，节点不支持 Range 时会重下全量。
func TestBuildDownloadRangeResume(t *testing.T) {
	server, instance := buildTestServer(t)
	payload := []byte("0123456789abcdefghij")
	zipPath := filepath.Join(instance.DataDir, "build", "model-range", "history", "#7", "zip")
	if err := os.MkdirAll(filepath.Dir(zipPath), 0o755); err != nil {
		t.Fatal(err)
	}
	if err := os.WriteFile(zipPath, payload, 0o644); err != nil {
		t.Fatal(err)
	}
	do := func(rangeHeader string) (*http.Response, []byte) {
		request, err := http.NewRequest(http.MethodGet,
			server.URL+"/build/download?buildModelId=model-range&buildNumberId=7", nil)
		if err != nil {
			t.Fatal(err)
		}
		if rangeHeader != "" {
			request.Header.Set("Range", rangeHeader)
		}
		request.Header.Set("Jpom-Agent-Authorize", apiTestAuthorize)
		response, err := http.DefaultClient.Do(request)
		if err != nil {
			t.Fatal(err)
		}
		body, readErr := io.ReadAll(response.Body)
		_ = response.Body.Close()
		if readErr != nil {
			t.Fatal(readErr)
		}
		return response, body
	}

	response, body := do("")
	if response.StatusCode != http.StatusOK {
		t.Fatalf("全量下载 status = %d, want 200", response.StatusCode)
	}
	if string(body) != string(payload) {
		t.Fatalf("全量下载内容 = %q", string(body))
	}
	if response.Header.Get("Accept-Ranges") != "bytes" {
		t.Fatalf("Accept-Ranges = %q, want bytes", response.Header.Get("Accept-Ranges"))
	}

	response, body = do("bytes=5-")
	if response.StatusCode != http.StatusPartialContent {
		t.Fatalf("续传 status = %d, want 206", response.StatusCode)
	}
	if string(body) != string(payload[5:]) {
		t.Fatalf("续传内容 = %q, want %q", string(body), string(payload[5:]))
	}
	wantRange := fmt.Sprintf("bytes %d-%d/%d", 5, len(payload)-1, len(payload))
	if got := response.Header.Get("Content-Range"); got != wantRange {
		t.Fatalf("Content-Range = %q, want %q", got, wantRange)
	}

	if response, _ = do("bytes=20-"); response.StatusCode != http.StatusRequestedRangeNotSatisfiable {
		t.Fatalf("越界 Range status = %d, want 416", response.StatusCode)
	}
	if response, _ = do("bytes=-5"); response.StatusCode != http.StatusRequestedRangeNotSatisfiable {
		t.Fatalf("后缀 Range status = %d, want 416", response.StatusCode)
	}
}

// TestParseRangeStart 覆盖 Range 头解析（仅支持单一区间 bytes=N- / bytes=N-M）。
func TestParseRangeStart(t *testing.T) {
	cases := []struct {
		raw    string
		offset int64
		ok     bool
	}{
		{"bytes=0-", 0, true},
		{"bytes=1024-", 1024, true},
		{"bytes=100-200", 100, true},
		{"bytes=-500", 0, false},
		{"bytes=0-10,20-30", 0, false},
		{"items=0-", 0, false},
		{"", 0, false},
	}
	for _, item := range cases {
		offset, ok := parseRangeStart(item.raw)
		if ok != item.ok || (ok && offset != item.offset) {
			t.Fatalf("parseRangeStart(%q) = (%d,%v), want (%d,%v)", item.raw, offset, ok, item.offset, item.ok)
		}
	}
}

// TestBuildEndpointsRequireAuthorize 覆盖鉴权头缺失时返回 900（httpx 约定）。
func TestBuildEndpointsRequireAuthorize(t *testing.T) {
	server, _ := buildTestServer(t)
	request, err := http.NewRequest(http.MethodPost, server.URL+"/build/status", nil)
	if err != nil {
		t.Fatal(err)
	}
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = response.Body.Close() }()
	envelope := apiEnvelope{}
	if err := json.NewDecoder(response.Body).Decode(&envelope); err != nil {
		t.Fatalf("decode: %v", err)
	}
	if envelope.Code != 900 {
		t.Fatalf("code = %d, want 900", envelope.Code)
	}
}
