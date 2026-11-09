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
	"os"
	"path/filepath"
	"strings"

	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/script"
)

// HandleSystemLog serves /system_log (AgentWebSocketSystemLogHandle): tails a
// file below <base>/logs.
func (m *Manager) HandleSystemLog(c *httpx.Context) {
	session, err := upgrade(c)
	if err != nil {
		return
	}
	defer func() {
		m.Tail.DetachSession(session.ID)
		session.Close()
	}()
	if !m.authorized(session, c.R) {
		return
	}
	session.SetReadLimit(defaultTextLimit)
	_ = session.Send(m.translate(session, "i18n.plugin_end_log_connection_successful.9035"))

	currentFile := ""
	for {
		messageType, data, err := session.Read()
		if err != nil {
			return
		}
		if messageType != websocket.TextMessage {
			continue
		}
		request, err := decodeObject(data)
		if err != nil {
			continue
		}
		if bodyString(request, "op") == "heart" {
			continue
		}
		fileName := bodyString(request, "fileName")
		file, ok := resolveSubFile(m.App.LogsDir, fileName)
		if !ok {
			_ = session.Send(m.translate(session, "i18n.file_not_found.d952"))
			continue
		}
		if info, err := os.Stat(file); err != nil || info.IsDir() {
			_ = session.Send(m.translate(session, "i18n.file_not_found.d952"))
			continue
		}
		if currentFile != "" && currentFile != file {
			// 切换日志文件时先摘除旧监听
			m.Tail.DetachFile(currentFile, session.ID)
		}
		currentFile = file
		attached, err := m.Tail.Attach(file, session, m.systemLogCharset())
		if err != nil {
			_ = session.Send(err.Error())
			continue
		}
		if !attached {
			// Java addWatcher returns false silently here
			m.App.Log().Warn("system log file not found", "file", file)
		}
	}
}

// systemLogCharset resolves jpom.system.log-charset (UTF-8 when absent).
func (m *Manager) systemLogCharset() string {
	configured := ""
	if m.App.Config != nil {
		configured = m.App.Config.Jpom.System.LogCharset
	}
	return script.SystemLogCharset(configured)
}

// resolveSubFile resolves a client supplied relative path inside baseDir and
// rejects absolute paths and traversal (Java FileUtils.resolveSubFile). It also
// verifies the symlink resolved path stays inside baseDir.
func resolveSubFile(baseDir, relative string) (string, bool) {
	relative = strings.TrimSpace(relative)
	if baseDir == "" || relative == "" {
		return "", false
	}
	relative = strings.TrimPrefix(relative, "/")
	relative = strings.TrimPrefix(relative, "\\")
	if relative == "" {
		return "", false
	}
	baseAbs, err := filepath.Abs(baseDir)
	if err != nil {
		return "", false
	}
	targetAbs, err := filepath.Abs(filepath.Join(baseAbs, relative))
	if err != nil {
		return "", false
	}
	if !isSubPath(baseAbs, targetAbs) {
		return "", false
	}
	if resolved, err := filepath.EvalSymlinks(targetAbs); err == nil {
		if resolvedBase, err := filepath.EvalSymlinks(baseAbs); err == nil {
			if !isSubPath(resolvedBase, resolved) {
				return "", false
			}
		}
	}
	return targetAbs, true
}

// isSubPath reports whether target is base itself or a descendant of base.
func isSubPath(base, target string) bool {
	if target == base {
		return false
	}
	relative, err := filepath.Rel(base, target)
	if err != nil {
		return false
	}
	return relative != ".." && !strings.HasPrefix(relative, ".."+string(filepath.Separator))
}
