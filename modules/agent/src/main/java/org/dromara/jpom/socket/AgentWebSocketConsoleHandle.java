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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.configuration.ProjectLogConfig;
import org.dromara.jpom.model.RunMode;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
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
    private static ProjectLogConfig logConfig;
    private static ProjectCommander projectCommander;

    @Autowired
    public void init(ProjectInfoService projectInfoService,
                     AgentConfig agentConfig,
                     ProjectCommander projectCommander) {
        AgentWebSocketConsoleHandle.projectInfoService = projectInfoService;
        AgentWebSocketConsoleHandle.logConfig = agentConfig.getProject().getLog();
        AgentWebSocketConsoleHandle.projectCommander = projectCommander;
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
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
                SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.connection_successful_with_message.5cf2") + nodeProjectInfoModel.getName());
            }
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
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.no_project_specified2.a7f5") + projectId);
            session.close();
            return null;
        }
        return nodeProjectInfoModel;
    }

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
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
            // DSL
            RunMode runMode = nodeProjectInfoModel.getRunMode();
            if (runMode == RunMode.Dsl) {
                // 判断是否可以执行 reload 事件
                DslYmlDto dslYmlDto = nodeProjectInfoModel.mustDslConfig();
                boolean b = dslYmlDto.hasRunProcess(ConsoleCommandOp.reload.name());
                json.put("canReload", b);
            }
            runMsg(consoleCommandOp, session, nodeProjectInfoModel, json);
        } finally {
            clearLanguage();
        }
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
                case stop:
                case reload:
                    logUser = true;
                    strResult = projectCommander.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new JsonMessage<>(200, I18nMessageUtil.get("i18n.operation_succeeded.3313"), strResult);
                    } else {
                        resultData = new JsonMessage<>(400, strResult.msgStr());
                    }
                    break;

                case status: {
                    // 获取项目状态
                    strResult = projectCommander.execCommand(consoleCommandOp, nodeProjectInfoModel);
                    if (strResult.isSuccess()) {
                        resultData = new JsonMessage<>(200, I18nMessageUtil.get("i18n.running_status.d679"), strResult);
                    } else {
                        resultData = new JsonMessage<>(404, I18nMessageUtil.get("i18n.not_running.4f8a"), strResult);
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
                    resultData = new JsonMessage<>(404, I18nMessageUtil.get("i18n.unsupported_method_with_colon.eae8") + consoleCommandOp.name());
                    break;
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.command_execution_failed.90ef"), e);
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.command_execution_failed_details.77ed"));
            SocketSessionUtil.send(session, ExceptionUtil.stacktraceToString(e));
            return;
        } finally {
            if (logUser) {
                // 记录操作人
                NodeProjectInfoModel update = new NodeProjectInfoModel();
                String name = getOptUserName(session);
                update.setModifyUser(name);
                projectInfoService.updateById(update, nodeProjectInfoModel.getId());
            }
        }
        // 返回数据
        if (resultData != null) {
            reqJson.putAll(resultData.toJson());
            reqJson.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
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
        File libFile = projectInfoService.resolveLibFile(nodeProjectInfoModel);
        File file = FileUtil.file(libFile, fileName);
        if (!FileUtil.isFile(file)) {
            return new JsonMessage<>(404, I18nMessageUtil.get("i18n.file_not_found.d952"));
        }
        I18nThreadUtil.execute(() -> {
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
                log.error(I18nMessageUtil.get("i18n.file_search_failed.231b"), e);
                try {
                    SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.command_execution_failed_details.77ed"));
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
            file = projectInfoService.resolveAbsoluteLogFile(nodeProjectInfoModel);
        } else {
            File libFile = projectInfoService.resolveLibFile(nodeProjectInfoModel);
            file = FileUtil.file(libFile, fileName);
        }
        try {
            Charset charset = logConfig.getFileCharset();
            boolean watcher = AgentFileTailWatcher.addWatcher(file, charset, session);
            if (!watcher) {
                SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.listen_file_failed_may_not_exist.fd56"));
            }
        } catch (Exception io) {
            log.error(I18nMessageUtil.get("i18n.listen_log_changes.9081"), io);
            SocketSessionUtil.send(session, io.getMessage());
        }
    }

    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        AgentFileTailWatcher.offline(session);
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
    }
}
