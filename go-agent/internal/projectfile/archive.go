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
	"archive/tar"
	"archive/zip"
	"compress/gzip"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"unicode/utf8"
)

// archiveKind detects the archive format from the file extension and the file
// magic, mirroring the heuristics of cn.hutool.extra.compress.
func archiveKind(path string) string {
	lower := strings.ToLower(filepath.Base(path))
	switch {
	case strings.HasSuffix(lower, ".zip"):
		return "zip"
	case strings.HasSuffix(lower, ".tar.gz"), strings.HasSuffix(lower, ".tgz"):
		return "tar.gz"
	case strings.HasSuffix(lower, ".tar"):
		return "tar"
	case strings.HasSuffix(lower, ".gz"):
		return "gz"
	case strings.HasSuffix(lower, ".7z"):
		return "7z"
	}
	file, err := os.Open(path)
	if err != nil {
		return ""
	}
	defer func() { _ = file.Close() }()
	head := make([]byte, 512)
	count, _ := io.ReadFull(file, head)
	head = head[:count]
	if len(head) >= 4 && head[0] == 'P' && head[1] == 'K' {
		return "zip"
	}
	if len(head) >= 2 && head[0] == 0x1f && head[1] == 0x8b {
		return "tar.gz"
	}
	if len(head) >= 6 && string(head[:6]) == "7z\xbc\xaf\x27\x1c" {
		return "7z"
	}
	if len(head) >= 263 && string(head[257:262]) == "ustar" {
		return "tar"
	}
	return ""
}

// decodeArchiveName converts an archive entry name to UTF-8. ZIP files created
// on Windows may store GBK encoded names without the UTF-8 flag; when the GBK
// codec is available it is used as fallback, matching CompressionFileUtil which
// retries GBK before UTF-8.
func decodeArchiveName(name string) string {
	if utf8.ValidString(name) {
		return name
	}
	if gbkDecoder != nil {
		if decoded, err := gbkDecoder([]byte(name)); err == nil {
			return decoded
		}
	}
	return strings.ToValidUTF8(name, "_")
}

// stripEntryPath applies tar --strip-components style processing to an archive
// entry name. It returns false when the entry must be skipped.
func stripEntryPath(name string, stripComponents int) (string, bool, error) {
	name = strings.ReplaceAll(name, "\\", "/")
	parts := make([]string, 0, 8)
	for _, part := range strings.Split(name, "/") {
		if part == "" || part == "." {
			continue
		}
		if part == ".." {
			return "", false, invalidf("非法的压缩文件路径:%s", name)
		}
		parts = append(parts, part)
	}
	if len(parts) == 0 {
		return "", false, nil
	}
	if stripComponents > 0 {
		if len(parts) <= stripComponents {
			return "", false, nil
		}
		parts = parts[stripComponents:]
	}
	return strings.Join(parts, "/"), true, nil
}

// writeArchiveEntry writes one extracted entry below destDir.
func writeArchiveEntry(destDir, relative string, mode os.FileMode, body io.Reader) error {
	target, err := secureJoin(destDir, relative)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return err
	}
	if mode == 0 {
		mode = 0o644
	}
	out, err := os.OpenFile(target, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, mode.Perm())
	if err != nil {
		return err
	}
	if body != nil {
		if _, err := io.Copy(out, body); err != nil {
			_ = out.Close()
			return err
		}
	}
	return out.Close()
}

// extractZip extracts a zip archive into destDir.
func extractZip(archivePath, destDir string, stripComponents int) error {
	reader, err := zip.OpenReader(archivePath)
	if err != nil {
		return err
	}
	defer func() { _ = reader.Close() }()
	if err := os.MkdirAll(destDir, 0o755); err != nil {
		return err
	}
	for _, file := range reader.File {
		name := decodeArchiveName(file.Name)
		relative, keep, err := stripEntryPath(name, stripComponents)
		if err != nil {
			return err
		}
		if !keep {
			continue
		}
		if file.FileInfo().IsDir() {
			target, err := secureJoin(destDir, relative)
			if err != nil {
				return err
			}
			if err := os.MkdirAll(target, 0o755); err != nil {
				return err
			}
			continue
		}
		body, err := file.Open()
		if err != nil {
			return err
		}
		err = writeArchiveEntry(destDir, relative, file.Mode(), body)
		_ = body.Close()
		if err != nil {
			return err
		}
	}
	return nil
}

// extractTar extracts a tar (optionally gzip compressed) archive into destDir.
func extractTar(archivePath, destDir string, stripComponents int, compressed bool) error {
	file, err := os.Open(archivePath)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	var reader io.Reader = file
	if compressed {
		gzipReader, err := gzip.NewReader(file)
		if err != nil {
			return err
		}
		defer func() { _ = gzipReader.Close() }()
		reader = gzipReader
	}
	tarReader := tar.NewReader(reader)
	if err := os.MkdirAll(destDir, 0o755); err != nil {
		return err
	}
	for {
		header, err := tarReader.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		relative, keep, err := stripEntryPath(header.Name, stripComponents)
		if err != nil {
			return err
		}
		if !keep {
			continue
		}
		mode := os.FileMode(header.Mode)
		if header.Typeflag == tar.TypeDir {
			target, err := secureJoin(destDir, relative)
			if err != nil {
				return err
			}
			if err := os.MkdirAll(target, 0o755); err != nil {
				return err
			}
			continue
		}
		if header.Typeflag != tar.TypeReg && header.Typeflag != tar.TypeRegA {
			// Symbolic links and devices are skipped so an archive can never
			// escape the destination directory.
			continue
		}
		if err := writeArchiveEntry(destDir, relative, mode, tarReader); err != nil {
			return err
		}
	}
	return nil
}

