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
	"time"

	"github.com/gorilla/websocket"

	"github.com/dromara/Jpom/go-agent/internal/httpx"
	"github.com/dromara/Jpom/go-agent/internal/i18n"
	"github.com/dromara/Jpom/go-agent/internal/project"
	"github.com/dromara/Jpom/go-agent/internal/script"
)

// systemProjectID is the pseudo project id used by the index monitor console
// (Const.SYSTEM_ID).
const systemProjectID = "system"

// ConsoleBackend provides the project console operations. It is implemented by
// the project domain (internal/project.Runtime); tests may replace it.
type ConsoleBackend interface {
	// Project returns one project by id.
	Project(id string) (map[string]any, bool)
	// Execute runs a console command (start/stop/restart/reload/status) and
	// returns its success flag, failure message and CommandOpResult data.
	Execute(op string, info map[string]any) (success bool, message string, data any)
	// UpdateModifyUser records the console operator on the project.
	UpdateModifyUser(projectID, user string)
	// CanReload reports whether the DSL project configures the reload process.
	CanReload(info map[string]any) bool
}

// ProjectConsoleBackend is the production ConsoleBackend built on the project
// runtime.
type ProjectConsoleBackend struct {
	runtime *project.Runtime
	lang    string
}

// NewProjectConsoleBackend creates the project console backend.
func NewProjectConsoleBackend(runtime *project.Runtime) *ProjectConsoleBackend {
	return &ProjectConsoleBackend{runtime: runtime, lang: i18n.DefaultLang()}
}

// translateDefault renders a backend message with the configured agent language.
func (b *ProjectConsoleBackend) translateDefault(key string, args ...any) string {
	return i18n.T(b.lang, key, args...)
}

// Project implements ConsoleBackend.
func (b *ProjectConsoleBackend) Project(id string) (map[string]any, bool) {
	info, exists, err := b.runtime.Manager.Get(id)
	if err != nil {
		return nil, false
	}
	return info, exists
}

// Execute implements ConsoleBackend.
func (b *ProjectConsoleBackend) Execute(op string, info map[string]any) (bool, string, any) {
	result, err := b.runtime.Operate(op, info)
	if err != nil {
		return false, err.Error(), nil
	}
	if result == nil {
		return false, b.translateDefault("i18n.operation_failed.3d94"), nil
	}
	if op == "status" {
		return result.Success, "", result.JSON()
	}
	if result.Success {
		return true, "", result.JSON()
	}
	return false, result.MsgStr(), nil
}

// UpdateModifyUser implements ConsoleBackend.
func (b *ProjectConsoleBackend) UpdateModifyUser(projectID, user string) {
	if projectID == "" || user == "" || user == "-" {
		return
	}
	_ = b.runtime.Manager.Mutate(func(values map[string]map[string]any) error {
		info, ok := values[projectID]
		if !ok || info == nil {
			return nil
		}
		info["modifyUser"] = user
		values[projectID] = info
		return nil
	})
}

// CanReload implements ConsoleBackend.
func (b *ProjectConsoleBackend) CanReload(info map[string]any) bool {
	canReload, _, err := b.runtime.DslProcessInfo(info)
	return err == nil && canReload
}

// NewDefaultConsoleBackend creates the production console backend.
func NewDefaultConsoleBackend(runtime *project.Runtime) ConsoleBackend {
	return NewProjectConsoleBackend(runtime)
}

