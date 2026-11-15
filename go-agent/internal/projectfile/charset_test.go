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
	"bytes"
	"os"
	"path/filepath"
	"testing"

	"golang.org/x/text/encoding/simplifiedchinese"
)

func TestGBKEditSuffixRoundTrip(t *testing.T) {
	service, base := testService(t)
	lib := createProject(t, service, base, "P1", "proj", nil)
	writeWhitelist(t, base, []string{base}, []string{"txt@GBK"})
	writeFile(t, filepath.Join(lib, "readme.txt"), "old")

	text := "中文内容"
	if err := service.UpdateConfigFile("P1", "", "readme.txt", text); err != nil {
		t.Fatalf("write GBK file: %v", err)
	}
	raw, err := os.ReadFile(filepath.Join(lib, "readme.txt"))
	if err != nil {
		t.Fatal(err)
	}
	decoded, err := simplifiedchinese.GBK.NewDecoder().Bytes(raw)
	if err != nil || string(decoded) != text {
		t.Fatalf("file should be written with GBK, raw=%v decoded=%q err=%v", raw, decoded, err)
	}
	content, err := service.ReadFile("P1", "", "readme.txt")
	if err != nil || content != text {
		t.Fatalf("read GBK file: %q %v", content, err)
	}
}

func TestUncompressGBKZipEntryName(t *testing.T) {
	source := t.TempDir()
	archivePath := filepath.Join(source, "gbk.zip")
	file, err := os.Create(archivePath)
	if err != nil {
		t.Fatal(err)
	}
	writer := zip.NewWriter(file)
	gbkName, err := simplifiedchinese.GBK.NewEncoder().Bytes([]byte("目录/文件.txt"))
	if err != nil {
		t.Fatal(err)
	}
	header := &zip.FileHeader{Name: string(gbkName), Method: zip.Deflate, NonUTF8: true}
	entry, err := writer.CreateHeader(header)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := entry.Write([]byte("content")); err != nil {
		t.Fatal(err)
	}
	if err := writer.Close(); err != nil {
		t.Fatal(err)
	}
	_ = file.Close()

	service := New(testApp(t))
	dest := filepath.Join(source, "extract")
	if err := service.Uncompress(archivePath, dest, 0); err != nil {
		t.Fatalf("uncompress GBK zip: %v", err)
	}
	if _, err := os.Stat(filepath.Join(dest, "目录", "文件.txt")); err != nil {
		t.Fatalf("GBK entry name should be decoded to UTF-8: %v", err)
	}
}

func TestTarGzMagicDetection(t *testing.T) {
	source := t.TempDir()
	project := filepath.Join(source, "app")
	writeFile(t, filepath.Join(project, "a.txt"), "a")
	archivePath := filepath.Join(source, "archive.bin")
	if err := Compress(project, archivePath, "tar.gz"); err != nil {
		t.Fatal(err)
	}
	if kind := archiveKind(archivePath); kind != "tar.gz" {
		t.Fatalf("unexpected archive kind: %s", kind)
	}
	var zipBuffer bytes.Buffer
	zipWriter := zip.NewWriter(&zipBuffer)
	if _, err := zipWriter.Create("a.txt"); err != nil {
		t.Fatal(err)
	}
	if err := zipWriter.Close(); err != nil {
		t.Fatal(err)
	}
	zipPath := filepath.Join(source, "archive.dat")
	if err := os.WriteFile(zipPath, zipBuffer.Bytes(), 0o644); err != nil {
		t.Fatal(err)
	}
	if kind := archiveKind(zipPath); kind != "zip" {
		t.Fatalf("zip magic not detected: %s", kind)
	}
}