// extractGzip extracts a single gzip compressed file into destDir, keeping the
// name without the .gz suffix.
func extractGzip(archivePath, destDir string, stripComponents int) error {
	name := strings.TrimSuffix(filepath.Base(archivePath), filepath.Ext(archivePath))
	if name == "" {
		name = "file"
	}
	relative, keep, err := stripEntryPath(name, stripComponents)
	if err != nil {
		return err
	}
	if !keep {
		return nil
	}
	file, err := os.Open(archivePath)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	gzipReader, err := gzip.NewReader(file)
	if err != nil {
		return err
	}
	defer func() { _ = gzipReader.Close() }()
	return writeArchiveEntry(destDir, relative, 0o644, gzipReader)
}

// Uncompress extracts archivePath into destDir, dropping stripComponents path
// levels (CompressionFileUtil#unCompress).
func (s *Service) Uncompress(archivePath, destDir string, stripComponents int) error {
	if stripComponents < 0 {
		stripComponents = 0
	}
	if err := os.MkdirAll(destDir, 0o755); err != nil {
		return fmt.Errorf("创建解压目录失败:%w", err)
	}
	switch archiveKind(archivePath) {
	case "zip":
		return extractZip(archivePath, destDir, stripComponents)
	case "tar":
		return extractTar(archivePath, destDir, stripComponents, false)
	case "tar.gz":
		return extractTar(archivePath, destDir, stripComponents, true)
	case "gz":
		return extractGzip(archivePath, destDir, stripComponents)
	case "7z":
		return fmt.Errorf("暂不支持解压 7z 文件")
	default:
		return fmt.Errorf("不支持的文件类型:%s", filepath.Ext(archivePath))
	}
}

// Compress creates a zip/tar/tar.gz archive of sourceDir at targetPath. The
// archive contains the source directory itself as root entry, same as
// CompressUtil.createArchiver(...).add(dir) in the Java agent.
func Compress(sourceDir, targetPath, kind string) error {
	sourceDir = filepath.Clean(sourceDir)
	rootName := filepath.Base(sourceDir)
	if err := os.MkdirAll(filepath.Dir(targetPath), 0o755); err != nil {
		return err
	}
	out, err := os.Create(targetPath)
	if err != nil {
		return err
	}
	finish := func(closeErr error) error {
		if closeErr != nil {
			_ = out.Close()
			return closeErr
		}
		return out.Close()
	}
	switch kind {
	case "zip":
		writer := zip.NewWriter(out)
		err = walkArchive(sourceDir, rootName, func(name, filePath string, info os.FileInfo) error {
			header, err := zip.FileInfoHeader(info)
			if err != nil {
				return err
			}
			header.Name = name
			if info.IsDir() {
				header.Name = strings.TrimSuffix(name, "/") + "/"
				header.Method = zip.Store
			} else {
				header.Method = zip.Deflate
			}
			entry, err := writer.CreateHeader(header)
			if err != nil {
				return err
			}
			if info.IsDir() {
				return nil
			}
			return copyFileToWriter(entry, filePath)
		})
		if err != nil {
			return finish(err)
		}
		return finish(writer.Close())
	case "tar", "tar.gz":
		var writer io.Writer = out
		var gzipWriter *gzip.Writer
		if kind == "tar.gz" {
			gzipWriter = gzip.NewWriter(out)
			writer = gzipWriter
		}
		tarWriter := tar.NewWriter(writer)
		err = walkArchive(sourceDir, rootName, func(name, filePath string, info os.FileInfo) error {
			header, err := tar.FileInfoHeader(info, "")
			if err != nil {
				return err
			}
			header.Name = name
			if info.IsDir() {
				header.Name = strings.TrimSuffix(name, "/") + "/"
			}
			if err := tarWriter.WriteHeader(header); err != nil {
				return err
			}
			if info.IsDir() {
				return nil
			}
			return copyFileToWriter(tarWriter, filePath)
		})
		if err != nil {
			_ = tarWriter.Close()
			if gzipWriter != nil {
				_ = gzipWriter.Close()
			}
			return finish(err)
		}
		if err := tarWriter.Close(); err != nil {
			return finish(err)
		}
		if gzipWriter != nil {
			return finish(gzipWriter.Close())
		}
		return finish(nil)
	default:
		_ = out.Close()
		return invalidf("不支持的压缩类型,%s", kind)
	}
}

// copyFileToWriter streams a file into an archive writer without loading it into
// memory (project directories may contain large jars).
func copyFileToWriter(writer io.Writer, filePath string) error {
	file, err := os.Open(filePath)
	if err != nil {
		return err
	}
	defer func() { _ = file.Close() }()
	_, err = io.Copy(writer, file)
	return err
}

// walkArchive walks sourceDir and calls fn with the archive entry name (rootName
// is the archive root directory), the file path and the file info.
func walkArchive(sourceDir, rootName string, fn func(name, filePath string, info os.FileInfo) error) error {
	rootInfo, err := os.Stat(sourceDir)
	if err != nil {
		return err
	}
	if err := fn(rootName+"/", "", rootInfo); err != nil {
		return err
	}
	return filepath.Walk(sourceDir, func(path string, info os.FileInfo, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		if path == sourceDir {
			return nil
		}
		relative, err := filepath.Rel(sourceDir, path)
		if err != nil {
			return err
		}
		name := rootName + "/" + filepath.ToSlash(relative)
		if info.IsDir() {
			name += "/"
		} else if !info.Mode().IsRegular() {
			return nil
		}
		return fn(name, path, info)
	})
}
