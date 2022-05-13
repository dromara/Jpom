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

import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.node.ScriptCacheModel;
import io.jpom.model.node.ScriptExecuteLogCacheModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.node.script.NodeScriptExecuteLogServer;
import io.jpom.service.node.script.NodeScriptServer;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ProxySession;

import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @since 2019/4/24
 */
@Feature(cls = ClassFeature.NODE_SCRIPT, method = MethodFeature.EXECUTE)
public class NodeScriptHandler extends BaseProxyHandler {

	public NodeScriptHandler() {
		super(NodeUrl.Script_Run);
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{"id", attributes.get("scriptId")};
	}

	@Override
	protected String handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
		if (consoleCommandOp != ConsoleCommandOp.heart) {
			super.logOpt(this.getClass(), attributes, json);
		}
		if (consoleCommandOp == ConsoleCommandOp.start) {
			//			UserModel userModel = (UserModel) attributes.get("userInfo");
			//			if (userModel.isDemoUser()) {
			//				return PermissionInterceptor.DEMO_TIP;
			//			}
			// 开始执行
			String executeId = this.createLog(attributes);
			json.put("executeId", executeId);
		}
		proxySession.send(json.toString());
		return null;
	}

	/**
	 * 创建执行日志
	 *
	 * @param attributes 参数属性
	 * @return 执行ID
	 */
	private String createLog(Map<String, Object> attributes) {
		NodeModel nodeInfo = (NodeModel) attributes.get("nodeInfo");
		UserModel userModel = (UserModel) attributes.get("userInfo");
		ScriptCacheModel dataItem = (ScriptCacheModel) attributes.get("dataItem");
		NodeScriptExecuteLogServer logServer = SpringUtil.getBean(NodeScriptExecuteLogServer.class);
		NodeScriptServer nodeScriptServer = SpringUtil.getBean(NodeScriptServer.class);
		//
		try {
			BaseServerController.resetInfo(userModel);
			//
			ScriptCacheModel scriptCacheModel = new ScriptCacheModel();
			scriptCacheModel.setId(dataItem.getId());
			scriptCacheModel.setLastRunUser(userModel.getId());
			nodeScriptServer.update(scriptCacheModel);
			//
			ScriptExecuteLogCacheModel scriptExecuteLogCacheModel = new ScriptExecuteLogCacheModel();
			scriptExecuteLogCacheModel.setScriptId(dataItem.getScriptId());
			scriptExecuteLogCacheModel.setNodeId(nodeInfo.getId());
			scriptExecuteLogCacheModel.setScriptName(dataItem.getName());
			scriptExecuteLogCacheModel.setTriggerExecType(0);
			scriptExecuteLogCacheModel.setWorkspaceId(nodeInfo.getWorkspaceId());
			logServer.insert(scriptExecuteLogCacheModel);
			return scriptExecuteLogCacheModel.getId();
		} finally {
			BaseServerController.removeAll();
		}
	}
}
