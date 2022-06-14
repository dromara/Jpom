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
package io.jpom.socket.handler;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.forward.NodeUrl;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ProxySession;
import io.jpom.socket.ServiceFileTailWatcher;
import io.jpom.system.WebAopLog;
import io.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @since 2019/4/24
 */
@Feature(cls = ClassFeature.TOMCAT, method = MethodFeature.EXECUTE)
@Slf4j
public class TomcatHandler extends BaseProxyHandler {

	public TomcatHandler() {
		super(NodeUrl.Tomcat_Socket);
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{"tomcatId", attributes.get("tomcatId")};
	}

	@Override
	protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
		String tomcatId = (String) attributes.get("tomcatId");
		String fileName = json.getString("fileName");
		if (!JpomApplication.SYSTEM_ID.equals(tomcatId) && consoleCommandOp == ConsoleCommandOp.heart) {
			// 服务端心跳
			return null;
		}

		super.logOpt(this.getClass(), attributes, json);

		//
		if (consoleCommandOp == ConsoleCommandOp.showlog && JpomApplication.SYSTEM_ID.equals(tomcatId)) {
			WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
			// 进入管理页面后需要实时加载日志
			File file = FileUtil.file(webAopLog.getPropertyValue(), fileName);
			//
			File nowFile = (File) attributes.get("nowFile");
			if (nowFile != null && !nowFile.equals(file)) {
				// 离线上一个日志
				ServiceFileTailWatcher.offlineFile(file, session);
			}
			try {
				ServiceFileTailWatcher.addWatcher(file, session);
				attributes.put("nowFile", file);
			} catch (Exception io) {
				log.error("监听日志变化", io);
				SocketSessionUtil.send(session, io.getMessage());
			}
		}
		return null;
	}

	@Override
	protected String handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
		proxySession.send(json.toString());
		return null;
	}

	@Override
	public void destroy(WebSocketSession session) {
		super.destroy(session);
		ServiceFileTailWatcher.offline(session);
	}
}
