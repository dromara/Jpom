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
	"crypto/sha1"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"io/fs"
	"os"
	"path"
	"path/filepath"
	"strings"
)

// UploadMode 对应 Server ResultDirFileAction.AntFileUploadMode。
type UploadMode string

const (
	// KeepDir 保留文件夹层级。
	KeepDir UploadMode = "KEEP_DIR"
	// SameDir 把所有文件合并到同一个文件夹（摊平）。
	SameDir UploadMode = "SAME_DIR"
)

// PathMode 对应 Server ResultDirFileAction.Type。
type PathMode string

const (
	// PathAnt 通配模式（Ant 表达式）。
	PathAnt PathMode = "ANT_PATH"
	// PathOriginal 原始路径模式。
	PathOriginal PathMode = "ORIGINAL"
)

// ErrNoMatch 表示产物配置没有命中任何文件（Server 同样按失败处理）。
var ErrNoMatch = errors.New("no matching build product")

// ResultDirFile 是 Server ResultDirFileAction 的解析结果（语法 path 或 path:MODE:SUB）。
type ResultDirFile struct {
	Path string
	Type PathMode
	Mode UploadMode
	// SubMatch 是 ant 二级目录（配置原值，未包裹 "/"）。
	SubMatch string
}

// ParseResultDirFile 与 Server ResultDirFileAction 构造器逐条对齐：
// 含 ":" 时拆 path:mode:subMatch；是否通配由 AntIsPattern 判定（不是含 "*" 就通配）。
func ParseResultDirFile(value string) ResultDirFile {
	if strings.Contains(value, ":") {
		parts := splitTrimList(value, ":")
		action := ResultDirFile{}
		if len(parts) > 0 {
			action.Path = parts[0]
		}
		if !AntIsPattern(action.Path) {
			action.Type = PathOriginal
			return action
		}
		action.Type = PathAnt
		action.Mode = KeepDir
		if len(parts) > 1 {
			// EnumUtil.fromString(..., KEEP_DIR) 语义：非法值回退默认 KEEP_DIR。
			if strings.EqualFold(strings.TrimSpace(parts[1]), string(SameDir)) {
				action.Mode = SameDir
			}
		}
		if len(parts) > 2 {
			action.SubMatch = parts[2]
		}
		return action
	}
	action := ResultDirFile{Path: value}
	if AntIsPattern(value) {
		action.Type = PathAnt
		action.Mode = KeepDir
		return action
	}
	action.Type = PathOriginal
	return action
}

// AntSubMatch 返回包裹成 "/x/" 的二级目录（对应 ResultDirFileAction#antSubMatch）。
func (r ResultDirFile) AntSubMatch() string {
	if strings.TrimSpace(r.SubMatch) == "" {
		return ""
	}
	return wrapDir(normalizeSlash(r.SubMatch))
}

// PackOptions 是打包阶段输入（PROTOCOL §2 的 resultDirFile/excludeReleaseAnt/releaseHideFile）。
type PackOptions struct {
	ResultDirFile     string
	ExcludeReleaseAnt string
	ReleaseHideFile   bool
}

// PackEntry 是一个 zip 条目：Source 为绝对路径，Name 为相对 Server 结果目录的条目名。
type PackEntry struct {
	Source string
	Name   string
}

// PackResult 是收集结果：条目 + 统计（Server 会把统计写进构建日志）。
type PackResult struct {
	Action   ResultDirFile
	Entries  []PackEntry
	Matched  int
	Excluded int
}

