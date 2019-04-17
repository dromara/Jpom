package cn.keepbx.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.service.manage.ConsoleService;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import cn.keepbx.jpom.system.TopManager;
import cn.keepbx.jpom.util.SocketSessionUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件端socket
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@ServerEndpoint(value = "/console/{projectId}/{optUser}")
@Component
public class AgentWebSocketHandle {

    private static final ConcurrentHashMap<String, String> USER = new ConcurrentHashMap<>();
    private static ProjectInfoService projectInfoService;

    @OnOpen
    public void onOpen(@PathParam("projectId") String projectId, @PathParam("optUser") String urlOptUser, Session session) {
        System.out.println(projectId);
        try {
            // 判断项目
            if (!WebSocketConfig.SYSTEM_ID.equals(projectId)) {
                if (projectInfoService == null) {
                    projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
                }
                ProjectInfoModel projectInfoModel = projectInfoService.getItem(projectId);
                if (projectInfoModel == null) {
                    SocketSessionUtil.send(session, "获取项目信息错误");
                    session.close();
                    return;
                }
            }
            String optUser = URLUtil.decode(urlOptUser);
            USER.put(session.getId(), optUser);
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

    private String getOptUserName(Session session) {
        String name = USER.get(session.getId());
        return StrUtil.emptyToDefault(name, StrUtil.DASHED);
    }

    private boolean silentMsg(CommandOp commandOp, Session session) {
        if (commandOp == CommandOp.heart) {
            return true;
        }
        if (commandOp == CommandOp.top) {
            TopManager.addMonitor(session);
            return true;
        }
        return false;
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject json = JSONObject.parseObject(message);
        String op = json.getString("op");
        CommandOp commandOp = CommandOp.valueOf(op);
        if (silentMsg(commandOp, session)) {
            return;
        }
        String projectId = json.getString("projectId");
        projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(projectId);
        System.out.println(op + "  " + projectId);
        if (projectInfoModel == null) {
            SocketSessionUtil.send(session, "没有对应项目");
            return;
        }
        runMsg(commandOp, session, projectInfoModel);
    }

    private void runMsg(CommandOp commandOp, Session session, ProjectInfoModel projectInfoModel) throws Exception {
        ConsoleService consoleService = SpringUtil.getBean(ConsoleService.class);
        JSONObject resultData = null;
        String strResult;
        boolean logUser = false;
        try {
            // 执行相应命令
            switch (commandOp) {
                case start:
                case restart:
                    logUser = true;
                    strResult = consoleService.execCommand(commandOp, projectInfoModel);
                    if (strResult.contains(AbstractProjectCommander.RUNING_TAG)) {
                        resultData = JsonMessage.toJson(200, "操作成功:" + strResult);
                    } else {
                        resultData = JsonMessage.toJson(400, strResult);
                    }
                    break;
                case stop:
                    logUser = true;
                    // 停止项目
                    strResult = consoleService.execCommand(commandOp, projectInfoModel);
                    if (strResult.contains(AbstractProjectCommander.STOP_TAG)) {
                        resultData = JsonMessage.toJson(200, "操作成功");
                    } else {
                        resultData = JsonMessage.toJson(500, strResult);
                    }
                    break;
                case status:
                    // 获取项目状态
                    strResult = consoleService.execCommand(commandOp, projectInfoModel);
                    if (strResult.contains(AbstractProjectCommander.RUNING_TAG)) {
                        resultData = JsonMessage.toJson(200, "运行中", strResult);
                    } else {
                        resultData = JsonMessage.toJson(404, "未运行", strResult);
                    }
                    break;
                case showlog: {
                    // 进入管理页面后需要实时加载日志
                    String log = projectInfoModel.getLog();
                    try {
                        FileTailWatcher.addWatcher(log, session);
                    } catch (IOException io) {
                        DefaultSystemLog.ERROR().error(io.getMessage(), io);
                        SocketSessionUtil.send(session, io.getMessage());
                    }
                    break;
                }
                default:
                    resultData = JsonMessage.toJson(404, "不支持的方式：" + commandOp.name());
                    break;
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
            return;
        } finally {
            if (logUser) {
                // 记录操作人
                projectInfoModel = projectInfoService.getItem(projectInfoModel.getId());
                String name = getOptUserName(session);
                projectInfoModel.setModifyUser(name);
                projectInfoService.updateItem(projectInfoModel);
            }
        }
        //
        if (resultData != null) {
            resultData.put("op", commandOp.name());
            DefaultSystemLog.LOG().info(resultData.toString());
            SocketSessionUtil.send(session, resultData.toString());
        }
    }

    @OnClose
    public void onClose(Session session) {
        destroy(session);
        // top
        TopManager.removeMonitor(session);
        System.out.println("close");
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
    public void onError(Session session, Throwable thr) {
        // java.io.IOException: Broken pipe
        try {
            SocketSessionUtil.send(session, "服务端发生异常" + ExceptionUtil.stacktraceToString(thr));
        } catch (IOException ignored) {
        }
        DefaultSystemLog.ERROR().error(session.getId() + "socket 异常", thr);
    }
}
