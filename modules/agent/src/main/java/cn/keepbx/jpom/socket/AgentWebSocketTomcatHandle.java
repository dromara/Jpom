package cn.keepbx.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.TomcatInfoModel;
import cn.keepbx.jpom.service.manage.TomcatManageService;
import cn.keepbx.jpom.system.TopManager;
import cn.keepbx.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;

/**
 * 插件端,控制台socket
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@ServerEndpoint(value = "/tomcat_log/{tomcatId}/{optUser}")
@Component
public class AgentWebSocketTomcatHandle extends BaseAgentWebSocketHandle {

    private TomcatManageService tomcatManageService;

    @OnOpen
    public void onOpen(@PathParam("tomcatId") String tomcatId, @PathParam("optUser") String urlOptUser, Session session) {
        try {
            if (tomcatManageService == null) {
                tomcatManageService = SpringUtil.getBean(TomcatManageService.class);
            }
            TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(tomcatId);
            if (tomcatInfoModel == null) {
                SocketSessionUtil.send(session, "获取tomcat信息错误");
                session.close();
                return;
            }
            this.addUser(session, urlOptUser);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("socket 错误", e);
            try {
                SocketSessionUtil.send(session, JsonMessage.getString(500, "系统错误!"));
                session.close();
            } catch (IOException e1) {
                DefaultSystemLog.ERROR().error(e1.getMessage(), e1);
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
        TomcatInfoModel tomcatInfoModel = tomcatManageService.getItem(tomcatId);
        if (tomcatInfoModel == null) {
            SocketSessionUtil.send(session, "没有对应tomcat");
            session.close();
            return;
        }
        runMsg(session, tomcatInfoModel, json);
    }

    private void runMsg(Session session, TomcatInfoModel tomcatInfoModel, JSONObject reqJson) throws Exception {
        try {
            String fileName = reqJson.getString("fileName");
            // 进入管理页面后需要实时加载日志
            File file = FileUtil.file(tomcatInfoModel.getPath(), "logs", fileName);
            try {
                FileTailWatcher.addWatcher(file, session);
            } catch (IOException io) {
                DefaultSystemLog.ERROR().error("监听日志变化", io);
                SocketSessionUtil.send(session, io.getMessage());
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
        }
    }

    @OnClose
    public void onClose(Session session) {
        destroy(session);
        // top
        TopManager.removeMonitor(session);
    }

    private void destroy(Session session) {
        // 清理日志监听
        try {
            FileTailWatcher.offline(session);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("关闭异常", e);
        }
        USER.remove(session.getId());
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
