//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package builder

import (
	"bufio"
	"os"
	"strconv"
	"strings"
	"testing"
)

// TestAntPathParity 校验 Go 侧 Ant 通配与 Spring AntPathMatcher 逐条一致。
//
// 基线文件由 dev-scripts/antpath-parity/ 生成（Java + spring-core 5.3.39）：
//
//	AntMatchGen < cases.tsv > java.out.tsv
//
// 任何一项不一致都意味着「同一份构建配置在 Server 与构建节点上收集到的产物不同」，
// 属于必须修掉的契约差异，不允许跳过。
func TestAntPathParity(t *testing.T) {
	file, err := os.Open("testdata/antpath_parity.tsv")
	if err != nil {
		t.Fatalf("open parity fixture: %v", err)
	}
	defer file.Close()
	scanner := bufio.NewScanner(file)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)
	total, failed := 0, 0
	lineNo := 0
	for scanner.Scan() {
		lineNo++
		line := scanner.Text()
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}
		fields := strings.Split(line, "\t")
		if len(fields) != 4 {
			t.Fatalf("line %d: want 4 fields, got %d: %q", lineNo, len(fields), line)
		}
		mode, pattern, path, wantText := fields[0], fields[1], fields[2], fields[3]
		want, err := strconv.ParseBool(wantText)
		if err != nil {
			t.Fatalf("line %d: bad expected value %q", lineNo, wantText)
		}
		var got bool
		switch mode {
		case "match":
			got = AntMatch(pattern, path)
		case "matchStart":
			got = AntMatchStart(pattern, path)
		case "isPattern":
			got = AntIsPattern(pattern)
		default:
			t.Fatalf("line %d: unknown mode %q", lineNo, mode)
		}
		total++
		if got != want {
			failed++
			if failed <= 20 {
				t.Errorf("%s(%q, %q) = %v, want %v", mode, pattern, path, got, want)
			}
		}
	}
	if err := scanner.Err(); err != nil {
		t.Fatalf("scan fixture: %v", err)
	}
	if total == 0 {
		t.Fatal("parity fixture is empty")
	}
	if failed > 0 {
		t.Fatalf("%d/%d parity cases failed", failed, total)
	}
	t.Logf("%d parity cases passed", total)
}

// TestAntMatchBasics 覆盖 Jpom 构建产物配置里最常见的几类写法（与 Server 端行为一致）。
func TestAntMatchBasics(t *testing.T) {
	// 期望值来自 Java 侧实测（spring-core 5.3.39）：注意 matchStart 对
	// "/dist" vs "/dist/app.php" 为 false —— 只有 pattern 里出现 ** 段时前缀匹配才成立，
	// 这正是 BuildExecuteManage 用 antSubMatch + "**" 组合 pattern 的原因。
	cases := []struct {
		pattern, path string
		match         bool
		matchStart    bool
	}{
		{"/target/*.jar", "/target/app.jar", true, true},
		{"/target/*.jar", "/target/a/b.jar", false, false},
		{"/target/**/*.jar", "/target/a/b.jar", true, true},
		{"/dist", "/dist", true, true},
		{"/dist", "/dist/app.php", false, false},
		{"/modules/server/target/*.jar", "/modules/server/target/server.jar", true, true},
		{"/target/app-?.jar", "/target/app-1.jar", true, true},
		{"/target/app-?.jar", "/target/app-12.jar", false, false},
		{"/lib/**", "/lib/a/b/c.jar", true, true},
		{"/lib/**", "/libx/a", false, false},
	}
	for _, c := range cases {
		if got := AntMatch(c.pattern, c.path); got != c.match {
			t.Errorf("AntMatch(%q, %q) = %v, want %v", c.pattern, c.path, got, c.match)
		}
		if got := AntMatchStart(c.pattern, c.path); got != c.matchStart {
			t.Errorf("AntMatchStart(%q, %q) = %v, want %v", c.pattern, c.path, got, c.matchStart)
		}
	}
}
