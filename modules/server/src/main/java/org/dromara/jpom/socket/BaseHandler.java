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

import cn.hutool.core.bean.BeanPath;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.system.init.OperateLogController;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2019/8/9
 */
@Slf4j
public abstract class BaseHandler extends TextWebSocketHandler {

    protected void setLanguage(WebSocketSession session) {
        if (session == null) {
            return;
        }
        Map<String, Object> attributes = session.getAttributes();
        String lang = (String) attributes.get("lang");
        I18nMessageUtil.setLanguage(lang);
    }

    protected void clearLanguage() {
        I18nMessageUtil.clearLanguage();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        setLanguage(session);
        try {
            Map<String, Object> attributes = session.getAttributes();
            //
            this.showHelloMsg(attributes, session);
            //
            String permissionMsg = (String) attributes.get("permissionMsg");
            if (StrUtil.isNotEmpty(permissionMsg)) {
                this.sendMsg(session, permissionMsg);
                ThreadUtil.sleep(2, TimeUnit.SECONDS);
                this.destroy(session);
                return;
            }
            this.afterConnectionEstablishedImpl(session);
        } finally {
            clearLanguage();
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            setLanguage(session);
            super.handleMessage(session, message);
        } finally {
            clearLanguage();
        }
    }

    protected void showHelloMsg(Map<String, Object> attributes, WebSocketSession session) {
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        if (userInfo != null) {
            String payload = StrUtil.format(I18nMessageUtil.get("i18n.welcome_join_session.1c16"), userInfo.getName(), session.getId() + StrUtil.CRLF);
            this.sendMsg(session, payload);
        }
    }

    /**
     * 建立会话后
     *
     * @param session 会话
     * @throws Exception 异常
     */
    protected void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        // 连接成功后记录
        this.logOpt(this.getClass(), attributes, attributes);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("{}{}", session.getId(), I18nMessageUtil.get("i18n.socket_exception.d836"), exception);
        destroy(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        destroy(session);
        log.debug(I18nMessageUtil.get("i18n.session_closed_reason.103a"), session.getId(), status);
    }

    /**
     * 关闭连接
     *
     * @param session session
     */
    public abstract void destroy(WebSocketSession session);

    protected void sendMsg(WebSocketSession session, String msg) {
        try {
            SocketSessionUtil.send(session, msg);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
        }
    }

    /**
     * 操作 websocket 日志
     *
     * @param cls        class
     * @param attributes 属性
     * @param reqData    请求数据
     */
    protected void logOpt(Class<?> cls, Map<String, Object> attributes, Object reqData) {
        String ip = (String) attributes.get("ip");
        NodeModel nodeModel = (NodeModel) attributes.get("nodeInfo");
        // 记录操作日志
        UserModel userInfo = (UserModel) attributes.get("userInfo");
        String workspaceId = (String) attributes.get("workspaceId");
        OperateLogController.CacheInfo cacheInfo = new OperateLogController.CacheInfo();
        cacheInfo.setIp(ip);
        Feature feature = cls.getAnnotation(Feature.class);
        MethodFeature method = feature.method();
//		Assert.state(feature != null && feature, "权限功能没有配置正确");
        cacheInfo.setClassFeature(feature.cls());
        cacheInfo.setWorkspaceId(workspaceId);
        cacheInfo.setMethodFeature(method);

        cacheInfo.setNodeModel(nodeModel);
        //
        Object dataItem = attributes.get("dataItem");
        Optional.ofNullable(dataItem).map(o -> {
            if (o instanceof BaseNodeModel) {
                BaseNodeModel baseNodeModel = (BaseNodeModel) o;
                return baseNodeModel.dataId();
            }
            Object id = BeanPath.create("id").get(o);
            return StrUtil.toStringOrNull(id);

        }).ifPresent(cacheInfo::setDataId);
        String userAgent = (String) attributes.get(HttpHeaders.USER_AGENT);
        cacheInfo.setUserAgent(userAgent);
        cacheInfo.setReqData(JSONObject.toJSONString(reqData));

        //cacheInfo.setMethodFeature(execute);
        Object proxySession = attributes.get("proxySession");
        try {
            attributes.remove("proxySession");
            attributes.put("use_type", "WebSocket");
            attributes.put("class_type", cls.getName());
            OperateLogController operateLogController = SpringUtil.getBean(OperateLogController.class);
            operateLogController.log(userInfo, JSONObject.toJSONString(attributes), cacheInfo);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.record_operation_log_exception.8012"), e);
        } finally {
            if (proxySession != null) {
                attributes.put("proxySession", proxySession);
            }
        }
    }

}
