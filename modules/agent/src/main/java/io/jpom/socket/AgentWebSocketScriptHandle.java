/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.socket;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.script.ScriptProcessBuilder;
import io.jpom.service.script.NodeScriptServer;
import io.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * 脚本模板socket
 *
 * @author jiangzeyin
 * @since 2019/4/24
 */
@ServerEndpoint(value = "/script_run")
@Component
@Slf4j
public class AgentWebSocketScriptHandle extends BaseAgentWebSocketHandle {

	private NodeScriptServer nodeScriptServer;

	@OnOpen
	public void onOpen(Session session) {
		if (nodeScriptServer == null) {
			nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
		}
		try {
			if (super.checkAuthorize(session)) {
				return;
			}
			String id = this.getParameters(session, "id");
			if (StrUtil.isEmpty(id)) {
				SocketSessionUtil.send(session, "脚本模板未知");
				return;
			}
			NodeScriptModel nodeScriptModel = nodeScriptServer.getItem(id);
			if (nodeScriptModel == null) {
				SocketSessionUtil.send(session, "没有找到对应的脚本模板");
				return;
			}
			SocketSessionUtil.send(session, "连接成功：" + nodeScriptModel.getName());
		} catch (Exception e) {
			log.error("socket 错误", e);
			try {
				SocketSessionUtil.send(session, JsonMessage.getString(500, "系统错误!"));
				session.close();
			} catch (IOException e1) {
				log.error(e1.getMessage(), e1);
			}
		}
	}

	@OnMessage
	public void onMessage(String message, Session session) throws Exception {
		JSONObject json = JSONObject.parseObject(message);
		String scriptId = json.getString("scriptId");
		NodeScriptModel nodeScriptModel = nodeScriptServer.getItem(scriptId);
		if (nodeScriptModel == null) {
			SocketSessionUtil.send(session, "没有对应脚本模板:" + scriptId);
			session.close();
			return;
		}
		String op = json.getString("op");
		ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
		switch (consoleCommandOp) {
			case start: {
				String args = json.getString("args");
				String executeId = json.getString("executeId");
				if (StrUtil.isEmpty(executeId)) {
					SocketSessionUtil.send(session, "没有执行ID");
					session.close();
					return;
				}
				ScriptProcessBuilder.addWatcher(nodeScriptModel, executeId, args, session);
				break;
			}
			case stop: {
				String executeId = json.getString("executeId");
				if (StrUtil.isEmpty(executeId)) {
					SocketSessionUtil.send(session, "没有执行ID");
					session.close();
					return;
				}
				ScriptProcessBuilder.stopRun( executeId);
				break;
			}
			case heart:
			default:
				return;
		}
		// 记录操作人
		nodeScriptModel = nodeScriptServer.getItem(scriptId);
		String name = getOptUserName(session);
		nodeScriptModel.setLastRunUser(name);
		nodeScriptServer.updateItem(nodeScriptModel);
		json.put("code", 200);
		json.put("msg", "执行成功");
		log.info(json.toString());
		SocketSessionUtil.send(session, json.toString());
	}


	@Override
	@OnClose
	public void onClose(Session session) {
		super.onClose(session);
		ScriptProcessBuilder.stopWatcher(session);
	}

	@OnError
	@Override
	public void onError(Session session, Throwable thr) {
		super.onError(session, thr);
	}
}
