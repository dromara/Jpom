package io.jpom.socket.handler;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.data.UserModel;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.script.ScriptExecuteLogServer;
import io.jpom.service.script.ScriptServer;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.ScriptProcessBuilder;
import io.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Feature(cls = ClassFeature.SCRIPT)
public class ServerScriptHandler extends BaseProxyHandler {


	private ScriptExecuteLogServer logServer;
	private ScriptServer nodeScriptServer;

	@Override
	protected void init(WebSocketSession session, Map<String, Object> attributes) throws URISyntaxException, IOException {
		super.init(session, attributes);
		//
		this.logServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
		this.nodeScriptServer = SpringUtil.getBean(ScriptServer.class);
		ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
		this.sendMsg(session, "连接成功：" + scriptModel.getName());
	}

	public ServerScriptHandler() {
		super(null);
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[0];
	}

	@Override
	protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
		ScriptModel scriptModel = (ScriptModel) attributes.get("dataItem");
		if (consoleCommandOp != ConsoleCommandOp.heart) {
			super.logOpt(attributes, json);
			switch (consoleCommandOp) {
				case start: {
					UserModel userModel = (UserModel) attributes.get("userInfo");
					if (userModel.isDemoUser()) {
						return PermissionInterceptor.DEMO_TIP;
					}
					String args = json.getString("args");
					String executeId = this.createLog(attributes, scriptModel);
					json.put("executeId", executeId);
					ScriptProcessBuilder.addWatcher(scriptModel, executeId, args, session);
					this.sendMsg(session, json.toString());
					break;
				}
				case stop: {
					String executeId = json.getString("executeId");
					if (StrUtil.isEmpty(executeId)) {
						SocketSessionUtil.send(session, "没有执行ID");
						session.close();
						return null;
					}
					ScriptProcessBuilder.stopRun(executeId);
					break;
				}
				default:
					return null;
			}
		}
		return null;
	}

	/**
	 * 创建执行日志
	 *
	 * @param attributes 参数属性
	 * @return 执行ID
	 */
	private String createLog(Map<String, Object> attributes, ScriptModel scriptModel) {
		UserModel userModel = (UserModel) attributes.get("userInfo");

		//
		try {
			BaseServerController.resetInfo(userModel);
			//
			ScriptModel scriptCacheModel = new ScriptModel();
			scriptCacheModel.setId(scriptModel.getId());
			scriptCacheModel.setLastRunUser(userModel.getId());
			nodeScriptServer.update(scriptCacheModel);
			//
			ScriptExecuteLogModel scriptExecuteLogCacheModel = new ScriptExecuteLogModel();
			scriptExecuteLogCacheModel.setScriptId(scriptModel.getId());

			scriptExecuteLogCacheModel.setScriptName(scriptModel.getName());
			scriptExecuteLogCacheModel.setTriggerExecType(0);
			scriptExecuteLogCacheModel.setWorkspaceId(scriptModel.getWorkspaceId());
			logServer.insert(scriptExecuteLogCacheModel);
			return scriptExecuteLogCacheModel.getId();
		} finally {
			BaseServerController.removeAll();
		}
	}


	@Override
	public void destroy(WebSocketSession session) {
		//
		super.destroy(session);
		ScriptProcessBuilder.stopWatcher(session);
	}
}
