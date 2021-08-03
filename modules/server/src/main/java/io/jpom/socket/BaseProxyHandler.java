package io.jpom.socket;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.system.init.OperateLogController;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端socket 基本类
 *
 * @author jiangzeyin
 * @date 2019/4/25
 */
public abstract class BaseProxyHandler extends BaseHandler {
	protected OperateLogController operateLogController;

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
			session.sendMessage(new TextMessage(StrUtil.format("欢迎加入:{} 会话id:{} ", userInfo.getName(), session.getId())));
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		if (operateLogController == null) {
			operateLogController = SpringUtil.getBean(OperateLogController.class);
		}
		String msg = message.getPayload();
		Map<String, Object> attributes = session.getAttributes();
		ProxySession proxySession = (ProxySession) attributes.get("proxySession");
		JSONObject json = JSONObject.parseObject(msg);
		String op = json.getString("op");
		ConsoleCommandOp consoleCommandOp = StrUtil.isNotEmpty(op) ? ConsoleCommandOp.valueOf(op) : null;
		if (proxySession != null) {
			this.handleTextMessage(attributes, proxySession, json, consoleCommandOp);
		} else {
			this.handleTextMessage(attributes, session, json, consoleCommandOp);
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
	protected void handleTextMessage(Map<String, Object> attributes,
									 WebSocketSession session,
									 JSONObject json,
									 ConsoleCommandOp consoleCommandOp) throws IOException {
	}

	/**
	 * 消息处理方法
	 *
	 * @param attributes       属性
	 * @param proxySession     代理回话
	 * @param json             数据
	 * @param consoleCommandOp 操作类型
	 */
	protected void handleTextMessage(Map<String, Object> attributes,
									 ProxySession proxySession,
									 JSONObject json,
									 ConsoleCommandOp consoleCommandOp) {
	}

	protected OperateLogController.CacheInfo cacheInfo(Map<String, Object> attributes, JSONObject json, UserOperateLogV1.OptType optType, String dataId) {
		String ip = (String) attributes.get("ip");
		NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
		OperateLogController.CacheInfo cacheInfo = new OperateLogController.CacheInfo();
		cacheInfo.setIp(ip);
		cacheInfo.setOptType(optType);
		cacheInfo.setNodeModel(nodeModel);
		cacheInfo.setDataId(dataId);
		String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
		cacheInfo.setUserAgent(userAgent);

		cacheInfo.setReqData(json.toString());
		return cacheInfo;
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
