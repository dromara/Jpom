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
	"github.com/gorilla/websocket"

	"jpom.io/go-agent/internal/httpx"
)

// HandleScriptRun serves /script_run (AgentWebSocketScriptHandle): it attaches
// the console of a running script execution and streams the execution log.
func (m *Manager) HandleScriptRun(c *httpx.Context) {
	session, err := upgrade(c)
	if err != nil {
		return
	}
	defer func() {
		m.Executor.Detach(session.ID)
		session.Close()
	}()
	if !m.authorized(session, c.R) {
		return
	}
	scriptID := query(c.R, "id")
	workspaceID := query(c.R, "workspaceId")
	// Java 的 onOpen 在参数缺失/脚本不存在时只发送提示帧、不关闭会话；
	// 后续 onMessage 仍会按消息里的 scriptId 处理（不存在时再提示并关闭）。
	if scriptID == "" || workspaceID == "" {
		_ = session.Send(m.translate(session, "i18n.unknown_script_template_or_workspace.27f1"))
	} else if item, exists, err := m.Scripts.Get(scriptID); err != nil {
		closeWithMessage(session, m.translate(session, "i18n.system_error.9417"))
		return
	} else if !exists {
		_ = session.Send(m.translate(session, "i18n.no_script_template_found.0498"))
	} else {
		_ = session.Send(m.translate(session, "i18n.connection_successful_with_message.5cf2") + bodyString(item, "name"))
	}
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
		scriptID := bodyString(request, "scriptId")
		item, exists, err := m.Scripts.Get(scriptID)
		if err != nil || !exists {
			_ = session.Send(m.translate(session, "i18n.no_script_template_specified.7d14") + scriptID)
			return
		}
		operation := bodyString(request, "op")
		switch operation {
		case "start":
			executeID := bodyString(request, "executeId")
			if executeID == "" {
				_ = session.Send(m.translate(session, "i18n.no_execution_id.68dc"))
				return
			}
			if err := m.Executor.Attach(item, executeID, bodyString(request, "args"), session.ID, session.Send, session.Lang); err != nil {
				_ = session.Send(m.translate(session, "i18n.general_execution_exception.62e9") + err.Error())
				return
			}
		case "stop":
			executeID := bodyString(request, "executeId")
			if executeID == "" {
				_ = session.Send(m.translate(session, "i18n.no_execution_id.68dc"))
				return
			}
			m.Executor.Stop(executeID)
		case "heart":
			continue
		default:
			continue
		}
		// 记录最后执行人（Java 在发送控制帧前更新 script.json）
		user := session.OptUser
		if user == "" {
			user = "-"
		}
		_ = m.Scripts.UpdateFields(scriptID, map[string]any{"lastRunUser": user})
		// Java 只是把 code/msg 追加回原始 json：服务端为 start 帧加了 JPOM_MSG，
		// stop 帧没有加，因此这里不能无条件追加 JPOM_MSG。
		frame := make(map[string]any, len(request)+2)
		for key, value := range request {
			frame[key] = value
		}
		frame["code"] = 200
		frame["msg"] = m.translate(session, "i18n.execution_succeeded.f56c")
		_ = session.SendJSON(frame)
	}
}
