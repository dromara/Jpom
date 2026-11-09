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
	"crypto/md5"
	"crypto/sha1"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
)

// secureJoin joins client supplied path fragments under base and refuses
// absolute paths, ".." traversal and symbolic links that escape the base
// directory. A fragment equal to "/" is a no-op, because the Java controllers
// rely on File(parent, "/") returning the parent directory itself.
func secureJoin(base string, parts ...string) (string, error) {
	baseAbs, err := filepath.Abs(base)
	if err != nil {
		return "", err
	}
	baseAbs = filepath.Clean(baseAbs)
	target := baseAbs
	for _, part := range parts {
		if strings.IndexByte(part, 0) >= 0 {
			return "", invalidMessage("Illegal path")
		}
		cleaned := strings.TrimLeft(strings.ReplaceAll(part, "\\", "/"), "/")
		if cleaned == "" {
			continue
		}
		target = filepath.Join(target, cleaned)
	}
	if !isSubPath(baseAbs, target) {
		return "", invalidMessage("Illegal path outside the project directory")
	}
	// Reject symlinks (including symlinked parents) that point outside base.
	baseReal := resolveRealPath(baseAbs)
	targetReal := resolveRealPath(target)
	if !isSubPath(baseReal, targetReal) {
		return "", invalidMessage("Illegal path: symbolic link escapes the project directory")
	}
	return target, nil
}

// isSubPath reports whether target is base or lives inside base.
func isSubPath(base, target string) bool {
	rel, err := filepath.Rel(base, target)
	if err != nil {
		return false
	}
	if rel == "." {
		return true
	}
	return rel != ".." && !strings.HasPrefix(rel, ".."+string(filepath.Separator))
}

// resolveRealPath resolves the symbolic links of the deepest existing ancestor
// of path and re-appends the not yet existing tail, so it can be used on files
// that are about to be created.
func resolveRealPath(path string) string {
	abs, err := filepath.Abs(path)
	if err != nil {
		return filepath.Clean(path)
	}
	current := filepath.Clean(abs)
	suffix := make([]string, 0, 4)
	for {
		if resolved, err := filepath.EvalSymlinks(current); err == nil {
			for i := len(suffix) - 1; i >= 0; i-- {
				resolved = filepath.Join(resolved, suffix[i])
			}
			return resolved
		}
		parent := filepath.Dir(current)
		if parent == current {
			return abs
		}
		suffix = append(suffix, filepath.Base(current))
		current = parent
	}
}

// extName returns the extension of a name without the dot, mirroring
// cn.hutool.core.io.FileUtil#extName.
func extName(name string) string {
	base := filepath.Base(name)
	if index := strings.LastIndexByte(base, '.'); index >= 0 && index < len(base)-1 {
		return base[index+1:]
	}
	return ""
}

// mainName returns the name without its last extension, mirroring
// cn.hutool.core.io.FileUtil#mainName.
func mainName(name string) string {
	base := filepath.Base(name)
	if index := strings.LastIndexByte(base, '.'); index > 0 {
		return base[:index]
	}
	return base
}

// trimLastExt removes the last ".xxx" part of a file name (StrUtil.subBefore(name, ".", true)).
func trimLastExt(name string) string {
	if index := strings.LastIndexByte(name, '.'); index >= 0 {
		return name[:index]
	}
	return name
}

// copyFile copies src to dst, creating parent directories. When override is
// false an existing destination is left untouched.
func copyFile(src, dst string, override bool) error {
	stat, err := os.Stat(src)
	if err != nil {
		return err
	}
	if stat.IsDir() {
		return copyContent(src, dst, override)
	}
	if !override && exists(dst) {
		return nil
	}
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return err
	}
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer func() { _ = in.Close() }()
	out, err := os.OpenFile(dst, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, stat.Mode().Perm())
	if err != nil {
		return err
	}
	if _, err := io.Copy(out, in); err != nil {
		_ = out.Close()
		return err
	}
	return out.Close()
}

