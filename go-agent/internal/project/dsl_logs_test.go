//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package project

import (
	"encoding/json"
	"log/slog"
	"os"
	"path/filepath"
	"testing"

	"github.com/dromara/Jpom/go-agent/internal/config"
)

const dslLogsContent = `
description: dsl log files
run:
  start:
    scriptId: start.sh
config:
  log:
    - name: app
      path: logs/app.log
    - path: logs/error.log
      charset: GBK
    - name: escape
      path: ../app.log
`

func TestParseDslLogFiles(t *testing.T) {
	parsed, err := parseDslContent(dslLogsContent)
	if err != nil {
		t.Fatalf("parse dsl content: %v", err)
	}
	entries := parseDslLogFiles(parsed)
	if len(entries) != 3 {
		t.Fatalf("want 3 entries, got %d", len(entries))
	}
	if entries[0].Name != "app" || entries[0].Path != "logs/app.log" {
		t.Fatalf("unexpected first entry: %+v", entries[0])
	}
	if entries[1].Charset != "GBK" {
		t.Fatalf("unexpected charset: %+v", entries[1])
	}
	// 未配置 config.log 返回 nil
	empty, err := parseDslContent("run:\n  start:\n    scriptId: a.sh\n")
	if err != nil {
		t.Fatalf("parse empty dsl content: %v", err)
	}
	if got := parseDslLogFiles(empty); got != nil {
		t.Fatalf("want nil, got %+v", got)
	}
}

func TestDslLogKey(t *testing.T) {
	// 与 Java DslProjectLogFileProvider.logKey 一致：md5(name + "\n" + path)
	key := dslLogKey(dslLogFileConfig{Name: "app", Path: "logs/app.log"})
	if key != "573e1c83583b4149816d301d8fc98944" {
		t.Fatalf("log key mismatch with Java: %s", key)
	}
}

func TestResolveDslLogPath(t *testing.T) {
	base := t.TempDir()
	variables := map[string]string{"PROJECT_PATH": base}
	if file, ok := resolveDslLogPath(base, "${PROJECT_PATH}/logs/app.log", variables); !ok || file != filepath.Join(base, "logs", "app.log") {
		t.Fatalf("variable path: %s %v", file, ok)
	}
	if file, ok := resolveDslLogPath(base, "logs/app.log", variables); !ok || file != filepath.Join(base, "logs", "app.log") {
		t.Fatalf("relative path: %s %v", file, ok)
	}
	if _, ok := resolveDslLogPath(base, "../escape.log", variables); ok {
		t.Fatal("relative path must stay inside project path")
	}
	absolute := filepath.Join(t.TempDir(), "app.log")
	if file, ok := resolveDslLogPath(base, absolute, variables); !ok || file != absolute {
		t.Fatalf("absolute path: %s %v", file, ok)
	}
	if _, ok := resolveDslLogPath(base, "   ", variables); ok {
		t.Fatal("blank path must be rejected")
	}
}

func TestDslLogFiles(t *testing.T) {
	base := t.TempDir()
	dataDir := filepath.Join(base, "data")
	if err := os.MkdirAll(dataDir, 0o755); err != nil {
		t.Fatalf("create data dir: %v", err)
	}
	info := map[string]any{
		"id": "p1", "name": "dsl", "workspaceId": "DEFAULT", "runMode": "Dsl",
		"whitelistDirectory": base, "lib": "lib", "dslContent": dslLogsContent,
	}
	encoded, err := json.Marshal(map[string]any{"p1": info})
	if err != nil {
		t.Fatalf("marshal project: %v", err)
	}
	if err := os.WriteFile(filepath.Join(dataDir, "project.json"), encoded, 0o644); err != nil {
		t.Fatalf("write project.json: %v", err)
	}
	appLog := filepath.Join(base, "lib", "logs", "app.log")
	if err := os.MkdirAll(filepath.Dir(appLog), 0o755); err != nil {
		t.Fatalf("create logs dir: %v", err)
	}
	if err := os.WriteFile(appLog, []byte("hello\n"), 0o644); err != nil {
		t.Fatalf("write app.log: %v", err)
	}
	runtime := NewRuntime(base, config.Default(), slog.Default())
	loaded, exists, err := runtime.Manager.Get("p1")
	if err != nil || !exists {
		t.Fatalf("load project: %v %v", exists, err)
	}
	items := runtime.DslLogFiles(loaded)
	// ../app.log 越界，被忽略
	if len(items) != 2 {
		t.Fatalf("want 2 items, got %d (%+v)", len(items), items)
	}
	if items[0].Name != "app" || !items[0].Exists || items[0].Size != 6 {
		t.Fatalf("unexpected first item: %+v", items[0])
	}
	if len(items[0].Key) != 32 {
		t.Fatalf("log key must be a md5 hex string: %+v", items[0])
	}
	if items[1].Name != "error.log" || items[1].Exists {
		t.Fatalf("unexpected second item: %+v", items[1])
	}
	if items[1].Charset != "GBK" {
		t.Fatalf("configured charset must be kept: %+v", items[1])
	}
	resolved, ok := runtime.ResolveDslLogFile(loaded, items[0].Key)
	if !ok || resolved.Path != appLog {
		t.Fatalf("resolve by key: %+v %v", resolved, ok)
	}
	if _, ok := runtime.ResolveDslLogFile(loaded, "not-exists"); ok {
		t.Fatal("unknown key must not resolve")
	}
	if _, ok := runtime.ResolveDslLogFile(loaded, ""); ok {
		t.Fatal("empty key must not resolve")
	}
	// logSize 支持按标识统计
	logBack, size, err := runtime.LogSizeOf(loaded, items[0].Key)
	if err != nil {
		t.Fatalf("log size: %v", err)
	}
	if logBack || size == "" {
		t.Fatalf("unexpected log size result: %v %q", logBack, size)
	}
}
