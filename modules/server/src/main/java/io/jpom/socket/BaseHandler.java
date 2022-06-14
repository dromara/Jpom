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
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.system.init.OperateLogController;
import io.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@Slf4j
public abstract class BaseHandler extends TextWebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Map<String, Object> attributes = session.getAttributes();
		//
		this.showHelloMsg(attributes, session);
		//
		String permissionMsg = (String) attributes.get("permissionMsg");
		if (StrUtil.isNotEmpty(permissionMsg)) {
			this.sendMsg(session, permissionMsg);
			this.destroy(session);
			return;
		}
		this.afterConnectionEstablishedImpl(session);
	}

	protected void showHelloMsg(Map<String, Object> attributes, WebSocketSession session) {
		UserModel userInfo = (UserModel) attributes.get("userInfo");
		String payload = StrUtil.format("欢迎加入:{} 会话id:{} ", userInfo.getName(), session.getId() + StrUtil.CRLF);
		this.sendMsg(session, payload);
	}

	/**
	 * 建立会话后
	 *
	 * @param session 会话
	 * @throws Exception 异常
	 */
	protected void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {

	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.error(session.getId() + "socket 异常", exception);
		destroy(session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		destroy(session);
	}

	/**
	 * 关闭连接
	 *
	 * @param session session
	 */
	public abstract void destroy(WebSocketSession session);

	protected void sendMsg(WebSocketSession session, String msg) {
		try {
			SocketSessionUtil.send(session, msg);
		} catch (Exception e) {
			log.error("发送消息失败", e);
		}
	}

	/**
	 * 操作 websocket 日志
	 *
	 * @param cls        class
	 * @param attributes 属性
	 * @param reqData    请求数据
	 */
	protected void logOpt(Class<?> cls, Map<String, Object> attributes, Object reqData) {
		String ip = (String) attributes.get("ip");
		NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
		// 记录操作日志
		UserModel userInfo = (UserModel) attributes.get("userInfo");
		String workspaceId = (String) attributes.get("workspaceId");
		OperateLogController.CacheInfo cacheInfo = new OperateLogController.CacheInfo();
		cacheInfo.setIp(ip);
		Feature feature = cls.getAnnotation(Feature.class);
		MethodFeature method = feature.method();
//		Assert.state(feature != null && feature, "权限功能没有配置正确");
		cacheInfo.setClassFeature(feature.cls());
		cacheInfo.setWorkspaceId(workspaceId);
		cacheInfo.setMethodFeature(method);

		cacheInfo.setNodeModel(nodeModel);
		cacheInfo.setDataId(null);
		String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
		cacheInfo.setUserAgent(userAgent);
		cacheInfo.setReqData(JSONObject.toJSONString(reqData));

		//cacheInfo.setMethodFeature(execute);
		Object proxySession = attributes.get("proxySession");
		try {
			attributes.remove("proxySession");
			attributes.put("use_type", "WebSocket");
			attributes.put("class_type", cls.getName());
			OperateLogController operateLogController = SpringUtil.getBean(OperateLogController.class);
			operateLogController.log(userInfo, JSONObject.toJSONString(attributes), cacheInfo);
		} catch (Exception e) {
			log.error("记录操作日志异常", e);
		} finally {
			if (proxySession != null) {
				attributes.put("proxySession", proxySession);
			}
		}
	}

}
