package io.jpom.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.system.init.OperateLogController;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

/**
 * 服务端socket 基本类
 *
 * @author jiangzeyin
 * @date 2019/4/25
 */
public abstract class BaseProxyHandler extends BaseHandler {
    protected OperateLogController operateLogController;

    private NodeUrl nodeUrl;
    private String dataParName;

    public BaseProxyHandler(NodeUrl nodeUrl, String dataParName) {
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
        if (nodeModel != null) {
            String url = NodeForward.getSocketUrl(nodeModel, nodeUrl);
            url = StrUtil.format(url, dataValue, userName);
            // 连接节点
            ProxySession proxySession = new ProxySession(url, session);
            session.getAttributes().put("proxySession", proxySession);
        }
        session.sendMessage(new TextMessage(StrUtil.format("欢迎加入:{} 会话id:{} ", userInfo.getName(), session.getId())));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (operateLogController == null) {
            operateLogController = SpringUtil.getBean(OperateLogController.class);
        }
        String msg = message.getPayload();
        Map<String, Object> attributes = session.getAttributes();
        ProxySession proxySession = (ProxySession) attributes.get("proxySession");
        JSONObject json = JSONObject.parseObject(msg);
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        if (proxySession != null) {
            this.handleTextMessage(attributes, proxySession, json, consoleCommandOp);
        } else {
            this.handleTextMessage(attributes, session, json, consoleCommandOp);
        }
    }

    protected void handleTextMessage(Map<String, Object> attributes,
                                     WebSocketSession session,
                                     JSONObject json,
                                     ConsoleCommandOp consoleCommandOp) throws IOException {

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
    public void destroy(WebSocketSession session) {
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
