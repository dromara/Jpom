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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.ConfigBean;
import io.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static javax.websocket.CloseReason.CloseCodes.CANNOT_ACCEPT;

/**
 * 插件端socket 基类
 *
 * @author jiangzeyin
 * @since 2019/4/24
 */
@Slf4j
public abstract class BaseAgentWebSocketHandle {

	private static final ConcurrentHashMap<String, String> USER = new ConcurrentHashMap<>();

	protected String getParameters(Session session, String name) {
		Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();
		Map<String, String> parameters = session.getPathParameters();
		log.debug("web socket parameters: {} {}", JSONObject.toJSONString(requestParameterMap), parameters);
		List<String> strings = requestParameterMap.get(name);
		String value = CollUtil.join(strings, StrUtil.COMMA);
		if (StrUtil.isEmpty(value)) {
			value = parameters.get(name);
		}
		return URLUtil.decode(value);
	}

	/**
	 * 判断授权信息是否正确
	 *
	 * @param session session
	 * @return true 需要结束回话
	 */
	public boolean checkAuthorize(Session session) {
		String authorize = this.getParameters(session, ConfigBean.JPOM_AGENT_AUTHORIZE);
		boolean ok = AgentAuthorize.getInstance().checkAuthorize(authorize);
		if (!ok) {
			try {
				session.close(new CloseReason(CANNOT_ACCEPT, "授权信息错误"));
			} catch (Exception e) {
				log.error("socket 错误", e);
			}
			return true;
		}
		this.addUser(session, this.getParameters(session, "optUser"));
		return false;
	}

	/**
	 * 添加用户监听的
	 *
	 * @param session session
	 * @param name    用户名
	 */
	private void addUser(Session session, String name) {
		String optUser = URLUtil.decode(name);
		if (optUser == null) {
			return;
		}
		USER.put(session.getId(), optUser);
	}

	public void onError(Session session, Throwable thr) {
		// java.io.IOException: Broken pipe
		try {
			SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
		} catch (IOException ignored) {
		}
		log.error(session.getId() + "socket 异常", thr);
	}

	protected String getOptUserName(Session session) {
		String name = USER.get(session.getId());
		return StrUtil.emptyToDefault(name, StrUtil.DASHED);
	}

	public void onClose(Session session) {
		// 清理日志监听
		try {
			AgentFileTailWatcher.offline(session);
		} catch (Exception e) {
			log.error("关闭异常", e);
		}
		// top
		//        TopManager.removeMonitor(session);
		USER.remove(session.getId());
	}
}
