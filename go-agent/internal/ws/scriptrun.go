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
	if scriptID == "" || workspaceID == "" {
		// Java 端只发提示，不关闭会话
		_ = session.Send("脚本模板或者工作空间未知")
		return
	}
	if _, exists, err := m.Scripts.Get(scriptID); err != nil {
		closeWithMessage(session, "系统错误!")
		return
	} else if !exists {
		_ = session.Send("没有找到对应的脚本模板")
		return
	}
	if item, exists, _ := m.Scripts.Get(scriptID); exists {
		_ = session.Send("连接成功：" + bodyString(item, "name"))
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
			_ = session.Send("没有对应脚本模板:" + scriptID)
			return
		}
		operation := bodyString(request, "op")
		switch operation {
		case "start":
			executeID := bodyString(request, "executeId")
			if executeID == "" {
				_ = session.Send("没有执行ID")
				return
			}
			if err := m.Executor.Attach(item, executeID, bodyString(request, "args"), session.ID, session.Send, session.Lang); err != nil {
				_ = session.Send("执行异常：" + err.Error())
				return
			}
		case "stop":
			executeID := bodyString(request, "executeId")
			if executeID == "" {
				_ = session.Send("没有执行ID")
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
		frame["msg"] = "执行成功"
		_ = session.SendJSON(frame)
	}
}