// copyContent copies the content of src into dst (FileUtil#copyContent): a
// directory is merged into dst, a file is copied into the dst directory.
func copyContent(src, dst string, override bool) error {
	stat, err := os.Stat(src)
	if err != nil {
		return err
	}
	if !stat.IsDir() {
		target := dst
		if isDir(dst) {
			target = filepath.Join(dst, filepath.Base(src))
		}
		return copyFile(src, target, override)
	}
	if err := os.MkdirAll(dst, 0o755); err != nil {
		return err
	}
	entries, err := os.ReadDir(src)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		from := filepath.Join(src, entry.Name())
		to := filepath.Join(dst, entry.Name())
		if entry.IsDir() {
			if err := copyContent(from, to, override); err != nil {
				return err
			}
			continue
		}
		if err := copyFile(from, to, override); err != nil {
			return err
		}
	}
	return nil
}

// moveContent moves every child of src into dst, overriding existing files, and
// removes the source (FileUtil#moveContent with isOverride=true).
func moveContent(src, dst string, override bool) error {
	entries, err := os.ReadDir(src)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(dst, 0o755); err != nil {
		return err
	}
	for _, entry := range entries {
		from := filepath.Join(src, entry.Name())
		to := filepath.Join(dst, entry.Name())
		if err := movePath(from, to, override); err != nil {
			return err
		}
	}
	return os.RemoveAll(src)
}

// movePath moves a file or directory to dst (rename when possible, copy and
// delete across devices).
func movePath(src, dst string, override bool) error {
	if !override && exists(dst) {
		return nil
	}
	if err := os.MkdirAll(filepath.Dir(dst), 0o755); err != nil {
		return err
	}
	if exists(dst) {
		if err := os.RemoveAll(dst); err != nil {
			return err
		}
	}
	stat, err := os.Stat(src)
	if err != nil {
		return err
	}
	if stat.IsDir() {
		if err := copyContent(src, dst, true); err != nil {
			return err
		}
		return os.RemoveAll(src)
	}
	if err := os.Rename(src, dst); err == nil {
		return nil
	}
	if err := copyFile(src, dst, true); err != nil {
		return err
	}
	return os.Remove(src)
}

// moveIntoDir moves a file into a directory, overriding an existing file with
// the same name (FileUtil#move(file, dir, true)).
func moveIntoDir(src, dir string) (string, error) {
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", err
	}
	dst := filepath.Join(dir, filepath.Base(src))
	if err := movePath(src, dst, true); err != nil {
		return "", err
	}
	return dst, nil
}

// cleanDir removes every child of dir while keeping dir itself
// (FileUtil#clean); a non existing path counts as successfully cleaned.
func cleanDir(dir string) error {
	path := filepath.Clean(dir)
	stat, err := os.Lstat(path)
	if err != nil {
		if os.IsNotExist(err) {
			return nil
		}
		return err
	}
	if !stat.IsDir() {
		return os.RemoveAll(path)
	}
	entries, err := os.ReadDir(path)
	if err != nil {
		return err
	}
	for _, entry := range entries {
		if err := os.RemoveAll(filepath.Join(path, entry.Name())); err != nil {
			return err
		}
	}
	return nil
}

// fileSHA1 returns the lower case hex sha1 of a file (SecureUtil#sha1(File)).
func fileSHA1(path string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer func() { _ = file.Close() }()
	hash := sha1.New()
	if _, err := io.Copy(hash, file); err != nil {
		return "", err
	}
	return hex.EncodeToString(hash.Sum(nil)), nil
}

// fileMD5 returns the lower case hex md5 of a file (SecureUtil#md5(File)).
func fileMD5(path string) (string, error) {
	file, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer func() { _ = file.Close() }()
	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return "", err
	}
	return hex.EncodeToString(hash.Sum(nil)), nil
}

// readableSize renders a byte count the same way FileUtil#readableFileSize does
// (1024 based units, two decimals trimmed).
func readableSize(size int64) string {
	const unit = 1024
	if size < unit {
		return fmt.Sprintf("%d B", size)
	}
	units := []string{"KB", "MB", "GB", "TB", "PB", "EB"}
	value := float64(size)
	index := -1
	for value >= unit && index < len(units)-1 {
		value /= unit
		index++
	}
	text := fmt.Sprintf("%.2f", value)
	text = strings.TrimRight(strings.TrimRight(text, "0"), ".")
	return text + " " + units[index]
}
