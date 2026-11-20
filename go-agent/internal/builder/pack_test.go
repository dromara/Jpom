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
	"archive/zip"
	"os"
	"path/filepath"
	"sort"
	"testing"
)

// writeTestFile 在 root 下创建文件（自动建目录）。
func writeTestFile(t *testing.T, root, rel, content string) {
	t.Helper()
	path := filepath.Join(root, filepath.FromSlash(rel))
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		t.Fatalf("mkdir %s: %v", path, err)
	}
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatalf("write %s: %v", path, err)
	}
}

// entryNames 返回排序后的 zip 条目名，便于断言。
func entryNames(entries []PackEntry) []string {
	names := make([]string, 0, len(entries))
	for _, entry := range entries {
		names = append(names, entry.Name)
	}
	sort.Strings(names)
	return names
}

func assertNames(t *testing.T, got []string, want ...string) {
	t.Helper()
	sort.Strings(want)
	if len(got) != len(want) {
		t.Fatalf("条目数不一致: got=%v want=%v", got, want)
	}
	for index := range want {
		if got[index] != want[index] {
			t.Fatalf("条目不一致: got=%v want=%v", got, want)
		}
	}
}

// TestParseResultDirFile 覆盖 ResultDirFileAction 的解析规则。
func TestParseResultDirFile(t *testing.T) {
	cases := []struct {
		input string
		typ   PathMode
		mode  UploadMode
		path  string
		sub   string
	}{
		{"target/app.jar", PathOriginal, "", "target/app.jar", ""},
		{"dist", PathOriginal, "", "dist", ""},
		{"**/*.jar", PathAnt, KeepDir, "**/*.jar", ""},
		{"**/*.jar:SAME_DIR", PathAnt, SameDir, "**/*.jar", ""},
		{"**/*.jar:keep_dir", PathAnt, KeepDir, "**/*.jar", ""},
		{"**/*.jar:ILLEGAL", PathAnt, KeepDir, "**/*.jar", ""},
		{"**/*.jar:KEEP_DIR:lib", PathAnt, KeepDir, "**/*.jar", "lib"},
		{"target/app.jar:KEEP_DIR:lib", PathOriginal, "", "target/app.jar", ""},
	}
	for _, item := range cases {
		action := ParseResultDirFile(item.input)
		if action.Type != item.typ || action.Mode != item.mode || action.Path != item.path || action.SubMatch != item.sub {
			t.Errorf("%s -> %+v, want type=%s mode=%s path=%s sub=%s", item.input, action, item.typ, item.mode, item.path, item.sub)
		}
	}
	if sub := ParseResultDirFile("**/*.jar:KEEP_DIR:lib").AntSubMatch(); sub != "/lib/" {
		t.Errorf("antSubMatch = %q, want /lib/", sub)
	}
	if sub := ParseResultDirFile("**/*.jar:KEEP_DIR:/lib/").AntSubMatch(); sub != "/lib/" {
		t.Errorf("antSubMatch = %q, want /lib/", sub)
	}
	if sub := ParseResultDirFile("**/*.jar").AntSubMatch(); sub != "" {
		t.Errorf("antSubMatch = %q, want empty", sub)
	}
}

// TestCollectKeepDir 覆盖 ANT + KEEP_DIR：条目名 = 命中路径去掉 antSubMatch 前缀后的相对路径。
func TestCollectKeepDir(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "lib/a/b.jar", "b")
	writeTestFile(t, source, "lib/c.txt", "c")
	writeTestFile(t, source, "target/app.jar", "app")

	result, err := Collect(source, PackOptions{ResultDirFile: "**/*.jar:KEEP_DIR:lib"})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	// /lib/a/b.jar 命中二级目录 /lib/ -> a/b.jar；/target/app.jar 不满足二级匹配被丢弃。
	assertNames(t, entryNames(result.Entries), "a/b.jar")
	if result.Matched != 2 {
		t.Errorf("Matched = %d, want 2（/lib/a/b.jar 与 /target/app.jar）", result.Matched)
	}
}

// TestCollectSameDir 覆盖 ANT + SAME_DIR：条目名 = basename（全部摊平）。
func TestCollectSameDir(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "lib/a/b.jar", "b")
	writeTestFile(t, source, "target/app.jar", "app")

	result, err := Collect(source, PackOptions{ResultDirFile: "**/*.jar:SAME_DIR"})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries), "app.jar", "b.jar")
}

// TestCollectKeepDirWithoutSubMatch 保留完整层级（antSubMatch 为空时 notMathIndex = 0）。
func TestCollectKeepDirWithoutSubMatch(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "lib/a/b.jar", "b")

	result, err := Collect(source, PackOptions{ResultDirFile: "**/*.jar"})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries), "lib/a/b.jar")
}

