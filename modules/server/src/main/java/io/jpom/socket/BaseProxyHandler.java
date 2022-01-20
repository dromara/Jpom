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
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.system.init.OperateLogController;
import io.jpom.util.SocketSessionUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * 服务端socket 基本类
 *
 * @author jiangzeyin
 * @date 2019/4/25
 */
public abstract class BaseProxyHandler extends BaseHandler {

	private final NodeUrl nodeUrl;

	public BaseProxyHandler(NodeUrl nodeUrl) {
		this.nodeUrl = nodeUrl;
	}

	/**
	 * 连接参数
	 *
	 * @param attributes 属性
	 * @return key, value, key, value.....
	 */
	protected abstract Object[] getParameters(Map<String, Object> attributes);

	/**
	 * 是否输出连接成功 消息
	 *
	 * @return true 输出
	 */
	protected boolean showHelloMsg() {
		return true;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Map<String, Object> attributes = session.getAttributes();
		this.init(session, attributes);
	}

	/**
	 * 连接成功 初始化
	 *
	 * @param session    会话
	 * @param attributes 属性
	 * @throws URISyntaxException 异常
	 * @throws IOException        IO
	 */
	protected void init(WebSocketSession session, Map<String, Object> attributes) throws URISyntaxException, IOException {
		boolean init = (boolean) attributes.getOrDefault("init", false);
		if (init) {
			return;
		}
		NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
		UserModel userInfo = (UserModel) attributes.get("userInfo");

		if (nodeModel != null) {
			Object[] parameters = this.getParameters(attributes);
			String url = NodeForward.getSocketUrl(nodeModel, nodeUrl, userInfo, parameters);
			// 连接节点
			ProxySession proxySession = new ProxySession(url, session);
			session.getAttributes().put("proxySession", proxySession);
		}
		if (this.showHelloMsg()) {
			String payload = StrUtil.format("欢迎加入:{} 会话id:{} ", userInfo.getName(), session.getId());
			this.sendMsg(session, payload);
		}
		attributes.put("init", true);
	}

	protected void sendMsg(WebSocketSession session, String msg) {
		try {
			SocketSessionUtil.send(session, msg);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("发送消息失败", e);
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		String msg = message.getPayload();
		Map<String, Object> attributes = session.getAttributes();
		ProxySession proxySession = (ProxySession) attributes.get("proxySession");
		JSONObject json = JSONObject.parseObject(msg);
		String op = json.getString("op");
		ConsoleCommandOp consoleCommandOp = StrUtil.isNotEmpty(op) ? ConsoleCommandOp.valueOf(op) : null;
		String textMessage;
		if (proxySession != null) {
			textMessage = this.handleTextMessage(attributes, proxySession, json, consoleCommandOp);
		} else {
			textMessage = this.handleTextMessage(attributes, session, json, consoleCommandOp);
		}
		if (textMessage != null) {
			this.sendMsg(session, textMessage);
		}
	}

	/**
	 * 消息处理方法
	 *
	 * @param attributes       属性
	 * @param session          当前回话
	 * @param json             数据
	 * @param consoleCommandOp 操作类型
	 */
	protected String handleTextMessage(Map<String, Object> attributes,
									   WebSocketSession session,
									   JSONObject json,
									   ConsoleCommandOp consoleCommandOp) throws IOException {
		return null;
	}

	/**
	 * 消息处理方法
	 *
	 * @param attributes       属性
	 * @param proxySession     代理回话
	 * @param json             数据
	 * @param consoleCommandOp 操作类型
	 */
	protected String handleTextMessage(Map<String, Object> attributes,
									   ProxySession proxySession,
									   JSONObject json,
									   ConsoleCommandOp consoleCommandOp) {
		return null;
	}

	public static void logOpt(Class<?> cls, Map<String, Object> attributes,
							  Object reqData) {
		String ip = (String) attributes.get("ip");
		NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
		OperateLogController.CacheInfo cacheInfo = new OperateLogController.CacheInfo();
		cacheInfo.setIp(ip);
		Feature feature = cls.getAnnotation(Feature.class);
		Assert.state(feature != null && feature.cls() != ClassFeature.NULL, "权限功能没有配置正确");
		cacheInfo.setClassFeature(feature.cls());
		cacheInfo.setMethodFeature(MethodFeature.EXECUTE);
		cacheInfo.setNodeModel(nodeModel);
		cacheInfo.setDataId(null);
		String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
		cacheInfo.setUserAgent(userAgent);
		cacheInfo.setReqData(JSONObject.toJSONString(reqData));

		// 记录操作日志
		UserModel userInfo = (UserModel) attributes.get("userInfo");
		cacheInfo.setMethodFeature(MethodFeature.EXECUTE);
		Object proxySession = attributes.get("proxySession");
		try {
			attributes.remove("proxySession");
			attributes.put("use_type", "WebSocket");
			attributes.put("class_type", cls.getName());
			OperateLogController operateLogController = SpringUtil.getBean(OperateLogController.class);
			operateLogController.log(userInfo, JSONObject.toJSONString(attributes), cacheInfo);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("记录操作日志异常", e);
		} finally {
			if (proxySession != null) {
				attributes.put("proxySession", proxySession);
			}
		}
	}

	protected void logOpt(Map<String, Object> attributes,
						  JSONObject json) {
		logOpt(this.getClass(), attributes, json);
	}

	@Override
	public void destroy(WebSocketSession session) {
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (IOException ignored) {
		}
		Map<String, Object> attributes = session.getAttributes();
		ProxySession proxySession = (ProxySession) attributes.get("proxySession");
		if (proxySession != null) {
			proxySession.close();
		}
	}
}
