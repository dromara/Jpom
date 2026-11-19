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
	"sync"

	"github.com/dromara/Jpom/go-agent/internal/proc"
)

// 帧类型（PROTOCOL §3）。
const (
	TypeStage  = "stage"
	TypeLog    = "log"
	TypeEnv    = "env"
	TypeResult = "result"
	TypeExit   = "exit"
	TypePing   = "ping"
)

// 阶段名，必须与 Server createProcess() 的 key 完全一致（PROTOCOL §3）。
const (
	StagePull           = "pull"
	StageExecuteCommand = "executeCommand"
	StagePackageFile    = "packageFile"
)

const (
	// MaxLineBytes 单行日志上限（PROTOCOL §8.5：超长行截断到 8 KiB）。
	MaxLineBytes = 8 << 10
	// defaultEventCapacity 事件环形缓冲容量：断线重连带 fromSeq 时从这里补发。
	defaultEventCapacity = 20000
	// subscriberBuffer 单个订阅者的发送缓冲；写满视为慢消费者，直接摘除让其重连。
	subscriberBuffer = 256
)

// Event 是一帧事件流（PROTOCOL §3），同时是 WS 帧的 JSON 结构。
//
// seq 从 1 开始单调递增（job 生命周期内不重复）；ping 帧没有 seq，因此用
// omitempty（真实事件 seq 恒 >= 1，不会被省略）。
type Event struct {
	Seq       int               `json:"seq,omitempty"`
	Type      string            `json:"type"`
	Stage     string            `json:"stage,omitempty"`
	Event     string            `json:"event,omitempty"`
	Code      *int              `json:"code,omitempty"`
	Message   string            `json:"message,omitempty"`
	Line      string            `json:"line,omitempty"`
	Data      map[string]string `json:"data,omitempty"`
	File      string            `json:"file,omitempty"`
	Size      int64             `json:"size,omitempty"`
	Sha1      string            `json:"sha1,omitempty"`
	ResultDir string            `json:"resultDir,omitempty"`
	State     string            `json:"state,omitempty"`
	Branch    string            `json:"branch,omitempty"`
	CommitID  string            `json:"commitId,omitempty"`
}

// JobLog 是单个构建 job 的日志：文件落盘 + 单调 seq + 订阅广播。
//
// 落盘路径与 Server 相对结构一致（data/build/<buildModelId>/log/#<n>.log），便于人工排障；
// 文件内容为 UTF-8 纯文本，每行一条 log 帧（stage/env/result/exit 帧不落文件，见 job 状态 JSON）。
type JobLog struct {
	path     string
	charset  string
	capacity int

	mu      sync.Mutex
	file    *os.File
	seq     int
	events  []Event
	subs    map[int64]chan Event
	nextSub int64
	closed  bool
}

// NewJobLog 创建（或续写）一个 job 日志文件。
func NewJobLog(path, charset string) (*JobLog, error) {
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return nil, err
	}
	file, err := os.OpenFile(path, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0o644)
	if err != nil {
		return nil, err
	}
	return &JobLog{
		path:     path,
		charset:  charset,
		capacity: defaultEventCapacity,
		file:     file,
		subs:     map[int64]chan Event{},
	}, nil
}

// Path 返回日志文件路径。
func (l *JobLog) Path() string { return l.path }

// Seq 返回当前已分配的最大 seq。
func (l *JobLog) Seq() int {
	l.mu.Lock()
	defer l.mu.Unlock()
	return l.seq
}

// Close 关闭日志文件；之后的事件不再落盘也不再广播。
func (l *JobLog) Close() error {
	l.mu.Lock()
	if l.closed {
		l.mu.Unlock()
		return nil
	}
	l.closed = true
	writer := l.file
	l.file = nil
	for id, ch := range l.subs {
		close(ch)
		delete(l.subs, id)
	}
	l.mu.Unlock()
	if writer != nil {
		return writer.Close()
	}
	return nil
}

// Log 记录一行输出（调用方已保证行内不含换行）。
func (l *JobLog) Log(stage, line string) {
	l.emit(Event{Type: TypeLog, Stage: stage, Line: truncateLine(line)})
}

