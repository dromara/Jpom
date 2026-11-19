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
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// TestJobLogSeqAndReplay 覆盖 seq 单调分配、fromSeq 补发与日志文件落盘（PROTOCOL §3）。
func TestJobLogSeqAndReplay(t *testing.T) {
	logPath := filepath.Join(t.TempDir(), "#1.log")
	jobLog, err := NewJobLog(logPath, "UTF-8")
	if err != nil {
		t.Fatalf("NewJobLog: %v", err)
	}
	defer func() { _ = jobLog.Close() }()

	jobLog.StageStart(StagePull)
	jobLog.Log(StagePull, "line-1")
	jobLog.Log(StagePull, "line-2")

	backlog, live, cancel := jobLog.Subscribe(1)
	if len(backlog) != 2 || backlog[0].Seq != 2 || backlog[1].Line != "line-2" {
		t.Fatalf("补发历史异常: %+v", backlog)
	}
	jobLog.Log(StagePull, "line-3")
	select {
	case event := <-live:
		if event.Seq != 4 || event.Line != "line-3" {
			t.Fatalf("实时事件异常: %+v", event)
		}
	default:
		t.Fatalf("订阅者没有收到实时事件")
	}
	cancel()

	content, err := os.ReadFile(logPath)
	if err != nil {
		t.Fatalf("读取日志文件: %v", err)
	}
	// 只有 log 帧落文件（stage/env/result/exit 帧在 job 状态里）。
	if string(content) != "line-1\nline-2\nline-3\n" {
		t.Fatalf("日志文件内容异常: %q", string(content))
	}

	all, _, cancelAll := jobLog.Subscribe(0)
	if len(all) != 4 || all[0].Type != TypeStage || all[0].Event != "start" {
		t.Fatalf("fromSeq=0 应补发全部事件: %+v", all)
	}
	cancelAll()
}

// TestJobLogTruncateAndDecode 覆盖单行 8KiB 截断与控制台编码解码。
func TestJobLogTruncateAndDecode(t *testing.T) {
	logPath := filepath.Join(t.TempDir(), "#2.log")
	jobLog, err := NewJobLog(logPath, "GBK")
	if err != nil {
		t.Fatalf("NewJobLog: %v", err)
	}
	defer func() { _ = jobLog.Close() }()

	jobLog.Log(StageExecuteCommand, strings.Repeat("a", MaxLineBytes*2))
	encoded, err := proc.EncodeString("GBK", "中文输出")
	if err != nil {
		t.Fatalf("encode gbk: %v", err)
	}
	jobLog.LogBytes(StageExecuteCommand, append(encoded, '\n'))

	events, _, cancel := jobLog.Subscribe(0)
	defer cancel()
	if len(events) != 2 {
		t.Fatalf("事件数 = %d", len(events))
	}
	if len(events[0].Line) != MaxLineBytes {
		t.Errorf("超长行未截断: %d", len(events[0].Line))
	}
	if events[1].Line != "中文输出" {
		t.Errorf("GBK 解码异常: %q", events[1].Line)
	}
}
