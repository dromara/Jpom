package io.jpom.socket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.ConfigBean;
import io.jpom.util.SocketSessionUtil;

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
 * @date 2019/4/24
 */
public abstract class BaseAgentWebSocketHandle {

	private static final ConcurrentHashMap<String, String> USER = new ConcurrentHashMap<>();

	protected String getParameters(Session session, String name) {
		Map<String, List<String>> pathParameters = session.getRequestParameterMap();
		List<String> strings = pathParameters.get(name);
		return CollUtil.join(strings, StrUtil.COMMA);
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
				DefaultSystemLog.getLog().error("socket 错误", e);
			}
			return false;
		}
		this.addUser(session, this.getParameters(session, "optUser"));
		return true;
	}

	/**
	 * 添加用户监听的
	 *
	 * @param session session
	 * @param name    用户名
	 */
	private void addUser(Session session, String name) {
		String optUser = URLUtil.decode(name);
		USER.put(session.getId(), optUser);
	}

	public void onError(Session session, Throwable thr) {
		// java.io.IOException: Broken pipe
		try {
			SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
		} catch (IOException ignored) {
		}
		DefaultSystemLog.getLog().error(session.getId() + "socket 异常", thr);
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
			DefaultSystemLog.getLog().error("关闭异常", e);
		}
		// top
		//        TopManager.removeMonitor(session);
		USER.remove(session.getId());
	}
}
