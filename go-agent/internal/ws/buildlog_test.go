//
// Copyright (c) 2019 Of Him Code Technology Studio
// Jpom is licensed under Mulan PSL v2.
// You can use this software according to the terms and conditions of the Mulan PSL v2.
// You may obtain a copy of Mulan PSL v2 at:
// 			http://license.coscl.org.cn/MulanPSL2
// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
// See the Mulan PSL v2 for more details.
//

package ws

import (
	"fmt"
	"net/url"
	"strings"
	"testing"
	"time"

	"github.com/gorilla/websocket"

	"github.com/dromara/Jpom/go-agent/internal/builder"
)

// TestBuildLogUnauthorizedClosesWith1003 覆盖握手鉴权失败（沿用 agent 既有约定）。
func TestBuildLogUnauthorizedClosesWith1003(t *testing.T) {
	instance := testApp(t)
	builds := builder.NewManager(instance)
	server := startServer(t, instance, HandleBuildLog(instance, builds))
	target := "ws" + strings.TrimPrefix(server.URL, "http") + "/build-log?jobId=missing&Jpom-Agent-Authorize=wrong"
	conn, _, err := websocket.DefaultDialer.Dial(target, nil)
	if err != nil {
		t.Fatalf("dial: %v", err)
	}
	defer func() { _ = conn.Close() }()
	_ = conn.SetReadDeadline(time.Now().Add(3 * time.Second))
	_, _, readErr := conn.ReadMessage()
	closeError, ok := readErr.(*websocket.CloseError)
	if !ok {
		t.Fatalf("expected close error, got %v", readErr)
	}
	if closeError.Code != closeCannotAccept {
		t.Fatalf("expected close code %d, got %d", closeCannotAccept, closeError.Code)
	}
}

// TestBuildLogLostJob 覆盖未知 jobId → 直接回 exit(lost) 并关闭（PROTOCOL §3/§4）。
func TestBuildLogLostJob(t *testing.T) {
	instance := testApp(t)
	builds := builder.NewManager(instance)
	server := startServer(t, instance, HandleBuildLog(instance, builds))
	conn := dial(t, server, "/build-log", "&jobId=missing")
	frame := readText(t, conn, 3*time.Second)
	if !strings.Contains(frame, `"type":"exit"`) || !strings.Contains(frame, `"state":"lost"`) {
		t.Fatalf("unexpected frame: %s", frame)
	}
}

// TestBuildLogStreamsJobFrames 覆盖事件流：stage/log/exit 帧按 seq 顺序下发，
// 关闭连接后带 fromSeq 重连可以补发（PROTOCOL §3）。
func TestBuildLogStreamsJobFrames(t *testing.T) {
	instance := testApp(t)
	builds := builder.NewManager(instance)
	job, err := builds.Start(&builder.Spec{
		JobID:        "ws-job",
		BuildModelID: "ws-model",
		Repo:         builder.RepoSpec{Type: "git", URL: "/nonexistent-repository"},
		Script:       "echo never",
		ShellSuffix:  "sh",
	})
	if err != nil {
		t.Fatalf("Start: %v", err)
	}
	status := waitBuildTerminal(t, builds, job.JobID())
	if status.State != builder.StateFailed {
		t.Fatalf("state = %s, message = %s", status.State, status.Message)
	}
	server := startServer(t, instance, HandleBuildLog(instance, builds))
	conn := dial(t, server, "/build-log", "&jobId="+url.QueryEscape(job.JobID()))
	frames := readUntil(t, conn, 5*time.Second, func(frame string) bool {
		return strings.Contains(frame, `"type":"exit"`)
	})
	if len(frames) < 2 {
		t.Fatalf("事件过少: %v", frames)
	}
	if !strings.Contains(frames[0], `"type":"stage"`) || !strings.Contains(frames[0], `"seq":1`) {
		t.Fatalf("首帧应为 seq=1 的 stage 帧: %s", frames[0])
	}
	// 补发：fromSeq 指向最后一个 stage end，重连后只应收到后续帧。
	fromSeq := 2
	replay := dial(t, server, "/build-log", fmt.Sprintf("&jobId=%s&fromSeq=%d", url.QueryEscape(job.JobID()), fromSeq))
	replayed := readUntil(t, replay, 5*time.Second, func(frame string) bool {
		return strings.Contains(frame, `"type":"exit"`)
	})
	if !strings.Contains(replayed[0], `"seq":3`) {
		t.Fatalf("fromSeq 补发应从 seq=3 开始: %v", replayed)
	}
}

// waitBuildTerminal 轮询到 job 终态。
func waitBuildTerminal(t *testing.T, builds *builder.Manager, jobID string) *builder.Status {
	t.Helper()
	deadline := time.Now().Add(30 * time.Second)
	for {
		status, err := builds.Status(jobID)
		if err != nil {
			t.Fatalf("Status: %v", err)
		}
		switch status.State {
		case builder.StateSuccess, builder.StateFailed, builder.StateCancelled, builder.StateTimeout, builder.StateSkipped, builder.StateLost:
			return status
		}
		if time.Now().After(deadline) {
			t.Fatalf("job 未结束: %+v", status)
		}
		time.Sleep(30 * time.Millisecond)
	}
}
