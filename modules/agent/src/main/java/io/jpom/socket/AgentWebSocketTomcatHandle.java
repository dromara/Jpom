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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.service.manage.TomcatEditService;
import io.jpom.system.WebAopLog;
import io.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件端,控制台socket
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@ServerEndpoint(value = "/tomcat_log")
@Component
@Slf4j
public class AgentWebSocketTomcatHandle extends BaseAgentWebSocketHandle {

	private TomcatEditService tomcatEditService;

	private static final Map<String, File> CACHE_FILE = new ConcurrentHashMap<>();

	@OnOpen
	public void onOpen(Session session) {
		try {
			if (super.checkAuthorize(session)) {
				return;
			}
			String tomcatId = super.getParameters(session, "tomcatId");
			if (tomcatEditService == null) {
				tomcatEditService = SpringUtil.getBean(TomcatEditService.class);
			}
			TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(tomcatId);
			if (tomcatInfoModel == null && !JpomApplication.SYSTEM_ID.equalsIgnoreCase(tomcatId)) {
				SocketSessionUtil.send(session, "获取tomcat信息错误");
				session.close();
				return;
			}
			SocketSessionUtil.send(session, "连接成功：" + (tomcatInfoModel == null ? "" : tomcatInfoModel.getName()));
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
		String op = json.getString("op");
		ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
		if (consoleCommandOp == ConsoleCommandOp.heart) {
			return;
		}
		String tomcatId = json.getString("tomcatId");
		if (JpomApplication.SYSTEM_ID.equalsIgnoreCase(tomcatId)) {
			runMsg(session, json);
		} else {
			TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(tomcatId);
			if (tomcatInfoModel == null) {
				SocketSessionUtil.send(session, "没有对应tomcat");
				session.close();
				return;
			}
			runMsg(session, tomcatInfoModel, json);
		}
	}

	private void runMsg(Session session, JSONObject reqJson) throws Exception {
		try {
			String fileName = reqJson.getString("fileName");
			WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
			// 进入管理页面后需要实时加载日志
			File file = FileUtil.file(webAopLog.getPropertyValue(), fileName);
			File file1 = CACHE_FILE.get(session.getId());
			if (file1 != null && !file1.equals(file)) {
				// 离线上一个日志
				AgentFileTailWatcher.offlineFile(file, session);
			}
			try {
				AgentFileTailWatcher.addWatcher(file, session);
				CACHE_FILE.put(session.getId(), file);
			} catch (IOException io) {
				log.error("监听日志变化", io);
				SocketSessionUtil.send(session, io.getMessage());
			}
		} catch (Exception e) {
			log.error("执行命令失败", e);
			SocketSessionUtil.send(session, "执行命令失败,详情如下：");
			SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
		}
	}

	private void runMsg(Session session, TomcatInfoModel tomcatInfoModel, JSONObject reqJson) throws Exception {
		try {
			String fileName = reqJson.getString("fileName");
			// 进入管理页面后需要实时加载日志
			File file = FileUtil.file(tomcatInfoModel.getPath(), "logs", fileName);
			try {
				AgentFileTailWatcher.addWatcher(file, session);
			} catch (IOException io) {
				log.error("监听日志变化", io);
				SocketSessionUtil.send(session, io.getMessage());
			}
		} catch (Exception e) {
			log.error("执行命令失败", e);
			SocketSessionUtil.send(session, "执行命令失败,详情如下：");
			SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
		}
	}

	@Override
	@OnClose
	public void onClose(Session session) {
		super.onClose(session);
	}

	@OnError
	@Override
	public void onError(Session session, Throwable thr) {
		super.onError(session, thr);
	}
}
