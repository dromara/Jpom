/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.socket;

import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.script.NodeScriptProcessBuilder;
import org.dromara.jpom.service.script.NodeScriptServer;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * 脚本模板socket
 *
 * @author bwcx_jzy
 * @since 2019/4/24
 */
@ServerEndpoint(value = "/script_run")
@Component
@Slf4j
public class AgentWebSocketScriptHandle extends BaseAgentWebSocketHandle {

    private static NodeScriptServer nodeScriptServer;

    @Autowired
    public void init(NodeScriptServer nodeScriptServer) {
        AgentWebSocketScriptHandle.nodeScriptServer = nodeScriptServer;
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            if (super.checkAuthorize(session)) {
                return;
            }
            String id = this.getParameters(session, "id");
            String workspaceId = this.getParameters(session, "workspaceId");
            if (StrUtil.hasEmpty(id, workspaceId)) {
                SocketSessionUtil.send(session, "脚本模板或者工作空间未知");
                return;
            }

            NodeScriptModel nodeScriptModel = nodeScriptServer.getItem(id);
            if (nodeScriptModel == null) {
                SocketSessionUtil.send(session, "没有找到对应的脚本模板");
                return;
            }
            SocketSessionUtil.send(session, "连接成功：" + nodeScriptModel.getName());
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
        String scriptId = json.getString("scriptId");
        NodeScriptModel nodeScriptModel = nodeScriptServer.getItem(scriptId);
        if (nodeScriptModel == null) {
            SocketSessionUtil.send(session, "没有对应脚本模板:" + scriptId);
            session.close();
            return;
        }
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        switch (consoleCommandOp) {
            case start: {
                String args = json.getString("args");
                String executeId = json.getString("executeId");
                if (StrUtil.isEmpty(executeId)) {
                    SocketSessionUtil.send(session, "没有执行ID");
                    session.close();
                    return;
                }
                NodeScriptProcessBuilder.addWatcher(nodeScriptModel, executeId, args, session);
                break;
            }
            case stop: {
                String executeId = json.getString("executeId");
                if (StrUtil.isEmpty(executeId)) {
                    SocketSessionUtil.send(session, "没有执行ID");
                    session.close();
                    return;
                }
                NodeScriptProcessBuilder.stopRun(executeId);
                break;
            }
            case heart:
            default:
                return;
        }
        // 记录操作人
        nodeScriptModel = nodeScriptServer.getItem(scriptId);
        String name = getOptUserName(session);
        nodeScriptModel.setLastRunUser(name);
        nodeScriptServer.updateItem(nodeScriptModel);
        json.put("code", 200);
        json.put("msg", "执行成功");
        log.debug(json.toString());
        SocketSessionUtil.send(session, json.toString());
    }


    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        NodeScriptProcessBuilder.stopWatcher(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
