/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package ws

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"
)

const tailPollInterval = time.Second

// TailManager feeds log file changes to websocket sessions. It mirrors
// AgentFileTailWatcher/BaseFileTailWatcher: one watcher per file, shared by every
// session, with a small ring of the most recent lines for late joiners.
type TailManager struct {
	initReadLine int

	mu       sync.Mutex
	watchers map[string]*tailWatcher
}

// NewTailManager creates the tail manager; initReadLine is
// jpom.init-read-line (default 10, 0 disables the initial replay).
func NewTailManager(initReadLine int) *TailManager {
	if initReadLine < 0 {
		initReadLine = 0
	}
	return &TailManager{initReadLine: initReadLine, watchers: make(map[string]*tailWatcher)}
}

// Attach starts tailing path for the session. It reports false when the file
// does not exist or is not a regular file (the caller decides which message to
// send, exactly like the Java endpoints).
func (m *TailManager) Attach(path string, session *Session) (bool, error) {
	info, err := os.Stat(path)
	if err != nil || info.IsDir() {
		return false, nil
	}
	m.mu.Lock()
	watcher, exists := m.watchers[path]
	if !exists {
		watcher = &tailWatcher{
			manager:  m,
			path:     path,
			sessions: make(map[string]*Session),
			recent:   make([]string, 0, m.initReadLine),
			stop:     make(chan struct{}),
		}
		m.watchers[path] = watcher
	}
	m.mu.Unlock()
	if err := watcher.add(session); err != nil {
		return true, err
	}
	if !exists {
		go watcher.run()
	}
	return true, nil
}

// DetachSession removes a session from every file watcher (socket close).
func (m *TailManager) DetachSession(sessionID string) {
	m.mu.Lock()
	watchers := make([]*tailWatcher, 0, len(m.watchers))
	for _, watcher := range m.watchers {
		watchers = append(watchers, watcher)
	}
	m.mu.Unlock()
	for _, watcher := range watchers {
		watcher.remove(sessionID)
	}
}

// DetachFile removes one session from the watcher of path (log file switch).
func (m *TailManager) DetachFile(path, sessionID string) {
	m.mu.Lock()
	watcher := m.watchers[path]
	m.mu.Unlock()
	if watcher != nil {
		watcher.remove(sessionID)
	}
}

// CloseAll stops every watcher (agent shutdown).
func (m *TailManager) CloseAll() {
	m.mu.Lock()
	watchers := make([]*tailWatcher, 0, len(m.watchers))
	for _, watcher := range m.watchers {
		watchers = append(watchers, watcher)
	}
	m.mu.Unlock()
	for _, watcher := range watchers {
		watcher.close()
	}
}

// tailWatcher tails one file.
type tailWatcher struct {
	manager *TailManager
	path    string

	mu       sync.Mutex
	sessions map[string]*Session
	recent   []string
	offset   int64
	pending  []byte
	closed   bool
	stop     chan struct{}
}

// add registers a session, sends the welcome frame and the recent lines.
func (w *tailWatcher) add(session *Session) error {
	w.mu.Lock()
	if w.closed {
		w.mu.Unlock()
		return errTailClosed
	}
	if _, ok := w.sessions[session.ID]; ok {
		w.mu.Unlock()
		return nil
	}
	w.sessions[session.ID] = session
	banner := fmt.Sprintf("监听%s日志成功,目前共有%d个会话正在查看", filepath.Base(w.path), len(w.sessions))
	recent := make([]string, len(w.recent))
	copy(recent, w.recent)
	w.mu.Unlock()
	if err := session.Send(banner); err != nil {
		w.remove(session.ID)
		return err
	}
	for _, line := range recent {
		if err := session.Send(line); err != nil {
			w.remove(session.ID)
			return err
		}
	}
	return nil
}

var errTailClosed = fmt.Errorf("文件监听已关闭")

// remove unregisters a session and closes the watcher when it becomes empty.
func (w *tailWatcher) remove(sessionID string) {
	w.mu.Lock()
	if _, ok := w.sessions[sessionID]; !ok {
		w.mu.Unlock()
		return
	}
	delete(w.sessions, sessionID)
	empty := len(w.sessions) == 0
	w.mu.Unlock()
	if empty {
		w.close()
	}
}

// close stops the watcher and removes it from the manager cache.
func (w *tailWatcher) close() {
	w.mu.Lock()
	if w.closed {
		w.mu.Unlock()
		return
	}
	w.closed = true
	w.sessions = make(map[string]*Session)
	close(w.stop)
	w.mu.Unlock()
	w.manager.mu.Lock()
	if current, ok := w.manager.watchers[w.path]; ok && current == w {
		delete(w.manager.watchers, w.path)
	}
	w.manager.mu.Unlock()
}

