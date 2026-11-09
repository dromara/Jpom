/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package proc

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestShellQuote(t *testing.T) {
	quoted := ShellQuote("/tmp/a b")
	if !strings.HasPrefix(quoted, "'") || !strings.HasSuffix(quoted, "'") {
		t.Fatalf("ShellQuote 结果错误: %s", quoted)
	}
	escaped := ShellQuote("it's")
	if escaped != "'it'\\''s'" {
		t.Fatalf("ShellQuote 单引号转义错误: %s", escaped)
	}
}

func TestSplitOutput(t *testing.T) {
	lines := splitOutput("a\r\nb\n\nc\n")
	want := []string{"a", "b", "", "c"}
	if len(lines) != len(want) {
		t.Fatalf("行数错误: %#v", lines)
	}
	for i := range want {
		if lines[i] != want[i] {
			t.Fatalf("第 %d 行错误: %q", i, lines[i])
		}
	}
	if got := splitOutput(""); len(got) != 0 {
		t.Fatalf("空输出应为空: %#v", got)
	}
}

func TestMatchPidTag(t *testing.T) {
	args := []string{"/usr/bin/java", "-Xmx1g", "-DJpom.application=ABC123", "-DJpom.basedir=/data/app", "-jar", "app.jar"}
	if !MatchPidTag(args, "ABC123") {
		t.Fatal("应匹配 -DJpom.application")
	}
	if MatchPidTag(args, "OTHER") {
		t.Fatal("不应匹配其他 id")
	}
	legacy := []string{"java", "-Jpom.application=OLD01"}
	if !MatchPidTag(legacy, "OLD01") {
		t.Fatal("应匹配旧标识 -Jpom.application")
	}
}

func TestParseLocalPort(t *testing.T) {
	cases := map[string]int{
		"0.0.0.0:8080":   8080,
		"*:2123":         2123,
		":::18000":       18000,
		"[::]:18000":     0, // Java 的本地端口解析只认可 0.0.0.0 / * / 无 IP 前缀
		"127.0.0.1:8080": 0, // 绑定具体 IP，Java 认为不是主端口
		"0.0.0.0:http":   0,
		"":               0,
	}
	for input, want := range cases {
		if got := parseLocalPort(input); got != want {
			t.Errorf("parseLocalPort(%q)=%d, want %d", input, got, want)
		}
	}
}

func TestStatusAndMainPort(t *testing.T) {
	if status := Status("jpom-go-agent-not-exist-tag"); status != StoppedTag {
		t.Fatalf("不存在的项目应返回 stopped: %s", status)
	}
	if port := MainPort(0); port != "-" {
		t.Fatalf("pid<=0 应返回 -: %s", port)
	}
	if ports := PortsByPid(os.Getpid()); ports == nil {
		// 允许当前测试进程没有监听端口，但不能 panic
		t.Log("当前测试进程没有监听端口")
	}
}

func TestTailLines(t *testing.T) {
	dir := t.TempDir()
	file := filepath.Join(dir, "console.log")
	content := "line1\nline2\nline3\nline4\n"
	if err := os.WriteFile(file, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
	lines, err := TailLines(file, 2)
	if err != nil {
		t.Fatal(err)
	}
	if len(lines) != 2 || lines[0] != "line3" || lines[1] != "line4" {
		t.Fatalf("TailLines 结果错误: %#v", lines)
	}
	all, err := TailLines(file, 10)
	if err != nil || len(all) != 4 {
		t.Fatalf("TailLines 全部读取错误: %#v err=%v", all, err)
	}

	read, next, total, err := ReadLinesFrom(file, 2, 1000)
	if err != nil {
		t.Fatal(err)
	}
	if total != 4 || next != 5 || len(read) != 3 || read[0] != "line2" || read[2] != "line4" {
		t.Fatalf("ReadLinesFrom 结果错误: read=%#v next=%d total=%d", read, next, total)
	}
	trimmed, _, _, err := ReadLinesFrom(file, 1, 2)
	if err != nil {
		t.Fatal(err)
	}
	if len(trimmed) != 2 || trimmed[0] != "line3" || trimmed[1] != "line4" {
		t.Fatalf("ReadLinesFrom 容量截断错误: %#v", trimmed)
	}
}

func TestExecAndMergeEnv(t *testing.T) {
	output := Exec("", map[string]string{"JPOM_TEST_ENV": "hello"}, "echo $JPOM_TEST_ENV")
	if strings.TrimSpace(output) != "hello" {
		t.Fatalf("Exec 环境变量未生效: %q", output)
	}
	code, lines, err := ExecCapture("", nil, "echo out; echo err 1>&2; exit 3")
	if err != nil || code != 3 {
		t.Fatalf("ExecCapture 退出码错误: code=%d err=%v", code, err)
	}
	if len(lines) != 2 || lines[0] != "out" || lines[1] != "err" {
		t.Fatalf("ExecCapture 输出错误: %#v", lines)
	}
	merged := MergeEnv(map[string]string{"JPOM_GO_AGENT_MERGE_TEST": "1"})
	found := false
	for _, item := range merged {
		if item == "JPOM_GO_AGENT_MERGE_TEST=1" {
			found = true
		}
	}
	if !found {
		t.Fatal("MergeEnv 未合并附加环境变量")
	}
}

func TestListProcesses(t *testing.T) {
	if items := ListProcesses("jpom-go-agent-not-exist-name", 5); len(items) != 0 {
		t.Fatalf("不存在的进程名应返回空: %#v", items)
	}
}

func TestJavaCommand(t *testing.T) {
	path, err := JavaCommand(false)
	if err != nil {
		if !strings.Contains(err.Error(), "未找到 java") {
			t.Fatalf("JavaCommand 错误信息不明确: %v", err)
		}
		t.Skip("当前测试环境没有 java，回退逻辑按预期返回明确错误")
	}
	if strings.TrimSpace(path) == "" {
		t.Fatal("JavaCommand 返回空路径")
	}
	if path != "java" && !isExecutableFile(path) {
		t.Fatalf("JavaCommand 返回的路径不可执行: %s", path)
	}
}
