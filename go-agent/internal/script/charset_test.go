/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package script

import (
	"os"
	"strings"
	"testing"
	"time"

	"github.com/dromara/Jpom/go-agent/internal/app"
	"github.com/dromara/Jpom/go-agent/internal/config"
)

func testAppWithConfig(t *testing.T) *app.App {
	instance := testApp(t)
	instance.Config = config.Default()
	return instance
}

func TestCharsetResolution(t *testing.T) {
	if got := ConsoleCharset(""); got != PlatformCharset() {
		t.Fatalf("console charset must fall back to platform charset, got %s", got)
	}
	if got := ConsoleCharset("gbk"); got != CharsetGBK {
		t.Fatalf("unexpected console charset: %s", got)
	}
	if got := SystemLogCharset(""); got != CharsetUTF8 {
		t.Fatalf("system log charset must default to UTF-8, got %s", got)
	}
	if got := SystemLogCharset("GBK"); got != CharsetGBK {
		t.Fatalf("unexpected system log charset: %s", got)
	}
	if got := ProjectLogCharset("GBK", "UTF-8"); got != CharsetGBK {
		t.Fatalf("project charset must win, got %s", got)
	}
	if got := ProjectLogCharset("", "UTF-8"); got != CharsetUTF8 {
		t.Fatalf("configured file-charset must be used, got %s", got)
	}
	if got := ProjectLogCharset("", ""); got != PlatformCharset() {
		t.Fatalf("project log charset must fall back to platform, got %s", got)
	}
	if got := ProjectLogCharset("not-a-charset", "GB18030"); got != CharsetGB1803 {
		t.Fatalf("unknown charset must fall back, got %s", got)
	}
}

func TestEncodeDecodeGBK(t *testing.T) {
	text := "中文日志" + string(rune(0x1F680))
	encoded, err := EncodeText(text, CharsetGBK)
	if err != nil {
		t.Fatal(err)
	}
	if string(encoded) == text {
		t.Fatal("GBK encoding must change the bytes")
	}
	decoded := DecodeText(encoded, CharsetGBK)
	// 不支持映射的 emoji 会按 JVM 行为替换为 '?'
	if !strings.HasPrefix(decoded, "中文日志") {
		t.Fatalf("GBK roundtrip failed: %q", decoded)
	}
	if !strings.Contains(decoded, "?") {
		t.Fatalf("unmappable rune must become ?: %q", decoded)
	}
	if got := DecodeText([]byte(text), CharsetUTF8); got != text {
		t.Fatalf("UTF-8 decode must pass through: %q", got)
	}
	if got := DecodeText([]byte{0xFF, 0xFE, 0x01}, CharsetGBK); got == "" {
		t.Fatal("invalid bytes must decode to a replacement string")
	}
}

func TestWriteScriptFileEncodesConsoleCharset(t *testing.T) {
	instance := testAppWithConfig(t)
	instance.Config.Jpom.System.ConsoleCharset = "GBK"
	manager := NewManager(instance)
	path, err := manager.WriteScriptFile("echo 中文")
	if err != nil {
		t.Fatal(err)
	}
	content, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	if DecodeText(content, CharsetGBK) != "echo 中文" {
		t.Fatalf("script file must be GBK encoded, got %q", string(content))
	}
	rawPath, err := manager.WriteRawScriptFile("echo 脚本库")
	if err != nil {
		t.Fatal(err)
	}
	rawContent, err := os.ReadFile(rawPath)
	if err != nil {
		t.Fatal(err)
	}
	if DecodeText(rawContent, CharsetGBK) != "echo 脚本库" {
		t.Fatalf("raw script file must be GBK encoded, got %q", string(rawContent))
	}
}

func TestExecutorDecodesGBKConsoleOutput(t *testing.T) {
	instance := testAppWithConfig(t)
	instance.Config.Jpom.System.ConsoleCharset = "GBK"
	manager := NewManager(instance)
	executor := NewExecutor(manager)
	// bash printf 输出 GBK 字节的“中文”: D6 D0 CE C4
	item := map[string]any{
		"id": "gbk-1", "name": "gbk", "context": "printf '\\xd6\\xd0\\xce\\xc4\\n'", "workspaceId": "DEFAULT",
	}
	executeID := NewExecutionID()
	if err := executor.Start(item, executeID, "", nil, "zh-CN"); err != nil {
		t.Fatal(err)
	}
	deadline := time.Now().Add(10 * time.Second)
	for executor.IsRun(executeID) && time.Now().Before(deadline) {
		time.Sleep(20 * time.Millisecond)
	}
	_, lines, err := ReadLogFile(manager.LogFile("gbk-1", executeID), 1)
	if err != nil {
		t.Fatal(err)
	}
	joined := strings.Join(lines, string(rune(10)))
	if !strings.Contains(joined, "中文") {
		t.Fatalf("GBK output must be decoded to UTF-8, log=%q", joined)
	}
	raw, err := os.ReadFile(manager.LogFile("gbk-1", executeID))
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(string(raw), "中文") {
		t.Fatalf("execution log file must be UTF-8: %q", string(raw))
	}
}
