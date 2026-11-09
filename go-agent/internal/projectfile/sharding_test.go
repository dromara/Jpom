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
	"encoding/hex"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

func md5Hex(content string) string {
	sum := md5.Sum([]byte(content))
	return hex.EncodeToString(sum[:])
}

// uploadAll uploads content as chunks named <name>.<index>.
func uploadAll(t *testing.T, service *Service, name, content, sliceID string, chunkSize int) {
	t.Helper()
	chunks := (len(content) + chunkSize - 1) / chunkSize
	for index := 0; index < chunks; index++ {
		start := index * chunkSize
		end := start + chunkSize
		if end > len(content) {
			end = len(content)
		}
		options := ShardingChunkOptions{
			SliceID:    sliceID,
			FileSumMD5: md5Hex(content),
			TotalSlice: fmt.Sprintf("%d", chunks),
			NowSlice:   fmt.Sprintf("%d", index),
		}
		if err := service.SaveShardingChunk(fmt.Sprintf("%s.%d", name, index), strings.NewReader(content[start:end]), options); err != nil {
			t.Fatalf("upload chunk %d: %v", index, err)
		}
	}
}

func TestShardingUploadAndMerge(t *testing.T) {
	service, _ := testService(t)
	content := strings.Repeat("0123456789", 37)
	chunkSize := 40
	uploadAll(t, service, "demo.txt", content, "a1b2c3d4e5f6", chunkSize)
	merged, err := service.MergeSharding("a1b2c3d4e5f6", md5Hex(content), "10")
	if err != nil {
		t.Fatalf("merge sharding: %v", err)
	}
	if filepath.Base(merged) != "demo.txt" {
		t.Fatalf("unexpected merged file name: %s", merged)
	}
	raw, err := os.ReadFile(merged)
	if err != nil {
		t.Fatalf("read merged file: %v", err)
	}
	if string(raw) != content {
		t.Fatalf("merged content mismatch: %d vs %d bytes", len(raw), len(content))
	}
	items := filepath.Join(filepath.Dir(merged), "items")
	if _, err := os.Stat(items); !os.IsNotExist(err) {
		t.Fatalf("chunk directory should be removed, err=%v", err)
	}
}

func TestShardingValidation(t *testing.T) {
	service, _ := testService(t)
	content := "abcdef"
	// Missing md5
	if err := service.SaveShardingChunk("a.txt.0", strings.NewReader(content), ShardingChunkOptions{
		SliceID: "abc", TotalSlice: "1", NowSlice: "0",
	}); err == nil || !strings.Contains(err.Error(), "没有文件签名信息") {
		t.Fatalf("unexpected error: %v", err)
	}
	// Traversal in slice id
	if err := service.SaveShardingChunk("a.txt.0", strings.NewReader(content), ShardingChunkOptions{
		SliceID: "../../evil", FileSumMD5: md5Hex(content), TotalSlice: "1", NowSlice: "0",
	}); err == nil {
		t.Fatal("expected invalid slice id to be rejected")
	}
	// Missing file part
	if err := service.SaveShardingChunk("a.txt.0", nil, ShardingChunkOptions{
		SliceID: "abc", FileSumMD5: md5Hex(content), TotalSlice: "1", NowSlice: "0",
	}); err == nil || !strings.Contains(err.Error(), "没有上传文件") {
		t.Fatalf("unexpected error: %v", err)
	}
	// Invalid now/total
	if err := service.SaveShardingChunk("a.txt.0", strings.NewReader(content), ShardingChunkOptions{
		SliceID: "abc", FileSumMD5: md5Hex(content), TotalSlice: "1", NowSlice: "-1",
	}); err == nil || !strings.Contains(err.Error(), "当前上传的分片信息错误") {
		t.Fatalf("unexpected error: %v", err)
	}
	// Incomplete upload info
	if err := service.SaveShardingChunk("a.txt.0", strings.NewReader(content), ShardingChunkOptions{
		SliceID: "abc", FileSumMD5: md5Hex(content), NowSlice: "0",
	}); err == nil || !strings.Contains(err.Error(), "上传信息不完整") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestShardingMergeFailures(t *testing.T) {
	service, _ := testService(t)
	content := "hello sharding world"
	uploadAll(t, service, "app.bin", content, "mergefail01", 5)
	// Wrong md5
	if _, err := service.MergeSharding("mergefail01", md5Hex("other"), "4"); err == nil ||
		!strings.Contains(err.Error(), "文件合并后异常") {
		t.Fatalf("unexpected error: %v", err)
	}
	// Missing chunks
	service2, _ := testService(t)
	uploadAll(t, service2, "app.bin", content, "mergefail02", 10)
	if _, err := service2.MergeSharding("mergefail02", md5Hex(content), "9"); err == nil ||
		!strings.Contains(err.Error(), "存在分片丢失") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestShardingRetryAfterFailedMerge(t *testing.T) {
	service, _ := testService(t)
	content := "retry after failed merge"
	uploadAll(t, service, "retry.txt", content, "retryslice0001", 8)
	// A wrong md5 must keep the chunks so the client can retry.
	if _, err := service.MergeSharding("retryslice0001", md5Hex("wrong"), "3"); err == nil {
		t.Fatal("expected md5 mismatch")
	}
	itemsDir := filepath.Join(service.app.TempDir, time.Now().Format("2006-01-02"), "slice", "retryslice0001", "items")
	if entries, err := os.ReadDir(itemsDir); err != nil || len(entries) != 3 {
		t.Fatalf("chunks must be kept after a failed merge: %v %v", entries, err)
	}
	merged, err := service.MergeSharding("retryslice0001", md5Hex(content), "3")
	if err != nil {
		t.Fatalf("retry merge: %v", err)
	}
	if raw, err := os.ReadFile(merged); err != nil || string(raw) != content {
		t.Fatalf("retry content: %q %v", raw, err)
	}
}
