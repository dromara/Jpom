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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentAuthorize;
import org.dromara.jpom.util.SocketSessionUtil;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件端socket 基类
 *
 * @author bwcx_jzy
 * @since 2019/4/24
 */
@Slf4j
public abstract class BaseAgentWebSocketHandle {

    private static final ConcurrentHashMap<String, String> USER = new SafeConcurrentHashMap<>();
    protected static AgentAuthorize agentAuthorize;

    /**
     * 设置授权对象
     *
     * @param agentAuthorize 授权
     */
    protected static void setAgentAuthorize(AgentAuthorize agentAuthorize) {
        BaseAgentWebSocketHandle.agentAuthorize = agentAuthorize;
    }

    protected void setLanguage(Session session) {
        Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();
        List<String> lang = requestParameterMap.get("lang");
        I18nMessageUtil.setLanguage(CollUtil.getFirst(lang));
    }

    protected void clearLanguage() {
        I18nMessageUtil.clearLanguage();
    }

    protected String getParameters(Session session, String name) {
        Map<String, List<String>> requestParameterMap = session.getRequestParameterMap();
        Map<String, String> parameters = session.getPathParameters();
        if (log.isDebugEnabled()) {
            log.debug("web socket parameters: {} {}", JSONObject.toJSONString(requestParameterMap), parameters);
        }
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
        String authorize = this.getParameters(session, Const.JPOM_AGENT_AUTHORIZE);
        boolean ok = agentAuthorize.checkAuthorize(authorize);
        if (!ok) {
            log.warn(I18nMessageUtil.get("i18n.socket_session_establishment_failed.4924"));
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, I18nMessageUtil.get("i18n.auth_info_error.c184")));
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.socket_error.18c1"), e);
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
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.server_exception_occurred.9eb4") + ExceptionUtil.stacktraceToString(thr));
        } catch (IOException ignored) {
        }
        log.error("{}{}", session.getId(), I18nMessageUtil.get("i18n.socket_exception.d836"), thr);
    }

    protected String getOptUserName(Session session) {
        String name = USER.get(session.getId());
        return StrUtil.emptyToDefault(name, StrUtil.DASHED);
    }

    public void onClose(Session session, CloseReason closeReason) {
        log.debug(I18nMessageUtil.get("i18n.session_closed_reason.103a"), session.getId(), closeReason);
        // 清理日志监听
        try {
            AgentFileTailWatcher.offline(session);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.close_exception.5b86"), e);
        }
        // top
        //        TopManager.removeMonitor(session);
        USER.remove(session.getId());
    }
}
