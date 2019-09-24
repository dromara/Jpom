package io.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.model.data.TomcatInfoModel;
import io.jpom.service.manage.TomcatEditService;
import io.jpom.system.WebAopLog;
import io.jpom.util.SocketSessionUtil;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件端,控制台socket
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@ServerEndpoint(value = "/tomcat_log/{tomcatId}/{optUser}")
@Component
public class AgentWebSocketTomcatHandle extends BaseAgentWebSocketHandle {

    private TomcatEditService tomcatEditService;

    private static final Map<String, File> CACHE_FILE = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(@PathParam("tomcatId") String tomcatId, @PathParam("optUser") String urlOptUser, Session session) {
        try {
            if (tomcatEditService == null) {
                tomcatEditService = SpringUtil.getBean(TomcatEditService.class);
            }
            TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(tomcatId);
            if (tomcatInfoModel == null && !JpomApplication.SYSTEM_ID.equalsIgnoreCase(tomcatId)) {
                SocketSessionUtil.send(session, "获取tomcat信息错误");
                session.close();
                return;
            }
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
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return;
        }
        String tomcatId = json.getString("tomcatId");
        if (JpomApplication.SYSTEM_ID.equalsIgnoreCase(tomcatId)) {
            runMsg(session, json);
        } else {
            TomcatInfoModel tomcatInfoModel = tomcatEditService.getItem(tomcatId);
            if (tomcatInfoModel == null) {
                SocketSessionUtil.send(session, "没有对应tomcat");
                session.close();
                return;
            }
            runMsg(session, tomcatInfoModel, json);
        }
    }

    private void runMsg(Session session, JSONObject reqJson) throws Exception {
        try {
            String fileName = reqJson.getString("fileName");
            WebAopLog webAopLog = SpringUtil.getBean(WebAopLog.class);
            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(webAopLog.getPropertyValue(), fileName);
            File file1 = CACHE_FILE.get(session.getId());
            if (file1 != null && !file1.equals(file)) {
                // 离线上一个日志
                AgentFileTailWatcher.offlineFile(file, session);
            }
            try {
                AgentFileTailWatcher.addWatcher(file, session);
                CACHE_FILE.put(session.getId(), file);
            } catch (IOException io) {
                DefaultSystemLog.getLog().error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
        }
    }

    private void runMsg(Session session, TomcatInfoModel tomcatInfoModel, JSONObject reqJson) throws Exception {
        try {
            String fileName = reqJson.getString("fileName");
            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(tomcatInfoModel.getPath(), "logs", fileName);
            try {
                AgentFileTailWatcher.addWatcher(file, session);
            } catch (IOException io) {
                DefaultSystemLog.getLog().error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
        }
    }

    @Override
    @OnClose
    public void onClose(Session session) {
        super.onClose(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
