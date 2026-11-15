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
	"archive/zip"
	"os"
	"path/filepath"
	"testing"
)

func TestCompressAndUncompressZip(t *testing.T) {
	source := t.TempDir()
	project := filepath.Join(source, "app")
	writeFile(t, filepath.Join(project, "index.html"), "<html></html>")
	writeFile(t, filepath.Join(project, "conf", "app.yml"), "name: demo")

	archivePath := filepath.Join(source, "app.zip")
	if err := Compress(project, archivePath, "zip"); err != nil {
		t.Fatalf("compress zip: %v", err)
	}
	reader, err := zip.OpenReader(archivePath)
	if err != nil {
		t.Fatalf("open zip: %v", err)
	}
	names := map[string]bool{}
	for _, file := range reader.File {
		names[file.Name] = true
	}
	_ = reader.Close()
	if !names["app/"] || !names["app/index.html"] || !names["app/conf/app.yml"] {
		t.Fatalf("archive must contain the source directory itself, got %v", names)
	}

	dest := filepath.Join(source, "extract")
	if err := New(testApp(t)).Uncompress(archivePath, dest, 1); err != nil {
		t.Fatalf("uncompress zip: %v", err)
	}
	content, err := os.ReadFile(filepath.Join(dest, "conf", "app.yml"))
	if err != nil || string(content) != "name: demo" {
		t.Fatalf("unexpected extracted content: %q %v", content, err)
	}
}

func TestCompressAndUncompressTarGz(t *testing.T) {
	source := t.TempDir()
	project := filepath.Join(source, "app")
	writeFile(t, filepath.Join(project, "bin", "start.sh"), "#!/bin/sh\necho hi\n")

	archivePath := filepath.Join(source, "app.tar.gz")
	if err := Compress(project, archivePath, "tar.gz"); err != nil {
		t.Fatalf("compress tar.gz: %v", err)
	}
	service := New(testApp(t))
	dest := filepath.Join(source, "extract")
	if err := service.Uncompress(archivePath, dest, 0); err != nil {
		t.Fatalf("uncompress tar.gz: %v", err)
	}
	if _, err := os.Stat(filepath.Join(dest, "app", "bin", "start.sh")); err != nil {
		t.Fatalf("expected app/bin/start.sh: %v", err)
	}
}

func TestUncompressRejectsTraversalEntry(t *testing.T) {
	source := t.TempDir()
	archivePath := filepath.Join(source, "evil.zip")
	file, err := os.Create(archivePath)
	if err != nil {
		t.Fatal(err)
	}
	writer := zip.NewWriter(file)
	entry, err := writer.Create("../evil.txt")
	if err != nil {
		t.Fatal(err)
	}
	if _, err := entry.Write([]byte("evil")); err != nil {
		t.Fatal(err)
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	_ = file.Close()

	service := New(testApp(t))
	dest := filepath.Join(source, "extract")
	if err := service.Uncompress(archivePath, dest, 0); err == nil {
		t.Fatal("expected traversal archive entry to be rejected")
	}
	if exists(filepath.Join(filepath.Dir(source), "evil.txt")) {
		t.Fatal("archive must not write outside the destination")
	}
}
