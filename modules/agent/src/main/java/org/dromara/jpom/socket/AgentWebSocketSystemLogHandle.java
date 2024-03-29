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
            if (super.checkAuthorize(session)) {
                return;
            }
            SocketSessionUtil.send(session, "连接成功：插件端日志");
        } catch (Exception e) {
            log.error("socket 错误", e);
            try {
                SocketSessionUtil.send(session, JsonMessage.getString(500, "系统错误!"));
                session.close();
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject json = JSONObject.parseObject(message);
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return;
        }
        runMsg(session, json);
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
                log.error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        } catch (Exception e) {
            log.error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
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
