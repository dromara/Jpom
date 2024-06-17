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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.script.BaseRunScript;
import org.dromara.jpom.script.CommandParam;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本执行
 *
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Slf4j
public class ServerScriptProcessBuilder extends BaseRunScript implements Runnable {
    /**
     * 执行中的缓存
     */
    private static final ConcurrentHashMap<String, ServerScriptProcessBuilder> FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP = new SafeConcurrentHashMap<>();

    private final ProcessBuilder processBuilder;
    private final Set<WebSocketSession> sessions = new HashSet<>();
    private final String executeId;
    private final File scriptFile;

    private final EnvironmentMapBuilder environmentMapBuilder;
    private ScriptExecuteLogServer scriptExecuteLogServer;

    private ServerScriptProcessBuilder(ScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        super(nodeScriptModel.logFile(executeId), CharsetUtil.CHARSET_UTF_8);
        //
        if (scriptExecuteLogServer == null) {
            scriptExecuteLogServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
        }
        this.executeId = executeId;
        //
        WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
        environmentMapBuilder = workspaceEnvVarService.getEnv(nodeScriptModel.getWorkspaceId());
        environmentMapBuilder.putStr(paramMap);
        scriptFile = scriptExecuteLogServer.toExecLogFile(nodeScriptModel);
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        processBuilder = new ProcessBuilder();
        List<String> command = CommandParam.toCommandList(args);
        command.add(0, script);
        CommandUtil.paddingPrefix(command);
        log.debug(CollUtil.join(command, StrUtil.SPACE));
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        Map<String, String> environment = processBuilder.environment();
        environment.putAll(environmentMapBuilder.environment());
        processBuilder.directory(scriptFile.getParentFile());
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     */
    public static ServerScriptProcessBuilder create(ScriptModel nodeScriptModel, String executeId, String args) {
        return create(nodeScriptModel, executeId, args, null);
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     * @param paramMap        环境变量参数
     */
    public static ServerScriptProcessBuilder create(ScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        return FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.computeIfAbsent(executeId, file1 -> {
            ServerScriptProcessBuilder serverScriptProcessBuilder1 = new ServerScriptProcessBuilder(nodeScriptModel, executeId, args, paramMap);
            I18nThreadUtil.execute(serverScriptProcessBuilder1);
            return serverScriptProcessBuilder1;
        });
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     * @param session         会话
     */
    public static void addWatcher(ScriptModel nodeScriptModel, String executeId, String args, WebSocketSession session) {
        ServerScriptProcessBuilder serverScriptProcessBuilder = create(nodeScriptModel, executeId, args);
        //
        if (serverScriptProcessBuilder.sessions.add(session)) {
            if (FileUtil.exist(serverScriptProcessBuilder.logFile)) {
                // 读取之前的信息并发送
                FileUtil.readLines(serverScriptProcessBuilder.logFile, CharsetUtil.CHARSET_UTF_8, (LineHandler) line -> {
                    try {
                        SocketSessionUtil.send(session, line);
                    } catch (IOException e) {
                        log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
                    }
                });
            }
        }
    }

    /**
     * 判断是否还在执行中
     *
     * @param executeId 执行id
     * @return true 还在执行
     */
    public static boolean isRun(String executeId) {
        return FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.containsKey(executeId);
    }

    /**
     * 关闭会话
     *
     * @param session 会话
     */
    public static void stopWatcher(WebSocketSession session) {
        Collection<ServerScriptProcessBuilder> serverScriptProcessBuilders = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.values();
        for (ServerScriptProcessBuilder serverScriptProcessBuilder : serverScriptProcessBuilders) {
            Set<WebSocketSession> sessions = serverScriptProcessBuilder.sessions;
            sessions.removeIf(session1 -> session1.getId().equals(session.getId()));
        }
    }

    /**
     * 停止脚本命令
     *
     * @param executeId 执行ID
     */
    public static void stopRun(String executeId) {
        ServerScriptProcessBuilder serverScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.get(executeId);
        if (serverScriptProcessBuilder != null) {
            serverScriptProcessBuilder.end(I18nMessageUtil.get("i18n.stop_running.1d4e"));
        }
    }

    @Override
    public void run() {
        //初始化ProcessBuilder对象
        try {
            scriptExecuteLogServer.updateStatus(executeId, CommandExecLogModel.Status.ING);
            this.environmentMapBuilder.eachStr(this::info);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) ServerScriptProcessBuilder.this::info);
            int waitFor = process.waitFor();
            this.system(I18nMessageUtil.get("i18n.execution_ended.b793"), waitFor);
            scriptExecuteLogServer.updateStatus(executeId, CommandExecLogModel.Status.DONE, waitFor);
            //
            JsonMessage<String> jsonMessage = new JsonMessage<>(200, I18nMessageUtil.get("i18n.execution_completed.24a1") + waitFor);
            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            jsonObject.put("op", ConsoleCommandOp.stop.name());
            this.end(jsonObject.toString());
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.execution_exception.b0d5"), e);
            scriptExecuteLogServer.updateStatus(executeId, CommandExecLogModel.Status.ERROR);
            this.system(I18nMessageUtil.get("i18n.execution_exception.b0d5"), e.getMessage());
            this.end(I18nMessageUtil.get("i18n.general_execution_exception.62e9") + e.getMessage());
        } finally {
            this.close();
        }
    }

    /**
     * 结束执行
     *
     * @param msg 响应的消息
     */
    @Override
    protected void end(String msg) {
        Iterator<WebSocketSession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            WebSocketSession session = iterator.next();
            try {
                SocketSessionUtil.send(session, msg);
            } catch (IOException e) {
                log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
            }
            iterator.remove();
        }
        ServerScriptProcessBuilder serverScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.remove(this.executeId);
        IoUtil.close(serverScriptProcessBuilder);
    }

    @Override
    protected void msgCallback(String info) {
        //
        Iterator<WebSocketSession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            WebSocketSession session = iterator.next();
            try {
                SocketSessionUtil.send(session, info);
            } catch (IOException e) {
                log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
                iterator.remove();
            }
        }
    }

    @Override
    public void close() {
        super.close();
        try {
            FileUtil.del(this.scriptFile);
        } catch (Exception ignored) {
        }
    }
}
