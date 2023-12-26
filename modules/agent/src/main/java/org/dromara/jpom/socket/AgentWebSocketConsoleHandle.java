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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ConsoleService;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.system.AgentConfig;
import org.dromara.jpom.util.FileSearchUtil;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 插件端,控制台socket
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@ServerEndpoint(value = "/console")
@Component
@Slf4j
public class AgentWebSocketConsoleHandle extends BaseAgentWebSocketHandle {

    private static ProjectInfoService projectInfoService;
    private static ConsoleService consoleService;
    private static AgentConfig.ProjectConfig.LogConfig logConfig;

    @Autowired
    public void init(ProjectInfoService projectInfoService,
                     ConsoleService consoleService,
                     AgentConfig agentConfig) {
        AgentWebSocketConsoleHandle.projectInfoService = projectInfoService;
        AgentWebSocketConsoleHandle.consoleService = consoleService;
        AgentWebSocketConsoleHandle.logConfig = agentConfig.getProject().getLog();
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            if (super.checkAuthorize(session)) {
                return;
            }
            String projectId = super.getParameters(session, "projectId");
            // 判断项目
            if (!Const.SYSTEM_ID.equals(projectId)) {
                NodeProjectInfoModel nodeProjectInfoModel = this.checkProject(projectId, session);
                if (nodeProjectInfoModel == null) {
                    return;
                }
                //
                SocketSessionUtil.send(session, "连接成功：" + nodeProjectInfoModel.getName());
            }
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

    private NodeProjectInfoModel checkProject(String projectId, Session session) throws IOException {
        NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(projectId);
        if (nodeProjectInfoModel == null) {
            SocketSessionUtil.send(session, "没有对应项目：" + projectId);
            session.close();
            return null;
        }
        return nodeProjectInfoModel;
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
        NodeProjectInfoModel nodeProjectInfoModel = this.checkProject(projectId, session);
        if (nodeProjectInfoModel == null) {
            return;
        }
        runMsg(consoleCommandOp, session, nodeProjectInfoModel, json);
    }

    private void runMsg(ConsoleCommandOp consoleCommandOp, Session session, NodeProjectInfoModel nodeProjectInfoModel, JSONObject reqJson) throws Exception {
        //

        JsonMessage<Object> resultData = null;
        CommandOpResult strResult;
        boolean logUser = false;
        try {
            // 执行相应命令
            switch (consoleCommandOp) {
                case start:
                case restart:
                    logUser = true;
                    strResult = consoleService.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new JsonMessage<>(200, "操作成功", strResult);
                    } else {
                        resultData = new JsonMessage<>(400, strResult.msgStr());
                    }
                    break;
                case stop: {
                    logUser = true;
                    // 停止项目
                    strResult = consoleService.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new JsonMessage<>(200, "操作成功", strResult);
                    } else {
                        resultData = new JsonMessage<>(400, strResult.msgStr());
                    }
                    break;
                }
                case status: {
                    // 获取项目状态
                    strResult = consoleService.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new JsonMessage<>(200, "运行中", strResult);
                    } else {
                        resultData = new JsonMessage<>(404, "未运行", strResult);
                    }
                    break;
                }
                case showlog: {
                    // 进入管理页面后需要实时加载日志
                    String search = reqJson.getString("search");
                    if (StrUtil.isNotEmpty(search)) {
                        resultData = searchLog(session, nodeProjectInfoModel, reqJson);
                    } else {
                        showLog(session, nodeProjectInfoModel, reqJson);
                    }
                    break;
                }
                default:
                    resultData = new JsonMessage<>(404, "不支持的方式：" + consoleCommandOp.name());
                    break;
            }
        } catch (Exception e) {
            log.error("执行命令失败", e);
            SocketSessionUtil.send(session, "执行命令失败,详情如下：");
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
            return;
        } finally {
            if (logUser) {
                // 记录操作人
                NodeProjectInfoModel newNodeProjectInfoModel = projectInfoService.getItem(nodeProjectInfoModel.getId());
                String name = getOptUserName(session);
                newNodeProjectInfoModel.setModifyUser(name);
                projectInfoService.updateItem(newNodeProjectInfoModel);
            }
        }
        // 返回数据
        if (resultData != null) {
            reqJson.putAll(resultData.toJson());
            reqJson.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            log.info(reqJson.toString());
            SocketSessionUtil.send(session, reqJson.toString());
        }
    }

    /**
     * {
     * "op": "showlog",
     * "projectId": "python",
     * "search": true,
     * "useProjectId": "python",
     * "useNodeId": "localhost",
     * "beforeCount": 0,
     * "afterCount": 10,
     * "head": 0,
     * "tail": 100,
     * "first": "false",
     * "logFile": "/run.log"
     * }
     *
     * @param session              会话
     * @param nodeProjectInfoModel 项目
     * @param reqJson              请求参数
     * @return 返回信息
     */
    private JsonMessage<Object> searchLog(Session session, NodeProjectInfoModel nodeProjectInfoModel, JSONObject reqJson) {
        //
        String fileName = reqJson.getString("logFile");
        File file = FileUtil.file(nodeProjectInfoModel.allLib(), fileName);
        if (!FileUtil.isFile(file)) {
            return new JsonMessage<>(404, "文件不存在");
        }
        ThreadUtil.execute(() -> {
            try {
                boolean first = Convert.toBool(reqJson.getString("first"), false);
                int head = reqJson.getIntValue("head");
                int tail = reqJson.getIntValue("tail");
                int beforeCount = reqJson.getIntValue("beforeCount");
                int afterCount = reqJson.getIntValue("afterCount");
                String keyword = reqJson.getString("keyword");
                Charset charset = logConfig.getFileCharset();
                //BaseFileTailWatcher.detectorCharset(file);
                String resultMsg = FileSearchUtil.searchList(file, charset, keyword, beforeCount, afterCount, head, tail, first, objects -> {
                    try {
                        String line = objects.get(1);
                        SocketSessionUtil.send(session, line);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                SocketSessionUtil.send(session, resultMsg);
            } catch (Exception e) {
                log.error("文件搜索失败", e);
                try {
                    SocketSessionUtil.send(session, "执行命令失败,详情如下：");
                } catch (IOException ignored) {
                }
            }
        });
        return null;
    }

    private void showLog(Session session, NodeProjectInfoModel nodeProjectInfoModel, JSONObject reqJson) throws IOException {
        //        日志文件路径
        String fileName = reqJson.getString("fileName");
        File file;
        if (StrUtil.isEmpty(fileName)) {
            file = new File(nodeProjectInfoModel.getLog());
        } else {
            file = FileUtil.file(nodeProjectInfoModel.allLib(), fileName);
        }
        try {
            Charset charset = logConfig.getFileCharset();
            boolean watcher = AgentFileTailWatcher.addWatcher(file, charset, session);
            if (!watcher) {
                SocketSessionUtil.send(session, "监听文件失败,可能文件不存在");
            }
        } catch (Exception io) {
            log.error("监听日志变化", io);
            SocketSessionUtil.send(session, io.getMessage());
        }
    }

    @Override
    @OnClose
    public void onClose(Session session) {
        super.onClose(session);
        AgentFileTailWatcher.offline(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