// sendAll writes a line to every session; sessions that fail are removed.
func (w *tailWatcher) sendAll(line string) {
	w.mu.Lock()
	if w.closed {
		w.mu.Unlock()
		return
	}
	sessions := make([]*Session, 0, len(w.sessions))
	for _, session := range w.sessions {
		sessions = append(sessions, session)
	}
	w.mu.Unlock()
	for _, session := range sessions {
		if err := session.Send(line); err != nil {
			w.remove(session.ID)
		}
	}
}

// remember appends a line to the recent ring and forwards it.
func (w *tailWatcher) remember(line string) {
	w.mu.Lock()
	limit := w.manager.initReadLine
	if limit > 0 {
		if len(w.recent) >= limit {
			w.recent = append(w.recent[len(w.recent)-limit+1:], line)
		} else {
			w.recent = append(w.recent, line)
		}
	}
	w.mu.Unlock()
	w.sendAll(line)
}

// run replays the last initReadLine lines and follows the file.
func (w *tailWatcher) run() {
	defer w.close()
	w.replayTail()
	ticker := time.NewTicker(tailPollInterval)
	defer ticker.Stop()
	for {
		select {
		case <-w.stop:
			return
		case <-ticker.C:
			w.poll()
		}
	}
}

// replayTail sends the last initReadLine lines (Hutool Tailer initial read).
func (w *tailWatcher) replayTail() {
	limit := w.manager.initReadLine
	if limit <= 0 {
		if info, err := os.Stat(w.path); err == nil {
			w.mu.Lock()
			w.offset = info.Size()
			w.mu.Unlock()
		}
		return
	}
	file, err := os.Open(w.path)
	if err != nil {
		return
	}
	defer func() { _ = file.Close() }()
	info, err := file.Stat()
	if err != nil {
		return
	}
	const window = 256 * 1024
	start := info.Size() - window
	if start < 0 {
		start = 0
	}
	if _, err := file.Seek(start, 0); err != nil {
		return
	}
	buffer := make([]byte, info.Size()-start)
	read, _ := file.Read(buffer)
	text := string(buffer[:read])
	if start > 0 {
		// 丢弃可能被截断的首行
		if index := strings.Index(text, "\n"); index >= 0 {
			text = text[index+1:]
		}
	}
	text = strings.TrimSuffix(text, "\n")
	lines := []string{}
	if text != "" {
		lines = strings.Split(text, "\n")
	}
	// 只保留最后 initReadLine 行
	if len(lines) > limit {
		lines = lines[len(lines)-limit:]
	}
	for _, line := range lines {
		w.remember(strings.TrimSuffix(line, "\r"))
	}
	w.mu.Lock()
	w.offset = info.Size()
	w.mu.Unlock()
}

// poll reads new bytes appended to the file (and handles truncation/rotation).
func (w *tailWatcher) poll() {
	info, err := os.Stat(w.path)
	if err != nil {
		return
	}
	w.mu.Lock()
	offset := w.offset
	pending := w.pending
	w.mu.Unlock()
	if info.Size() < offset {
		// 文件被清空或轮转，从头读取，并提示前端重新监听（Java reWatcher）
		offset = 0
		pending = nil
		w.sendAll("Relisten to the file............")
	}
	if info.Size() <= offset {
		w.mu.Lock()
		w.offset = offset
		w.pending = pending
		w.mu.Unlock()
		return
	}
	file, err := os.Open(w.path)
	if err != nil {
		return
	}
	defer func() { _ = file.Close() }()
	if _, err := file.Seek(offset, 0); err != nil {
		return
	}
	buffer := make([]byte, info.Size()-offset)
	read, _ := file.Read(buffer)
	data := append(pending, buffer[:read]...)
	lastNewline := strings.LastIndex(string(data), "\n")
	if lastNewline < 0 {
		w.mu.Lock()
		w.pending = data
		w.mu.Unlock()
		return
	}
	completed := string(data[:lastNewline])
	rest := append([]byte{}, data[lastNewline+1:]...)
	for _, line := range strings.Split(completed, "\n") {
		w.remember(strings.TrimSuffix(line, "\r"))
	}
	w.mu.Lock()
	w.offset = offset + int64(lastNewline+1)
	w.pending = rest
	w.mu.Unlock()
}
