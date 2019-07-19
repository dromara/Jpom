package cn.keepbx.jpom.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.forward.NodeForward;
import cn.keepbx.jpom.common.forward.NodeUrl;
import cn.keepbx.jpom.model.data.NodeModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.model.log.UserOperateLogV1;
import cn.keepbx.jpom.system.init.OperateLogController;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端socket 基本类
 *
 * @author jiangzeyin
 * @date 2019/4/25
 */
public abstract class BaseServerWebSocketHandler extends TextWebSocketHandler {
    protected OperateLogController operateLogController;

    private NodeUrl nodeUrl;
    private String dataParName;

    public BaseServerWebSocketHandler(NodeUrl nodeUrl, String dataParName) {
        this.nodeUrl = nodeUrl;
        this.dataParName = dataParName;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        String dataValue = (String) attributes.get(dataParName);
        String userName = UserModel.getOptUserName(userInfo);
        userName = URLUtil.encode(userName);
        String url = NodeForward.getSocketUrl(nodeModel, nodeUrl);
        url = StrUtil.format(url, dataValue, userName);
        // 连接节点
        ProxySession proxySession = new ProxySession(url, session);
        session.getAttributes().put("proxySession", proxySession);
        session.sendMessage(new TextMessage(StrUtil.format("欢迎加入:{} 会话id:{} ", userInfo.getName(), session.getId())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if (operateLogController == null) {
            operateLogController = SpringUtil.getBean(OperateLogController.class);
        }
        String msg = message.getPayload();
        Map<String, Object> attributes = session.getAttributes();
        ProxySession proxySession = (ProxySession) attributes.get("proxySession");
        JSONObject json = JSONObject.parseObject(msg);
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        this.handleTextMessage(attributes, proxySession, json, consoleCommandOp);
    }

    /**
     * 消息处理方法
     *
     * @param attributes       属性
     * @param proxySession     代理回话
     * @param json             数据
     * @param consoleCommandOp 操作类型
     */
    protected abstract void handleTextMessage(Map<String, Object> attributes,
                                              ProxySession proxySession,
                                              JSONObject json,
                                              ConsoleCommandOp consoleCommandOp);

    protected OperateLogController.CacheInfo cacheInfo(Map<String, Object> attributes, JSONObject json, UserOperateLogV1.OptType optType, String dataId) {
        String ip = (String) attributes.get("ip");
        NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
        OperateLogController.CacheInfo cacheInfo = new OperateLogController.CacheInfo();
        cacheInfo.setIp(ip);
        cacheInfo.setOptType(optType);
        cacheInfo.setNodeModel(nodeModel);
        cacheInfo.setDataId(dataId);
        String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
        cacheInfo.setUserAgent(userAgent);

        cacheInfo.setReqData(json.toString());
        return cacheInfo;
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
