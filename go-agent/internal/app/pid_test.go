/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package app

import (
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"testing"
)

// TestPidFileWriteAndRemove 验证 PID 文件路径、内容与删除（CONTRACT §4）。
func TestPidFileWriteAndRemove(t *testing.T) {
	base := t.TempDir()
	a := &App{BaseDir: base, Pid: 4321}
	wantPath := filepath.Join(base, "bin", "agent.pid")
	if a.PidFilePath() != wantPath {
		t.Fatalf("PidFilePath = %q, want %q", a.PidFilePath(), wantPath)
	}
	if err := a.WritePidFile(); err != nil {
		t.Fatalf("WritePidFile: %v", err)
	}
	content, err := os.ReadFile(wantPath)
	if err != nil {
		t.Fatalf("读取 PID 文件失败: %v", err)
	}
	if got := strings.TrimSpace(string(content)); got != "4321" {
		t.Errorf("PID 文件内容 = %q, want 4321", got)
	}
	a.RemovePidFile()
	if _, err := os.Stat(wantPath); !os.IsNotExist(err) {
		t.Errorf("RemovePidFile 后文件仍存在: %v", err)
	}
}

// TestPidFileFallbackToCurrentProcess Pid 未设置时写入当前进程 PID。
func TestPidFileFallbackToCurrentProcess(t *testing.T) {
	a := &App{BaseDir: t.TempDir()}
	if err := a.WritePidFile(); err != nil {
		t.Fatalf("WritePidFile: %v", err)
	}
	content, err := os.ReadFile(a.PidFilePath())
	if err != nil {
		t.Fatal(err)
	}
	if got := strings.TrimSpace(string(content)); got != strconv.Itoa(os.Getpid()) {
		t.Errorf("PID 文件内容 = %q, want %d", got, os.Getpid())
	}
}
