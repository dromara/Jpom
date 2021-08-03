package io.jpom.socket;

import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.model.AgentFileModel;
import io.jpom.model.WebSocketMessageModel;
import io.jpom.model.data.UploadFileModel;
import io.jpom.system.AgentConfigBean;
import io.jpom.util.SocketSessionUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.util.HashMap;
import java.util.Map;

/**
 * 在线升级
 *
 * @author bwcx_jzy
 * @date 2021/8/3
 */
@ServerEndpoint(value = "/node_update")
@Component
public class AgentWebSocketUpdateHandle extends BaseAgentWebSocketHandle {

	private static final Map<String, UploadFileModel> UPLOAD_FILE_INFO = new HashMap<>();

	@OnOpen
	public void onOpen(Session session) {
		if (super.checkAuthorize(session)) {
			return;
		}
		session.setMaxBinaryMessageBufferSize(1024 * 1024);
		//
	}


	@OnMessage
	public void onMessage(String message, Session session) throws Exception {
		WebSocketMessageModel model = WebSocketMessageModel.getInstance(message);
		switch (model.getCommand()) {
			case "getVersion":
				model.setData(JSONObject.toJSONString(JpomManifest.getInstance()));
				break;
			case "upload":
				AgentFileModel agentFileModel = ((JSONObject) model.getParams()).toJavaObject(AgentFileModel.class);
				UploadFileModel uploadFileModel = new UploadFileModel();
				uploadFileModel.setId(model.getNodeId());
				uploadFileModel.setName(agentFileModel.getName());
				uploadFileModel.setSize(agentFileModel.getSize());
				uploadFileModel.setVersion(agentFileModel.getVersion());
				uploadFileModel.setSavePath(AgentConfigBean.getInstance().getTempPath().getAbsolutePath());
				uploadFileModel.remove();
				UPLOAD_FILE_INFO.put(session.getId(), uploadFileModel);
				break;
			case "restart":
				model.setData(restart(session));
				break;
			default:
				break;
		}
		SocketSessionUtil.send(session, model.toString());
		//session.sendMessage(new TextMessage(model.toString()));
	}

	@OnMessage
	public void onMessage(byte[] message, Session session) throws Exception {
		UploadFileModel uploadFileModel = UPLOAD_FILE_INFO.get(session.getId());
		uploadFileModel.save(message);
		// 更新进度
		WebSocketMessageModel model = new WebSocketMessageModel("updateNode", uploadFileModel.getId());
		model.setData(uploadFileModel);
		SocketSessionUtil.send(session, model.toString());
//		session.sendMessage(new TextMessage(model.toString()));
	}

	/**
	 * 重启
	 *
	 * @param session 回话
	 * @return 结果
	 */
	public String restart(Session session) {
		String result = "重启中";
		try {
			UploadFileModel uploadFile = UPLOAD_FILE_INFO.get(session.getId());
			JpomManifest.releaseJar(uploadFile.getFilePath(), uploadFile.getVersion(), true);
			JpomApplication.restart();
		} catch (RuntimeException e) {
			result = e.getMessage();
			DefaultSystemLog.getLog().error("重启失败", e);
		}
		return result;
	}

	@Override
	@OnClose
	public void onClose(Session session) {
		super.onClose(session);
		UPLOAD_FILE_INFO.remove(session.getId());
	}

	@OnError
	@Override
	public void onError(Session session, Throwable thr) {
		super.onError(session, thr);
	}
}
