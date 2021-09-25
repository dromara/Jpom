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

import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ProxySession;
import io.jpom.system.init.OperateLogController;

import java.util.Map;

/**
 * 脚本模板消息控制器
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
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
		UserOperateLogV1.OptType type = null;
		switch (consoleCommandOp) {
			case stop:
				type = UserOperateLogV1.OptType.Script_Stop;
				break;
			case start:
				type = UserOperateLogV1.OptType.Script_Start;
				break;
			default:
				break;
		}
		if (type != null) {
			// 记录操作日志
			UserModel userInfo = (UserModel) attributes.get("userInfo");
			//
			String scriptId = (String) attributes.get("scriptId");
			OperateLogController.CacheInfo cacheInfo = cacheInfo(attributes, json, type, scriptId);
			try {
				operateLogController.log(userInfo, "脚本模板执行...", cacheInfo);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("记录操作日志异常", e);
			}
		}
		proxySession.send(json.toString());
	}
}
