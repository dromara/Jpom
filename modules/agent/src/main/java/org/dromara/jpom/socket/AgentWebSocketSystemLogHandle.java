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

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.configuration.SystemConfig;
import org.dromara.jpom.system.LogbackConfig;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * 系统日志
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@ServerEndpoint(value = "/system_log")
@Component
@Slf4j
public class AgentWebSocketSystemLogHandle extends BaseAgentWebSocketHandle {

    private static SystemConfig systemConfig;

    private static final Map<String, File> CACHE_FILE = new SafeConcurrentHashMap<>();

    @Autowired
    public void init(AgentConfig agentConfig) {
        AgentWebSocketSystemLogHandle.systemConfig = agentConfig.getSystem();
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.plugin_end_log_connection_successful.9035"));
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.socket_error.18c1"), e);
            try {
                SocketSessionUtil.send(session, JsonMessage.getString(500, I18nMessageUtil.get("i18n.system_error.9417")));
                session.close();
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        } finally {
            clearLanguage();
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
            JSONObject json = JSONObject.parseObject(message);
            String op = json.getString("op");
            ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
            if (consoleCommandOp == ConsoleCommandOp.heart) {
                return;
            }
            runMsg(session, json);
        } finally {
            clearLanguage();
        }
    }

    private void runMsg(Session session, JSONObject reqJson) throws Exception {
        try {
            String fileName = reqJson.getString("fileName");
            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(LogbackConfig.getPath(), fileName);
            File file1 = CACHE_FILE.get(session.getId());
            if (file1 != null && !file1.equals(file)) {
                // 离线上一个日志
                AgentFileTailWatcher.offlineFile(file, session);
            }
            try {
                AgentFileTailWatcher.addWatcher(file, systemConfig.getLogCharset(), session);
                CACHE_FILE.put(session.getId(), file);
            } catch (Exception io) {
                log.error(I18nMessageUtil.get("i18n.listen_log_changes.9081"), io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.command_execution_failed.90ef"), e);
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.command_execution_failed_details.77ed"));
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
        }
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
