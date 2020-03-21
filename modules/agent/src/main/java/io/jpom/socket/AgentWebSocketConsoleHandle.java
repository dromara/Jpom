package io.jpom.socket;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.ProjectInfoModel;
import io.jpom.service.manage.ConsoleService;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.util.SocketSessionUtil;
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
@ServerEndpoint(value = "/console/{projectId}/{optUser}")
@Component
public class AgentWebSocketConsoleHandle extends BaseAgentWebSocketHandle {


    private static ProjectInfoService projectInfoService;

    @OnOpen
    public void onOpen(@PathParam("projectId") String projectId, @PathParam("optUser") String urlOptUser, @PathParam("copyId") String copyId, Session session) {
        try {
            // 判断项目
            if (!JpomApplication.SYSTEM_ID.equals(projectId)) {
                if (projectInfoService == null) {
                    projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
                }
                ProjectInfoModel projectInfoModel = this.checkProject(projectId, copyId, session);
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

    /**
     * 静默消息不做过多处理
     *
     * @param consoleCommandOp 操作
     * @param session          回话
     * @return true
     */
    private boolean silentMsg(ConsoleCommandOp consoleCommandOp, Session session) {
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return true;
        }
//        if (consoleCommandOp == ConsoleCommandOp.top) {
//            TopManager.addMonitor(session);
//            return true;
//        }
        return false;
    }

    private ProjectInfoModel checkProject(String projectId, String copyId, Session session) throws IOException {
        ProjectInfoModel projectInfoModel = projectInfoService.getItem(projectId);
        if (projectInfoModel == null) {
            SocketSessionUtil.send(session, "没有对应项目");
            session.close();
            return null;
        }
        // 判断副本集
        if (StrUtil.isNotEmpty(copyId)) {
            ProjectInfoModel.JavaCopyItem copyItem = projectInfoModel.findCopyItem(copyId);
            if (copyItem == null) {
                SocketSessionUtil.send(session, "获取项目信息错误,没有对应副本：" + copyId);
                session.close();
                return null;
            }
        }
        return projectInfoModel;
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        JSONObject json = JSONObject.parseObject(message);
        String op = json.getString("op");
        ConsoleCommandOp consoleCommandOp = ConsoleCommandOp.valueOf(op);
        if (silentMsg(consoleCommandOp, session)) {
            return;
        }
        String projectId = json.getString("projectId");
        String copyId = json.getString("copyId");
        ProjectInfoModel projectInfoModel = this.checkProject(projectId, copyId, session);
        if (projectInfoModel == null) {
            return;
        }
        runMsg(consoleCommandOp, session, projectInfoModel, copyId, json);
    }

    private void runMsg(ConsoleCommandOp consoleCommandOp, Session session, ProjectInfoModel projectInfoModel, String copyId, JSONObject reqJson) throws Exception {
        ConsoleService consoleService = SpringUtil.getBean(ConsoleService.class);
        //
        ProjectInfoModel.JavaCopyItem copyItem = projectInfoModel.findCopyItem(copyId);
        JSONObject resultData = null;
        String strResult;
        boolean logUser = false;
        try {
            // 执行相应命令
            switch (consoleCommandOp) {
                case start:
                case restart:
                    logUser = true;
                    strResult = consoleService.execCommand(consoleCommandOp, projectInfoModel, copyItem);
                    if (strResult.contains(AbstractProjectCommander.RUNNING_TAG)) {
                        resultData = JsonMessage.toJson(200, "操作成功:" + strResult);
                    } else {
                        resultData = JsonMessage.toJson(400, strResult);
                    }
                    break;
                case stop:
                    logUser = true;
                    // 停止项目
                    strResult = consoleService.execCommand(consoleCommandOp, projectInfoModel, copyItem);
                    if (strResult.contains(AbstractProjectCommander.STOP_TAG)) {
                        resultData = JsonMessage.toJson(200, "操作成功");
                    } else {
                        resultData = JsonMessage.toJson(500, strResult);
                    }
                    break;
                case status:
                    // 获取项目状态
                    strResult = consoleService.execCommand(consoleCommandOp, projectInfoModel, copyItem);
                    if (strResult.contains(AbstractProjectCommander.RUNNING_TAG)) {
                        resultData = JsonMessage.toJson(200, "运行中", strResult);
                    } else {
                        resultData = JsonMessage.toJson(404, "未运行", strResult);
                    }
                    break;
                case showlog: {
                    // 进入管理页面后需要实时加载日志
                    //        日志文件路径
                    File file = copyItem == null ? new File(projectInfoModel.getLog()) : projectInfoModel.getLog(copyItem);
                    try {
                        AgentFileTailWatcher.addWatcher(file, session);
                    } catch (IOException io) {
                        DefaultSystemLog.getLog().error("监听日志变化", io);
                        SocketSessionUtil.send(session, io.getMessage());
                    }
                    break;
                }
                default:
                    resultData = JsonMessage.toJson(404, "不支持的方式：" + consoleCommandOp.name());
                    break;
            }
        } catch (Exception e) {
            DefaultSystemLog.getLog().error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
            return;
        } finally {
            if (logUser) {
                // 记录操作人
                ProjectInfoModel newProjectInfoModel = projectInfoService.getItem(projectInfoModel.getId());
                String name = getOptUserName(session);
                newProjectInfoModel.setModifyUser(name);
                projectInfoService.updateItem(newProjectInfoModel);
            }
        }
        // 返回数据
        if (resultData != null) {
            reqJson.putAll(resultData);
            DefaultSystemLog.getLog().info(reqJson.toString());
            SocketSessionUtil.send(session, reqJson.toString());
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