// Collect 遍历 source 收集产物，条目名口径见 PROTOCOL §6：
//   - ORIGINAL：条目名 = resultDirFile 本身（目录则内容挂在该前缀下）；
//   - ANT + KEEP_DIR：条目名 = 命中路径去掉 antSubMatch 前缀后的相对路径（Server 的 itemEnd）；
//   - ANT + SAME_DIR：条目名 = basename。
func Collect(sourceDir string, opts PackOptions) (*PackResult, error) {
	source, err := filepath.Abs(sourceDir)
	if err != nil {
		return nil, err
	}
	action := ParseResultDirFile(opts.ResultDirFile)
	result := &PackResult{Action: action}
	excludes := splitTrimList(opts.ExcludeReleaseAnt, ",")
	seen := make(map[string]struct{}, 16)

	add := func(sourceFile, name string) {
		name = cleanEntryName(name)
		if name == "" {
			return
		}
		if _, exists := seen[name]; exists {
			return
		}
		seen[name] = struct{}{}
		result.Entries = append(result.Entries, PackEntry{Source: sourceFile, Name: name})
	}
	// 与 Server predicate 一致：excludeReleaseAnt 直接 match 原规则（未 normalize），
	// 探针是 "/相对路径"。注意 "*.md" 这类不带前导 "/" 的规则在 Server 上同样不命中。
	excluded := func(probe string) bool {
		for _, rule := range excludes {
			if AntMatch(rule, probe) {
				return true
			}
		}
		return false
	}
	hidden := func(rel string) bool {
		if opts.ReleaseHideFile {
			return false
		}
		return strings.HasPrefix(path.Base(rel), ".")
	}

	if action.Type == PathOriginal {
		relative := strings.TrimPrefix(normalizeSlash(action.Path), "/")
		target := filepath.Join(source, filepath.FromSlash(relative))
		info, statErr := os.Stat(target)
		if statErr != nil {
			return nil, fmt.Errorf("build product does not exist: %s", action.Path)
		}
		if !info.IsDir() {
			if hidden(relative) {
				return result, nil
			}
			if excluded("/" + relative) {
				result.Excluded++
				return result, nil
			}
			result.Matched++
			add(target, relative)
			return result, nil
		}
		walkErr := filepath.WalkDir(target, func(current string, entry fs.DirEntry, walkErr error) error {
			if walkErr != nil {
				return walkErr
			}
			if entry.IsDir() {
				return nil
			}
			rel, relErr := filepath.Rel(source, current)
			if relErr != nil {
				return relErr
			}
			rel = normalizeSlash(rel)
			if hidden(rel) {
				return nil
			}
			if excluded("/" + rel) {
				result.Excluded++
				return nil
			}
			add(current, rel)
			return nil
		})
		if walkErr != nil {
			return nil, walkErr
		}
		result.Matched = len(result.Entries)
		return result, nil
	}

	pattern := "/" + strings.TrimPrefix(normalizeSlash(action.Path), "/")
	subMatch := action.AntSubMatch()
	type antItem struct {
		probe string
		dir   bool
	}
	items := make([]antItem, 0, 8)
	walkErr := filepath.WalkDir(source, func(current string, entry fs.DirEntry, walkErr error) error {
		if walkErr != nil {
			return walkErr
		}
		probe := probePath(source, current)
		if AntMatch(pattern, probe) {
			items = append(items, antItem{probe: probe, dir: entry.IsDir()})
		}
		return nil
	})
	if walkErr != nil {
		return nil, walkErr
	}
	if len(items) == 0 {
		return nil, fmt.Errorf("%w: %s", ErrNoMatch, action.Path)
	}
	result.Matched = len(items)
	for _, item := range items {
		// 二级匹配：StrUtil.isEmpty(antSubMatch) || matchStart(antSubMatch + "**", path)
		if subMatch != "" && !AntMatchStart(subMatch+"**", item.probe) {
			continue
		}
		if excluded(item.probe) {
			result.Excluded++
			continue
		}
		if !item.dir {
			name, ok := entryName(item.probe, subMatch, action.Mode)
			if !ok {
				continue
			}
			target := filepath.Join(source, filepath.FromSlash(strings.TrimPrefix(item.probe, "/")))
			add(target, name)
			continue
		}
		// 命中目录：对应 Server 的 setCopyContentIfDir(true)，目录内容整体拷到目标位置。
		baseName, ok := entryName(item.probe, subMatch, action.Mode)
		if !ok {
			continue
		}
		dirTarget := filepath.Join(source, filepath.FromSlash(strings.TrimPrefix(item.probe, "/")))
		dirErr := filepath.WalkDir(dirTarget, func(current string, entry fs.DirEntry, walkErr error) error {
			if walkErr != nil {
				return walkErr
			}
			if entry.IsDir() {
				return nil
			}
			rel, relErr := filepath.Rel(dirTarget, current)
			if relErr != nil {
				return relErr
			}
			rel = normalizeSlash(rel)
			if hidden(rel) {
				return nil
			}
			if action.Mode == SameDir {
				add(current, rel)
				return nil
			}
			add(current, baseName+"/"+rel)
			return nil
		})
		if dirErr != nil {
			return nil, dirErr
		}
	}
	return result, nil
}

