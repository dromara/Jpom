/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AgentFileModel;
import io.jpom.model.WebSocketMessageModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.permission.SystemPermission;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.SystemParametersServer;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.client.NodeClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 节点管理控制器
 *
 * @author lf
 */
@SystemPermission
@Feature(cls = ClassFeature.UPGRADE_NODE_LIST)
public class NodeUpdateHandler extends BaseProxyHandler {

	private final ConcurrentMap<String, NodeClient> clientMap = new ConcurrentHashMap<>();

	private SystemParametersServer systemParametersServer;
	private NodeService nodeService;
	private UserModel userInfo;

	public NodeUpdateHandler() {
		super(null);
	}

	private void init(Map<String, Object> attributes) {
		systemParametersServer = SpringUtil.getBean(SystemParametersServer.class);
		nodeService = SpringUtil.getBean(NodeService.class);
		userInfo = (UserModel) attributes.get("userInfo");
	}

	@Override
	protected boolean showHelloMsg() {
		return false;
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{};
	}

	private void pullNodeList(WebSocketSession session, String ids) {
		List<String> split = StrUtil.split(ids, StrUtil.COMMA);
		List<NodeModel> nodeModelList = nodeService.listById(split);
		if (nodeModelList == null) {
			this.onError(session, "没有查询到节点信息：" + ids);
			return;
		}
		for (NodeModel model : nodeModelList) {
			if (clientMap.containsKey(model.getId())) {
				continue;
			}
			Map<String, Object> attributes = session.getAttributes();
			String url = NodeForward.getSocketUrl(model, NodeUrl.NodeUpdate, (UserModel) attributes.get("userInfo"));
			// 连接节点
			ThreadUtil.execute(() -> {
				try {
					NodeClient client = new NodeClient(url, model, session);
					clientMap.put(model.getId(), client);
				} catch (Exception e) {
					DefaultSystemLog.getLog().error("创建插件端连接失败", e);
				}
			});
		}
	}

	@Override
	public void destroy(WebSocketSession session) {
		for (String key : clientMap.keySet()) {
			NodeClient client = clientMap.get(key);
			if (client.isOpen()) {
				client.close();
			}
		}
		clientMap.clear();
		//
		super.destroy(session);
	}

	@Override
	protected void handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
		WebSocketMessageModel model = WebSocketMessageModel.getInstance(json.toString());
		this.init(attributes);
		String ids = null;
		String command = model.getCommand();
		switch (command) {
			case "getAgentVersion":
				model.setData(getAgentVersion());
				break;
			case "updateNode":
				super.logOpt(attributes, json);
				updateNode(model, session);
				break;
			default: {
				if (StrUtil.startWith(command, "getNodeList:")) {
					ids = StrUtil.removePrefix(command, "getNodeList:");
				}
			}
			break;
		}

		if (model.getData() != null) {
			this.sendMsg(model, session);
		}
		if (StrUtil.isNotEmpty(ids)) {
			pullNodeList(session, ids);
		}
	}

	private void onError(WebSocketSession session, String msg) {
		WebSocketMessageModel error = new WebSocketMessageModel("onError", "");
		error.setData(msg);
		this.sendMsg(error, session);
	}

	/**
	 * 更新节点
	 *
	 * @param model 参数
	 */
	private void updateNode(WebSocketMessageModel model, WebSocketSession session) {
		JSONObject params = (JSONObject) model.getParams();
		JSONArray ids = params.getJSONArray("ids");
		if (CollUtil.isEmpty(ids)) {
			return;
		}
		try {
			AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class);
			//
			if (agentFileModel == null || !FileUtil.exist(agentFileModel.getSavePath())) {
				this.onError(session, "Agent JAR包不存在");
				return;
			}
			for (int i = 0; i < ids.size(); i++) {
				int finalI = i;
				ThreadUtil.execute(() -> this.updateNodeItem(ids.getString(finalI), session, agentFileModel));
			}
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("升级失败", e);
		}
	}

	private void updateNodeItem(String id, WebSocketSession session, AgentFileModel agentFileModel) {
		try {
			NodeModel node = nodeService.getByKey(id);
			if (node == null) {
				this.onError(session, "没有对应的节点：" + id);
				return;
			}
			NodeClient client = clientMap.get(node.getId());
			if (client == null) {
				this.onError(session, "对应的插件端还没有被初始化：" + id);
				return;
			}
			if (client.isOpen()) {
				// 发送文件信息
				WebSocketMessageModel webSocketMessageModel = new WebSocketMessageModel("upload", id);
				webSocketMessageModel.setNodeId(id);
				webSocketMessageModel.setParams(agentFileModel);
				client.send(webSocketMessageModel.toString());
				//
				try (FileInputStream fis = new FileInputStream(agentFileModel.getSavePath())) {
					// 发送文件内容
					int len;
					byte[] buffer = new byte[1024 * 1024];
					while ((len = fis.read(buffer)) > 0) {
						client.send(ByteBuffer.wrap(buffer, 0, len));
					}
				}
				WebSocketMessageModel restartMessage = new WebSocketMessageModel("restart", id);
				client.send(restartMessage.toString());
				// 重启后尝试访问插件端，能够连接说明重启完毕
				ThreadUtil.execute(() -> {
					WebSocketMessageModel callbackRestartMessage = new WebSocketMessageModel("restart", id);
					int retryCount = 0;
					try {
						// 先等待一会，太快可能还没重启
						ThreadUtil.sleep(10000L);
						while (retryCount <= 30) {
							++retryCount;
							try {
								ThreadUtil.sleep(1000L);
								if (client.reconnectBlocking()) {
									this.sendMsg(callbackRestartMessage.setData("重启完成"), session);
									return;
								}
							} catch (Exception ignored) {
							}
						}
						this.sendMsg(callbackRestartMessage.setData("重连失败"), session);
					} catch (Exception e) {
						DefaultSystemLog.getLog().error("升级后重连插件端失败:" + id, e);
						this.sendMsg(callbackRestartMessage.setData("重连插件端失败"), session);
					}
				});
			} else {
				this.onError(session, "节点连接丢失");
			}
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("升级失败:" + id, e);
			this.onError(session, "节点升级失败：" + e.getMessage());
		}
	}

	private void sendMsg(WebSocketMessageModel model, WebSocketSession session) {
		try {
			synchronized (session.getId()) {
				session.sendMessage(new TextMessage(model.toString()));
			}
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("发送消息失败", e);
		}
	}

	/**
	 * 获取当前系统缓存的Agent
	 *
	 * @return json
	 */
	private String getAgentVersion() {
		AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class);
		if (agentFileModel == null) {
			return null;
		}
		return JSONObject.toJSONString(agentFileModel);
	}

//	/**
//	 * 获取节点列表
//	 *
//	 * @return 节点列表
//	 */
//	private List<NodeVersionModel> getNodeList() {
//		NodeOld1Service nodeService = SpringUtil.getBean(NodeOld1Service.class);
//		List<NodeModel> nodeModels = nodeService.list();
//		List<NodeVersionModel> result = new ArrayList<>();
//		for (NodeModel node : nodeModels) {
//			NodeVersionModel model = new NodeVersionModel();
//			model.setId(node.getId());
//			model.setName(node.getName());
////			model.setGroup(node.getGroup());
//			result.add(model);
//		}
//		return result;
//	}
}
