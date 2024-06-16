/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.transport.*;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

/**
 * 服务端socket 基本类
 *
 * @author bwcx_jzy
 * @since 2019/4/25
 */
@Slf4j
public abstract class BaseProxyHandler extends BaseHandler {

    private final NodeUrl nodeUrl;

    public BaseProxyHandler(NodeUrl nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    /**
     * 连接参数
     *
     * @param attributes 属性
     * @return key, value, key, value.....
     */
    protected abstract Object[] getParameters(Map<String, Object> attributes);

    @Override
    public void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
        super.afterConnectionEstablishedImpl(session);
        Map<String, Object> attributes = session.getAttributes();
        this.init(session, attributes);
    }

    /**
     * 连接成功 初始化
     *
     * @param session    会话
     * @param attributes 属性
     * @throws URISyntaxException 异常
     * @throws IOException        IO
     */
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        boolean init = (boolean) attributes.getOrDefault("init", false);
        if (init) {
            return;
        }
        NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
        MachineNodeModel machine = (MachineNodeModel) attributes.get("machine");

        Object[] parameters = this.getParameters(attributes);
        UserModel userModel = (UserModel) attributes.get("userInfo");
        parameters = ArrayUtil.append(parameters, "optUser", userModel.getId(), "lang", attributes.get("lang"));
        // 连接节点
        INodeInfo nodeInfo = Optional.ofNullable(machine)
            .map(NodeForward::coverNodeInfo)
            .orElseGet(() -> Optional.ofNullable(nodeModel)
                .map(NodeForward::parseNodeInfo)
                .orElse(null)
            );
        if (nodeInfo == null) {
            return;
        }
        String workspaceId = Optional.ofNullable(nodeModel).map(BaseWorkspaceModel::getWorkspaceId).orElse(StrUtil.EMPTY);
        IUrlItem urlItem = NodeForward.parseUrlItem(nodeInfo, workspaceId, this.nodeUrl, DataContentType.FORM_URLENCODED);

        IProxyWebSocket proxySession = TransportServerFactory.get().websocket(nodeInfo, urlItem, parameters);
        proxySession.onMessage(s -> onProxyMessage(session, s));
        if (!proxySession.connectBlocking()) {
            this.sendMsg(session, I18nMessageUtil.get("i18n.plugin_connection_failed.02a1"));
            this.destroy(session);
            return;
        }
        session.getAttributes().put("proxySession", proxySession);


        attributes.put("init", true);
    }

    protected void onProxyMessage(WebSocketSession session, String msg) {
        sendMsg(session, msg);
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String msg = message.getPayload();
        Map<String, Object> attributes = session.getAttributes();
        IProxyWebSocket proxySession = (IProxyWebSocket) attributes.get("proxySession");
        JSONObject json = JSONObject.parseObject(msg);
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = StrUtil.isNotEmpty(op) ? ConsoleCommandOp.valueOf(op) : null;
        try {
            String textMessage;
            if (proxySession != null) {
                textMessage = this.handleTextMessage(attributes, session, proxySession, json, consoleCommandOp);
            } else {
                textMessage = this.handleTextMessage(attributes, session, json, consoleCommandOp);
            }
            if (textMessage != null) {
                this.sendMsg(session, textMessage);
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.handle_message_exception.0bdc"), e);
            this.sendMsg(session, I18nMessageUtil.get("i18n.handle_message_exception_with_colon.56f0") + e.getMessage());
        }
    }

    /**
     * 消息处理方法
     *
     * @param attributes       属性
     * @param session          当前回话
     * @param json             数据
     * @param consoleCommandOp 操作类型
     */
    protected String handleTextMessage(Map<String, Object> attributes,
                                       WebSocketSession session,
                                       JSONObject json,
                                       ConsoleCommandOp consoleCommandOp) throws IOException {
        return null;
    }

    /**
     * 消息处理方法
     *
     * @param attributes       属性
     * @param proxySession     代理回话
     * @param json             数据
     * @param consoleCommandOp 操作类型
     */
    protected String handleTextMessage(Map<String, Object> attributes,
                                       WebSocketSession session,
                                       IProxyWebSocket proxySession,
                                       JSONObject json,
                                       ConsoleCommandOp consoleCommandOp) throws IOException {
        return this.handleTextMessage(attributes, proxySession, json, consoleCommandOp);
    }

    /**
     * 消息处理方法
     *
     * @param attributes       属性
     * @param proxySession     代理回话
     * @param json             数据
     * @param consoleCommandOp 操作类型
     */
    protected String handleTextMessage(Map<String, Object> attributes,
                                       IProxyWebSocket proxySession,
                                       JSONObject json,
                                       ConsoleCommandOp consoleCommandOp) throws IOException {
        return null;
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
        IProxyWebSocket proxySession = (IProxyWebSocket) attributes.get("proxySession");
        if (proxySession != null) {
            try {
                proxySession.close();
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.close_exception.5b86"), e);
            }
        }
        SocketSessionUtil.close(session);
    }
}
