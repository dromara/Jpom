package io.jpom.socket.spring.handler;

import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.JpomManifest;
import io.jpom.model.AgentFileModel;
import io.jpom.model.WebSocketMessageModel;
import io.jpom.model.data.UploadFileModel;
import io.jpom.system.AgentConfigBean;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * 节点升级websocket处理器
 *
 * @author lf
 */
public class NodeUpdateHandler extends AbstractWebSocketHandler {
    private static final Map<String, UploadFileModel> UPLOAD_FILE_INFO = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 设置二进制消息的最大长度为1M
        session.setBinaryMessageSizeLimit(1024 * 1024);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
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

        session.sendMessage(new TextMessage(model.toString()));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        UploadFileModel uploadFileModel = UPLOAD_FILE_INFO.get(session.getId());
        uploadFileModel.save(message.getPayload().array());
        // 更新进度
        WebSocketMessageModel model = new WebSocketMessageModel("updateNode", uploadFileModel.getId());
        model.setData(uploadFileModel);
        session.sendMessage(new TextMessage(model.toString()));
    }

    /**
     * 重启
     *
     * @param session
     * @return
     */
    public String restart(WebSocketSession session) {
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
}