// TestCollectExclude 覆盖 excludeReleaseAnt（与 Server 同为「原规则直接 match /相对路径」）。
func TestCollectExclude(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "docs/readme.md", "md")
	writeTestFile(t, source, "docs/keep.txt", "txt")

	// 用只命中文件（不命中目录）的通配，才能观察到 excludeReleaseAnt 的效果：
	// Server 的 predicate 只作用于「命中的那一层路径」，目录整体拷贝时不逐文件过滤。
	result, err := Collect(source, PackOptions{
		ResultDirFile:     "docs/*",
		ExcludeReleaseAnt: "/**/*.md",
	})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries), "docs/keep.txt")
	if result.Excluded != 1 {
		t.Errorf("Excluded = %d, want 1", result.Excluded)
	}

	// 与 Server 一致的反向证据：不带前导 "/" 的 "*.md" 不会命中 "/docs/readme.md"。
	result, err = Collect(source, PackOptions{
		ResultDirFile:     "docs/*",
		ExcludeReleaseAnt: "*.md",
	})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries), "docs/keep.txt", "docs/readme.md")
}

// TestCollectHideFile 覆盖 releaseHideFile：默认跳过隐藏文件，开启后保留。
func TestCollectHideFile(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "dist/index.html", "html")
	writeTestFile(t, source, "dist/.env", "secret")

	result, err := Collect(source, PackOptions{ResultDirFile: "dist"})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries), "dist/index.html")

	result, err = Collect(source, PackOptions{ResultDirFile: "dist", ReleaseHideFile: true})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries), "dist/.env", "dist/index.html")
}

// TestCollectOriginal 覆盖 ORIGINAL：条目名 = resultDirFile 本身（目录则内容挂在该前缀下）。
func TestCollectOriginal(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "target/app.jar", "app")
	writeTestFile(t, source, "dist/index.html", "html")
	writeTestFile(t, source, "dist/assets/app.js", "js")

	fileResult, err := Collect(source, PackOptions{ResultDirFile: "target/app.jar"})
	if err != nil {
		t.Fatalf("Collect file: %v", err)
	}
	assertNames(t, entryNames(fileResult.Entries), "target/app.jar")

	dirResult, err := Collect(source, PackOptions{ResultDirFile: "dist"})
	if err != nil {
		t.Fatalf("Collect dir: %v", err)
	}
	assertNames(t, entryNames(dirResult.Entries), "dist/assets/app.js", "dist/index.html")
}

// TestCollectMatchedDirectory 覆盖「目录也参与匹配」：命中目录时其内容整体收集。
func TestCollectMatchedDirectory(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "modules/server/A.java", "a")
	writeTestFile(t, source, "modules/server/nested/B.java", "b")
	writeTestFile(t, source, "modules/other/C.java", "c")

	result, err := Collect(source, PackOptions{ResultDirFile: "modules/server"})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries),
		"modules/server/A.java", "modules/server/nested/B.java")

	// ANT 模式命中目录：条目名按目录名 + 相对路径拼接（目录参与匹配）。
	result, err = Collect(source, PackOptions{ResultDirFile: "modules/*"})
	if err != nil {
		t.Fatalf("Collect: %v", err)
	}
	assertNames(t, entryNames(result.Entries),
		"modules/other/C.java", "modules/server/A.java", "modules/server/nested/B.java")
}

// TestCollectNoMatch 覆盖没有命中产物的报错。
func TestCollectNoMatch(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "target/app.jar", "app")
	if _, err := Collect(source, PackOptions{ResultDirFile: "**/*.zip"}); err == nil {
		t.Fatal("期望没有匹配产物时报错")
	}
	if _, err := Collect(source, PackOptions{ResultDirFile: "dist"}); err == nil {
		t.Fatal("期望产物路径不存在时报错")
	}
}

// TestZipEntries 覆盖 zip 生成：条目名、大小与 sha1。
func TestZipEntries(t *testing.T) {
	source := t.TempDir()
	writeTestFile(t, source, "dist/index.html", "hello")
	target := filepath.Join(t.TempDir(), "history", "#1", "zip")

	entries := []PackEntry{{Source: filepath.Join(source, "dist", "index.html"), Name: "dist/index.html"}}
	size, digest, err := Zip(entries, target)
	if err != nil {
		t.Fatalf("Zip: %v", err)
	}
	if size <= 0 || len(digest) != 40 {
		t.Fatalf("size=%d sha1=%q", size, digest)
	}
	reader, err := zip.OpenReader(target)
	if err != nil {
		t.Fatalf("open zip: %v", err)
	}
	defer func() { _ = reader.Close() }()
	if len(reader.File) != 1 || reader.File[0].Name != "dist/index.html" {
		t.Fatalf("zip 条目异常: %+v", reader.File)
	}
}
