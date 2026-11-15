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
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// fixture7z is a real 7z archive generated with Apache commons-compress (see the
// repository notes for the generator command); it contains:
//
//	pkg/            (directory)
//	pkg/sub/        (directory)
//	pkg/a.txt       "alpha"
//	pkg/sub/b.txt   "beta"
//	中文.txt         "zh"
//	empty/          (empty directory)
func fixture7z(name string) string {
	return filepath.Join("testdata", name)
}

func TestUncompressSevenZip(t *testing.T) {
	service := New(testApp(t))
	source := fixture7z("sample.7z")
	if kind := archiveKind(source); kind != "7z" {
		t.Fatalf("unexpected archive kind: %s", kind)
	}
	dest := filepath.Join(t.TempDir(), "extract")
	if err := service.Uncompress(source, dest, 0); err != nil {
		t.Fatalf("uncompress 7z: %v", err)
	}
	cases := map[string]string{
		filepath.Join("pkg", "a.txt"):        "alpha",
		filepath.Join("pkg", "sub", "b.txt"): "beta",
		"中文.txt":                             "zh",
	}
	for name, want := range cases {
		raw, err := os.ReadFile(filepath.Join(dest, name))
		if err != nil || string(raw) != want {
			t.Fatalf("unexpected %s: %q %v", name, raw, err)
		}
	}
	if !isDir(filepath.Join(dest, "empty")) {
		t.Fatal("empty directory should be created")
	}

	// stripComponents=1 drops the first path segment ("pkg/"), a single segment
	// entry (中文.txt) is skipped exactly like hutool Extractor#stripName.
	stripped := filepath.Join(t.TempDir(), "stripped")
	if err := service.Uncompress(source, stripped, 1); err != nil {
		t.Fatalf("uncompress 7z with stripComponents: %v", err)
	}
	if raw, err := os.ReadFile(filepath.Join(stripped, "a.txt")); err != nil || string(raw) != "alpha" {
		t.Fatalf("stripped a.txt: %q %v", raw, err)
	}
	if raw, err := os.ReadFile(filepath.Join(stripped, "sub", "b.txt")); err != nil || string(raw) != "beta" {
		t.Fatalf("stripped sub/b.txt: %q %v", raw, err)
	}
	if exists(filepath.Join(stripped, "中文.txt")) {
		t.Fatal("single segment entry must be skipped when stripping one level")
	}
}

func TestUncompressSevenZipRejectsTraversal(t *testing.T) {
	service := New(testApp(t))
	source := fixture7z("evil-traversal.7z")
	dest := filepath.Join(t.TempDir(), "extract")
	err := service.Uncompress(source, dest, 0)
	if err == nil || !strings.Contains(err.Error(), "Illegal archive entry path") {
		t.Fatalf("expected traversal rejection, got %v", err)
	}
	if exists(filepath.Join(filepath.Dir(dest), "evil.txt")) {
		t.Fatal("archive must not write outside the destination")
	}
}

func TestUncompressSevenZipSkipsSymlink(t *testing.T) {
	service := New(testApp(t))
	source := fixture7z("evil-symlink.7z")
	dest := filepath.Join(t.TempDir(), "extract")
	if err := service.Uncompress(source, dest, 0); err != nil {
		t.Fatalf("symbolic link entry should be skipped, got %v", err)
	}
	if exists(filepath.Join(dest, "link")) {
		t.Fatal("symbolic link entry must not be created")
	}
}
