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

import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ScriptExecuteLogModel;
import io.jpom.model.data.ScriptModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.node.script.ScriptExecuteLogServer;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ProxySession;

import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@Feature(cls = ClassFeature.NODE_SCRIPT)
public class ScriptHandler extends BaseProxyHandler {

	public ScriptHandler() {
		super(NodeUrl.Script_Run);
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{"id", attributes.get("scriptId")};
	}

	@Override
	protected void handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
		if (consoleCommandOp != ConsoleCommandOp.heart) {
			super.logOpt(attributes, json);
		}
		if (consoleCommandOp == ConsoleCommandOp.start) {
			// 开始执行
			String executeId = this.createLog(attributes);
			json.put("executeId", executeId);
		}
		proxySession.send(json.toString());
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
		ScriptModel dataItem = (ScriptModel) attributes.get("dataItem");
		ScriptExecuteLogServer logServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
		//
		try {
			BaseServerController.resetInfo(userModel);
			ScriptExecuteLogModel scriptExecuteLogModel = new ScriptExecuteLogModel();
			scriptExecuteLogModel.setScriptId(dataItem.getScriptId());
			scriptExecuteLogModel.setNodeId(nodeInfo.getId());
			scriptExecuteLogModel.setWorkspaceId(nodeInfo.getWorkspaceId());
			logServer.insert(scriptExecuteLogModel);
			return scriptExecuteLogModel.getId();
		} finally {
			BaseServerController.remove();
		}
	}
}
