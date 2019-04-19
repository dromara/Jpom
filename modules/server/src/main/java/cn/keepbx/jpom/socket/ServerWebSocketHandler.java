package cn.keepbx.jpom.socket;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.data.UserOperateLogV1;
import cn.keepbx.jpom.system.init.OperateLogController;
import com.alibaba.fastjson.JSONObject;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

/**
 * 消息处理器
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
public class ServerWebSocketHandler extends TextWebSocketHandler {
    private OperateLogController operateLogController;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (operateLogController == null) {
            operateLogController = SpringUtil.getBean(OperateLogController.class);
        }
        Map<String, Object> attributes = session.getAttributes();
        NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
        String projectId = (String) attributes.get("projectId");
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        String url = NodeForward.getSocketUrl(nodeModel, NodeUrl.TopSocket);
        String userName = UserModel.getOptUserName(userInfo);
        userName = URLUtil.encode(userName);
        url = StrUtil.format(url, projectId, userName);
        // 连接节点
        ProxySession proxySession = new ProxySession(url, session);
        session.getAttributes().put("proxySession", proxySession);
        session.sendMessage(new TextMessage(StrUtil.format("欢迎加入:{} 会话id:{} ", userInfo.getName(), session.getId())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String msg = message.getPayload();
        Map<String, Object> attributes = session.getAttributes();
        ProxySession proxySession = (ProxySession) attributes.get("proxySession");
        JSONObject json = JSONObject.parseObject(msg);
        String op = json.getString("op");
        CommandOp commandOp = CommandOp.valueOf(op);
        UserOperateLogV1.OptType type = null;
        switch (commandOp) {
            case stop:
                type = UserOperateLogV1.OptType.Stop;
                break;
            case start:
                type = UserOperateLogV1.OptType.Start;
                break;
            case restart:
                type = UserOperateLogV1.OptType.Restart;
                break;
            default:
                break;
        }
        if (type != null) {
            // 记录操作日志
            UserModel userInfo = (UserModel) attributes.get("userInfo");
            String ip = (String) attributes.get("ip");
            NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
            //
            String projectId = (String) attributes.get("projectId");

            String reqId = IdUtil.fastUUID();
            json.put("reqId", reqId);

            try {
                operateLogController.log(reqId, userInfo, "还没有响应", ip, type, nodeModel, projectId);
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("记录操作日志异常", e);
            }
            proxySession.send(json.toString());
        } else {
            proxySession.send(msg);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", exception);
        destroy(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        destroy(session);
    }

    private void destroy(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
        }
        Map<String, Object> attributes = session.getAttributes();
        ProxySession proxySession = (ProxySession) attributes.get("proxySession");
        if (proxySession != null) {
            proxySession.close();
        }
    }
}