// LogBytes 先按控制台编码（jpom.system.console-charset，Windows 默认 GBK）解码再记录。
func (l *JobLog) LogBytes(stage string, raw []byte) {
	line := strings.TrimRight(string(raw), "\r\n")
	decoded, err := proc.DecodeString(l.charset, []byte(line))
	if err != nil {
		decoded = line
	}
	l.Log(stage, decoded)
}

// StageStart 记录阶段开始帧。
func (l *JobLog) StageStart(stage string) {
	l.emit(Event{Type: TypeStage, Stage: stage, Event: "start"})
}

// StageEnd 记录阶段结束帧；branch/commitID 只在 pull 阶段有值（Server 用于回填历史）。
func (l *JobLog) StageEnd(stage string, code int, message, branch, commitID string) {
	exitCode := code
	l.emit(Event{
		Type:     TypeStage,
		Stage:    stage,
		Event:    "end",
		Code:     &exitCode,
		Message:  message,
		Branch:   branch,
		CommitID: commitID,
	})
}

// Env 回传最终生效的环境变量（已脱敏）。
func (l *JobLog) Env(data map[string]string) {
	l.emit(Event{Type: TypeEnv, Data: data})
}

// Result 回传产物元信息，file 固定为 zip。
func (l *JobLog) Result(size int64, sha1, resultDir string) {
	l.emit(Event{Type: TypeResult, File: "zip", Size: size, Sha1: sha1, ResultDir: resultDir})
}

// Exit 记录终态帧；state 取值见 PROTOCOL §3。
func (l *JobLog) Exit(code int, state, message string) {
	exitCode := code
	l.emit(Event{Type: TypeExit, Code: &exitCode, State: state, Message: message})
}

// Subscribe 订阅事件：先返回 seq > fromSeq 的历史（环形缓冲），再持续推送新事件。
//
// 返回的 cancel 必须由调用方执行（WS 断开时）。慢消费者会被摘除（channel 关闭），
// 由 Server 带 fromSeq 重连补发，避免阻塞构建进程的输出。
func (l *JobLog) Subscribe(fromSeq int) ([]Event, <-chan Event, func()) {
	l.mu.Lock()
	defer l.mu.Unlock()
	backlog := make([]Event, 0, len(l.events))
	if fromSeq < 0 {
		fromSeq = 0
	}
	for _, event := range l.events {
		if event.Seq > fromSeq {
			backlog = append(backlog, event)
		}
	}
	channel := make(chan Event, subscriberBuffer)
	id := l.nextSub
	l.nextSub++
	l.subs[id] = channel
	cancel := func() {
		l.mu.Lock()
		defer l.mu.Unlock()
		if existing, ok := l.subs[id]; ok {
			delete(l.subs, id)
			close(existing)
		}
	}
	return backlog, channel, cancel
}

func (l *JobLog) emit(event Event) {
	l.mu.Lock()
	if l.closed {
		l.mu.Unlock()
		return
	}
	l.seq++
	event.Seq = l.seq
	if event.Type == TypeLog && l.file != nil {
		if _, err := l.file.WriteString(event.Line + "\n"); err != nil {
			_ = err
		}
	}
	// 环形缓冲：批量丢弃最老的四分之一，避免逐条搬移整个切片。
	if len(l.events) >= l.capacity+l.capacity/4 {
		drop := len(l.events) - l.capacity
		l.events = append(l.events[:0], l.events[drop:]...)
	}
	l.events = append(l.events, event)
	for id, channel := range l.subs {
		select {
		case channel <- event:
		default:
			// 慢消费者：摘除，等待带 fromSeq 重连补发。
			delete(l.subs, id)
			close(channel)
		}
	}
	l.mu.Unlock()
}

// truncateLine 把单行截断到 MaxLineBytes（按 UTF-8 边界，避免产生半个字符）。
func truncateLine(line string) string {
	line = strings.ReplaceAll(line, "\r\n", " ")
	line = strings.ReplaceAll(line, "\n", " ")
	if len(line) <= MaxLineBytes {
		return line
	}
	cut := MaxLineBytes
	for cut > 0 && (line[cut]&0xC0) == 0x80 {
		cut--
	}
	return line[:cut]
}
