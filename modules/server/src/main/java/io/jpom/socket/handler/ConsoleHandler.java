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

import cn.hutool.core.util.IdUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.UserModel;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ProxySession;
import io.jpom.system.init.OperateLogController;

import java.util.Map;

/**
 * 控制台消息处理器
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class ConsoleHandler extends BaseProxyHandler {

	public ConsoleHandler() {
		super(NodeUrl.TopSocket);
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{"projectId", attributes.get("projectId"), "copyId", attributes.get("copyId")};
	}

	@Override
	protected void handleTextMessage(Map<String, Object> attributes, ProxySession proxySession, JSONObject json, ConsoleCommandOp consoleCommandOp) {
//		UserOperateLogV1.OptType type = null;
		switch (consoleCommandOp) {
			case stop:
//				type = UserOperateLogV1.OptType.Stop;
				break;
			case start:
//				type = UserOperateLogV1.OptType.Start;
				break;
			case restart:
//				type = UserOperateLogV1.OptType.Restart;
				break;
			default:
				break;
		}
//		if (type != null) {
		// 记录操作日志
		UserModel userInfo = (UserModel) attributes.get("userInfo");
		String reqId = IdUtil.fastUUID();
		json.put("reqId", reqId);
		//
		String projectId = (String) attributes.get("projectId");
		OperateLogController.CacheInfo cacheInfo = cacheInfo(attributes, json, projectId);
		try {
			operateLogController.log(reqId, userInfo, "还没有响应", cacheInfo);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("记录操作日志异常", e);
		}
//		}
		proxySession.send(json.toString());
	}
}
