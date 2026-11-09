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

	"jpom.io/go-agent/internal/httpx"
	"jpom.io/go-agent/internal/project"
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
}

// NewProjectConsoleBackend creates the project console backend.
func NewProjectConsoleBackend(runtime *project.Runtime) *ProjectConsoleBackend {
	return &ProjectConsoleBackend{runtime: runtime}
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
		return false, "操作失败", nil
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
			_ = session.Send("没有对应项目：" + projectID)
			session.Close()
			return
		}
		current = info
		_ = session.Send("连接成功：" + projectName(info))
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
			_ = session.Send("没有对应项目：" + id)
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
				_ = session.SendJSON(controlFrame(request, 200, "操作成功", resultData))
			} else {
				_ = session.SendJSON(controlFrame(request, 400, message, nil))
			}
		case "status":
			success, _, resultData := m.Console.Execute(operation, info)
			if success {
				_ = session.SendJSON(controlFrame(request, 200, "运行中", resultData))
			} else {
				_ = session.SendJSON(controlFrame(request, 404, "未运行", resultData))
			}
		case "showlog":
			if requestString(request, "search") != "" {
				m.searchProjectLog(session, current, request)
				continue
			}
			file, ok := m.projectLogFile(current, bodyString(request, "fileName"))
			if !ok {
				_ = session.Send("文件不存在")
				continue
			}
			if currentFile != "" && currentFile != file {
				// 切换日志文件时先摘除旧监听
				m.Tail.DetachFile(currentFile, session.ID)
				_ = session.Send("Relisten to the file............")
			}
			currentFile = file
			attached, err := m.Tail.Attach(file, session)
			if err != nil {
				_ = session.Send(err.Error())
				continue
			}
			if !attached {
				_ = session.Send("监听文件失败,可能文件不存在")
			}
		default:
			_ = session.SendJSON(controlFrame(request, 404, "不支持的方式："+operation, nil))
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
