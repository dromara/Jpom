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
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"
)

// ShardingChunkOptions are the form fields of the upload-sharding endpoints.
type ShardingChunkOptions struct {
	SliceID    string
	FileSumMD5 string
	TotalSlice string
	NowSlice   string
}

// shardingDateDir returns <agent>/data/temp/<yyyy-MM-dd>, the directory the Java
// agent uses for every temporary upload (AgentConfig#getTempPath).
func (s *Service) shardingDateDir() (string, error) {
	dir := filepath.Join(s.app.TempDir, time.Now().Format("2006-01-02"))
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", fmt.Errorf("创建临时目录失败:%w", err)
	}
	return dir, nil
}

// validateSliceID makes sure the slice id can safely be used as a directory
// name. The server always sends a UUID, so only [0-9A-Za-z_-] is accepted.
func validateSliceID(sliceID string) error {
	if strings.TrimSpace(sliceID) == "" {
		return invalidf("没有分片 id 信息")
	}
	if len(sliceID) > 128 {
		return invalidf("分片 id 信息不合法")
	}
	for _, value := range sliceID {
		switch {
		case value >= '0' && value <= '9':
		case value >= 'a' && value <= 'z':
		case value >= 'A' && value <= 'Z':
		case value == '-' || value == '_':
		default:
			return invalidf("分片 id 信息不合法")
		}
	}
	return nil
}

func parseIntParam(value, name string) (int, error) {
	value = strings.TrimSpace(value)
	if value == "" {
		return 0, invalidf("上传信息不完整：%s", name)
	}
	parsed, err := strconv.Atoi(value)
	if err != nil {
		return 0, invalidf("上传信息不完整：%s", name)
	}
	return parsed, nil
}

// validateShardingCommon validates the parameters shared by upload-sharding and
// sharding-merge, in the same order as BaseJpomController.
func validateShardingCommon(fileSumMD5, sliceID string) error {
	if strings.TrimSpace(fileSumMD5) == "" {
		return invalidf("没有文件签名信息")
	}
	return validateSliceID(sliceID)
}

// uploadShardingItemPath returns <temp>/<date>/slice/<sliceId>/items.
func (s *Service) uploadShardingItemPath(sliceID string) (string, error) {
	dateDir, err := s.shardingDateDir()
	if err != nil {
		return "", err
	}
	return filepath.Join(dateDir, "slice", sliceID, "items"), nil
}

// SaveShardingChunk stores one uploaded chunk, mirroring
// BaseJpomController#uploadSharding. body is nil when the request had no file
// part; filename is the multipart original file name ("<name>.<index>").
func (s *Service) SaveShardingChunk(filename string, body io.Reader, options ShardingChunkOptions) error {
	if err := validateShardingCommon(options.FileSumMD5, options.SliceID); err != nil {
		return err
	}
	totalSlice, err := parseIntParam(options.TotalSlice, "totalSlice")
	if err != nil {
		return err
	}
	nowSlice, err := parseIntParam(options.NowSlice, "nowSlice")
	if err != nil {
		return err
	}
	if totalSlice <= 0 || nowSlice < 0 || totalSlice < nowSlice {
		return invalidf("当前上传的分片信息错误")
	}
	if body == nil {
		return invalidf("没有上传文件")
	}
	original := filepath.Base(strings.ReplaceAll(filename, "\\", "/"))
	if original == "" || original == "." || original == ".." || original == "/" {
		return invalidf("没有上传文件")
	}
	itemsDir, err := s.uploadShardingItemPath(options.SliceID)
	if err != nil {
		return err
	}
	if err := os.MkdirAll(itemsDir, 0o755); err != nil {
		return fmt.Errorf("创建分片目录失败:%w", err)
	}
	target := filepath.Join(itemsDir, original)
	out, err := os.OpenFile(target, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0o644)
	if err != nil {
		return fmt.Errorf("保存分片失败:%w", err)
	}
	if _, err := io.Copy(out, body); err != nil {
		_ = out.Close()
		return fmt.Errorf("保存分片失败:%w", err)
	}
	return out.Close()
}

// chunkIndex extracts the numeric chunk index from "name.0" style file names.
func chunkIndex(name string) int {
	extension := extName(name)
	value, err := strconv.Atoi(extension)
	if err != nil {
		return 0
	}
	return value
}

// MergeSharding merges the uploaded chunks into one file inside the temp dir and
// verifies the md5, mirroring BaseJpomController#shardingTryMerge.
func (s *Service) MergeSharding(sliceID, fileSumMD5, totalSliceValue string) (string, error) {
	if err := validateShardingCommon(fileSumMD5, sliceID); err != nil {
		return "", err
	}
	totalSlice, err := parseIntParam(totalSliceValue, "totalSlice")
	if err != nil {
		return "", err
	}
	itemsDir, err := s.uploadShardingItemPath(sliceID)
	if err != nil {
		return "", err
	}
	entries, readErr := os.ReadDir(itemsDir)
	if readErr != nil {
		entries = nil
	}
	if len(entries) != totalSlice {
		return "", invalidf("文件上传失败,存在分片丢失的情况, %d != %d", len(entries), totalSlice)
	}
	sliceDir := filepath.Dir(itemsDir)
	baseName := trimLastExt(entries[0].Name())
	mergedPath := filepath.Join(sliceDir, baseName)
	sorted := make([]os.DirEntry, 0, len(entries))
	sorted = append(sorted, entries...)
	sort.SliceStable(sorted, func(i, j int) bool {
		return chunkIndex(sorted[i].Name()) < chunkIndex(sorted[j].Name())
	})
	removeMerged := func() { _ = os.RemoveAll(mergedPath) }
	out, err := os.OpenFile(mergedPath, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0o644)
	if err != nil {
		return "", fmt.Errorf("合并分片失败:%w", err)
	}
	for _, entry := range sorted {
		chunk, openErr := os.Open(filepath.Join(itemsDir, entry.Name()))
		if openErr != nil {
			_ = out.Close()
			removeMerged()
			return "", fmt.Errorf("合并分片失败:%w", openErr)
		}
		_, copyErr := io.Copy(out, chunk)
		_ = chunk.Close()
		if copyErr != nil {
			_ = out.Close()
			removeMerged()
			return "", fmt.Errorf("合并分片失败:%w", copyErr)
		}
	}
	if err := out.Close(); err != nil {
		removeMerged()
		return "", fmt.Errorf("合并分片失败:%w", err)
	}
	actualMD5, err := fileMD5(mergedPath)
	if err != nil {
		removeMerged()
		return "", fmt.Errorf("合并分片失败:%w", err)
	}
	if !strings.EqualFold(actualMD5, fileSumMD5) {
		// Keep the chunks so the caller can retry after the upload was truncated;
		// the Java agent deletes them here, which makes a retry impossible.
		s.Log().Warn("file merge md5 mismatch", "path", mergedPath, "actual", actualMD5, "expect", fileSumMD5)
		removeMerged()
		return "", invalidf("文件合并后异常,文件不完整可能被损坏")
	}
	// Only remove the chunks after the merged file has been verified.
	_ = os.RemoveAll(itemsDir)
	return mergedPath, nil
}

// randomHex returns a short random hex string used for temporary directories.
func randomHex(length int) string {
	buffer := make([]byte, length)
	if _, err := rand.Read(buffer); err != nil {
		return strconv.FormatInt(time.Now().UnixNano(), 16)
	}
	return hex.EncodeToString(buffer)
}
