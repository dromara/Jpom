package io.jpom.socket;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.ScriptModel;
import io.jpom.service.script.ScriptServer;
import io.jpom.util.SocketSessionUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * 脚本模板socket
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@ServerEndpoint(value = "/script_run/{id}/{optUser}")
@Component
public class AgentWebSocketScriptHandle extends BaseAgentWebSocketHandle {

    private ScriptServer scriptServer;

    @OnOpen
    public void onOpen(@PathParam("id") String id, Session session, @PathParam("optUser") String urlOptUser) {
        if (scriptServer == null) {
            scriptServer = SpringUtil.getBean(ScriptServer.class);
        }
        try {
            if (StrUtil.isEmpty(id)) {
                SocketSessionUtil.send(session, "脚本模板未知");
                return;
            }
            ScriptModel scriptModel = scriptServer.getItem(id);
            if (scriptModel == null) {
                SocketSessionUtil.send(session, "没有找到对应的脚本模板");
                return;
            }
            SocketSessionUtil.send(session, "连接成功：" + scriptModel.getName());
            this.addUser(session, urlOptUser);
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("socket 错误", e);
            try {
                SocketSessionUtil.send(session, JsonMessage.getString(500, "系统错误!"));
                session.close();
            } catch (IOException e1) {
                DefaultSystemLog.getLog().error(e1.getMessage(), e1);
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject json = JSONObject.parseObject(message);
        String scriptId = json.getString("scriptId");
        ScriptModel scriptModel = scriptServer.getItem(scriptId);
        if (scriptModel == null) {
            SocketSessionUtil.send(session, "没有对应脚本模板:" + scriptId);
            session.close();
            return;
        }
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        switch (consoleCommandOp) {
            case start:
                String args = json.getString("args");
                ScriptProcessBuilder.addWatcher(scriptModel, args, session);
                break;
            case stop:
                ScriptProcessBuilder.stopRun(scriptModel);
                break;
            case heart:
            default:
                return;
        }
        // 记录操作人
        scriptModel = scriptServer.getItem(scriptId);
        String name = getOptUserName(session);
        scriptModel.setLastRunUser(name);
        scriptServer.updateItem(scriptModel);
        json.put("code", 200);
        json.put("msg", "执行成功");
        DefaultSystemLog.getLog().info(json.toString());
        SocketSessionUtil.send(session, json.toString());
    }


    @Override
    @OnClose
    public void onClose(Session session) {
        super.onClose(session);
        ScriptProcessBuilder.stopWatcher(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
