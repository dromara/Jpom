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

import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeUrl;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ProxySession;

import java.util.Map;

/**
 * 控制台消息处理器
 *
 * @author jiangzeyin
 * @since 2019/4/19
 */
@Feature(cls = ClassFeature.PROJECT_CONSOLE, method = MethodFeature.EXECUTE)
public class ConsoleHandler extends BaseProxyHandler {

	public ConsoleHandler() {
		super(NodeUrl.TopSocket);
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{"projectId", attributes.get("projectId"), "copyId", attributes.get("copyId")};
	}

	@Override
	protected String handleTextMessage(Map<String, Object> attributes,
									   ProxySession proxySession,
									   JSONObject json,
									   ConsoleCommandOp consoleCommandOp) {
		//ProjectInfoCacheModel dataItem = (ProjectInfoCacheModel) attributes.get("dataItem");
//		UserModel userModel = (UserModel) attributes.get("userInfo");
//		if (RunMode.Dsl.name().equals(dataItem.getRunMode()) && userModel.isDemoUser()) {
//			if (consoleCommandOp == ConsoleCommandOp.stop || consoleCommandOp == ConsoleCommandOp.start || consoleCommandOp == ConsoleCommandOp.restart) {
//				return PermissionInterceptor.DEMO_TIP;
//			}
//		}
		if (consoleCommandOp != ConsoleCommandOp.heart) {
			super.logOpt(this.getClass(), attributes, json);

		}
		proxySession.send(json.toString());
		return null;
	}
}