// Zip 把收集到的条目写成 zip，返回文件大小与 sha1。
func Zip(entries []PackEntry, target string) (int64, string, error) {
	if err := os.MkdirAll(filepath.Dir(target), 0o755); err != nil {
		return 0, "", err
	}
	file, err := os.Create(target)
	if err != nil {
		return 0, "", err
	}
	digest := sha1.New()
	writer := zip.NewWriter(io.MultiWriter(file, digest))
	for _, entry := range entries {
		info, statErr := os.Stat(entry.Source)
		if statErr != nil {
			continue
		}
		header, headerErr := zip.FileInfoHeader(info)
		if headerErr != nil {
			_ = writer.Close()
			_ = file.Close()
			return 0, "", headerErr
		}
		header.Name = entry.Name
		header.Method = zip.Deflate
		destination, createErr := writer.CreateHeader(header)
		if createErr != nil {
			_ = writer.Close()
			_ = file.Close()
			return 0, "", createErr
		}
		source, openErr := os.Open(entry.Source)
		if openErr != nil {
			_ = writer.Close()
			_ = file.Close()
			return 0, "", openErr
		}
		_, copyErr := io.Copy(destination, source)
		_ = source.Close()
		if copyErr != nil {
			_ = writer.Close()
			_ = file.Close()
			return 0, "", copyErr
		}
	}
	if err := writer.Close(); err != nil {
		_ = file.Close()
		return 0, "", err
	}
	if err := file.Close(); err != nil {
		return 0, "", err
	}
	stat, err := os.Stat(target)
	if err != nil {
		return 0, "", err
	}
	return stat.Size(), hex.EncodeToString(digest.Sum(nil)), nil
}

// entryName 计算命中路径的条目名（KEEP_DIR 走 Server 的 notMathIndex 算法，SAME_DIR 取 basename）。
func entryName(probe, subMatch string, mode UploadMode) (string, bool) {
	if mode == SameDir {
		return path.Base(strings.TrimSuffix(probe, "/")), true
	}
	return keepDirName(probe, subMatch)
}

// keepDirName 复刻 Server packageFile 的 KEEP_DIR 逻辑：
// 从尾部向前用 AntMatch(subMatch, "/a/b/") 找到 notMathIndex，保留其后的层级。
func keepDirName(probe, subMatch string) (string, bool) {
	tokens := splitTokens(probe)
	if subMatch == "" || subMatch == "/" {
		return strings.Join(tokens, "/"), len(tokens) > 0
	}
	size := len(tokens)
	for index := size - 1; index >= 0; index-- {
		suffix := ""
		if index != size-1 {
			suffix = "/"
		}
		item := "/" + strings.Join(tokens[:index+1], "/") + suffix
		if AntMatch(subMatch, item) {
			return strings.Join(tokens[index+1:], "/"), true
		}
	}
	return "", false
}

// probePath 把绝对路径归一成 "/相对路径"（对应 AntPathUtil 的 subPath normalize）。
func probePath(source, current string) string {
	rel, err := filepath.Rel(source, current)
	if err != nil {
		return "/"
	}
	return "/" + strings.TrimPrefix(normalizeSlash(rel), "/")
}

// splitTokens 拆路径段（丢弃空段，等价 Java StrUtil.splitTrim(path, "/")）。
func splitTokens(value string) []string {
	parts := strings.Split(value, "/")
	tokens := make([]string, 0, len(parts))
	for _, part := range parts {
		if part == "" || part == "." {
			continue
		}
		tokens = append(tokens, part)
	}
	return tokens
}

// normalizeSlash 把分隔符统一成 "/"（等价 Hutool FileUtil.normalize）。
func normalizeSlash(value string) string {
	value = strings.ReplaceAll(value, "\\", "/")
	return path.Clean(value)
}

// wrapDir 把路径包成 "/x/"（等价 StrUtil.wrapIfMissing(str, "/", "/")）。
func wrapDir(value string) string {
	if strings.TrimSpace(value) == "" {
		return ""
	}
	if !strings.HasPrefix(value, "/") {
		value = "/" + value
	}
	if !strings.HasSuffix(value, "/") {
		value = value + "/"
	}
	return value
}

// cleanEntryName 清洗 zip 条目名：去前导 "/"、拒绝穿越、统一 "/"。
func cleanEntryName(name string) string {
	name = strings.TrimPrefix(normalizeSlash(name), "/")
	if name == "" || name == "." || name == ".." {
		return ""
	}
	for _, part := range strings.Split(name, "/") {
		if part == ".." {
			return ""
		}
	}
	return name
}

// splitTrimList 按分隔符拆分并 trim（等价 Hutool StrUtil.splitTrim）。
func splitTrimList(value, separator string) []string {
	if strings.TrimSpace(value) == "" {
		return nil
	}
	parts := strings.Split(value, separator)
	result := make([]string, 0, len(parts))
	for _, part := range parts {
		part = strings.TrimSpace(part)
		if part != "" {
			result = append(result, part)
		}
	}
	return result
}