// HandleConsole serves /console (AgentWebSocketConsoleHandle): project console
// log tailing plus lifecycle commands.
func (m *Manager) HandleConsole(c *httpx.Context) {
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
	projectID := query(c.R, "projectId")
	current := map[string]any(nil)
	if projectID != systemProjectID {
		info, exists := m.Console.Project(projectID)
		if !exists {
			_ = session.Send(m.translate(session, "i18n.no_project_specified2.a7f5") + projectID)
			session.Close()
			return
		}
		current = info
		_ = session.Send(m.translate(session, "i18n.connection_successful_with_message.5cf2") + projectName(info))
	}
	currentFile := ""
	for {
		_ = session.SetReadDeadline(time.Now().Add(consoleIdleTimeout))
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
		operation := bodyString(request, "op")
		if operation == "heart" {
			// 静默心跳
			continue
		}
		id := bodyString(request, "projectId")
		if id == "" {
			id = projectID
		}
		info, exists := m.Console.Project(id)
		if !exists {
			_ = session.Send(m.translate(session, "i18n.no_project_specified2.a7f5") + id)
			session.Close()
			return
		}
		current = info
		if bodyString(info, "runMode") == "Dsl" {
			request["canReload"] = m.Console.CanReload(info)
		}
		switch operation {
		case "start", "restart", "stop", "reload":
			success, message, resultData := m.Console.Execute(operation, info)
			m.Console.UpdateModifyUser(id, session.OptUser)
			if success {
				_ = session.SendJSON(controlFrame(request, 200, m.translate(session, "i18n.operation_succeeded.3313"), resultData))
			} else {
				_ = session.SendJSON(controlFrame(request, 400, message, nil))
			}
		case "status":
			success, _, resultData := m.Console.Execute(operation, info)
			if success {
				_ = session.SendJSON(controlFrame(request, 200, m.translate(session, "i18n.running_status.d679"), resultData))
			} else {
				_ = session.SendJSON(controlFrame(request, 404, m.translate(session, "i18n.not_running.4f8a"), resultData))
			}
		case "showlog":
			if requestString(request, "search") != "" {
				m.searchProjectLog(session, current, request, m.projectLogCharset(current))
				continue
			}
			file, ok := m.projectLogFile(current, bodyString(request, "fileName"))
			if !ok {
				_ = session.Send(m.translate(session, "i18n.file_not_found.d952"))
				continue
			}
			if currentFile != "" && currentFile != file {
				// 切换日志文件时先摘除旧监听（Java 只离线旧 watcher，不发 Relisten；
				// Relisten 仅由 reWatcher——日志文件被重置时——发送）
				m.Tail.DetachFile(currentFile, session.ID)
			}
			currentFile = file
			attached, err := m.Tail.Attach(file, session, m.projectLogCharset(current))
			if err != nil {
				_ = session.Send(err.Error())
				continue
			}
			if !attached {
				_ = session.Send(m.translate(session, "i18n.listen_file_failed_may_not_exist.fd56"))
			}
		default:
			_ = session.SendJSON(controlFrame(request, 404, m.translate(session, "i18n.unsupported_method_with_colon.eae8")+operation, nil))
		}
	}
}

// projectName returns the project display name.
func projectName(info map[string]any) string {
	if value, ok := info["name"].(string); ok {
		return value
	}
	return ""
}

// projectLibDir returns the project lib directory (projectPath).
func (m *Manager) projectLibDir(info map[string]any) (string, error) {
	return project.ProjectPath(info)
}

// projectLogCharset resolves the project console log charset. The project
// runtime owns the Java compatible resolution (linked project logCharset ->
// jpom.project.log.file-charset -> platform default); the local fallback keeps
// the websocket usable when no runtime was wired (tests).
func (m *Manager) projectLogCharset(info map[string]any) string {
	if m.ProjectRuntime != nil {
		return m.ProjectRuntime.ResolveLogCharset(info)
	}
	projectCharset := project.GetString(info, "logCharset")
	configured := ""
	if m.App.Config != nil {
		configured = m.App.Config.Jpom.Project.Log.FileCharset
	}
	return script.ProjectLogCharset(projectCharset, configured)
}

// projectLogFile resolves the console log file to tail. An empty fileName means
// the default <project-log-dir>/<projectId>.log; otherwise the file is resolved
// inside the project lib directory (Java resolveAbsoluteLogFile / resolveSubFile).
func (m *Manager) projectLogFile(info map[string]any, fileName string) (string, bool) {
	if fileName == "" {
		// 默认控制台日志：<project-log>/<id>/<id>.log（Link 项目解析到被软链项目）
		file, err := m.Projects.ResolveLogFile(info)
		if err != nil {
			return "", false
		}
		return file, true
	}
	libDir, err := project.ProjectPath(info)
	if err != nil {
		return "", false
	}
	return resolveSubFile(libDir, fileName)
}
