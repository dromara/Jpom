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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.script.BaseRunScript;
import org.dromara.jpom.script.CommandParam;
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

    private ServerScriptProcessBuilder(ScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        super(nodeScriptModel.logFile(executeId), CharsetUtil.CHARSET_UTF_8);
        this.executeId = executeId;
        //
        WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
        environmentMapBuilder = workspaceEnvVarService.getEnv(nodeScriptModel.getWorkspaceId());
        environmentMapBuilder.putStr(paramMap);
        scriptFile = nodeScriptModel.scriptFile();
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
            ThreadUtil.execute(serverScriptProcessBuilder1);
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
                        log.error("发送消息失败", e);
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
            serverScriptProcessBuilder.end("停止运行");
        }
    }

    @Override
    public void run() {
        //初始化ProcessBuilder对象
        try {
            this.environmentMapBuilder.eachStr(this::info);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) ServerScriptProcessBuilder.this::info);
            int waitFor = process.waitFor();
            this.system("执行结束:{}", waitFor);
            //
            JsonMessage<String> jsonMessage = new JsonMessage<>(200, "执行完毕:" + waitFor);
            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            jsonObject.put("op", ConsoleCommandOp.stop.name());
            this.end(jsonObject.toString());
        } catch (Exception e) {
            log.error("执行异常", e);
            this.system("执行异常", e.getMessage());
            this.end("执行异常：" + e.getMessage());
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
                log.error("发送消息失败", e);
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
                log.error("发送消息失败", e);
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
