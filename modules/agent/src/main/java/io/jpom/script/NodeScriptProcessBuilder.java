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
package io.jpom.script;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.Const;
import io.jpom.common.JsonMessage;
import io.jpom.model.EnvironmentMapBuilder;
import io.jpom.model.data.NodeScriptModel;
import io.jpom.service.system.AgentWorkspaceEnvVarService;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.system.ExtConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.FileUtils;
import io.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;

import javax.websocket.Session;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脚本执行
 *
 * @author jiangzeyin
 * @since 2019/4/25
 */
@Slf4j
public class NodeScriptProcessBuilder extends BaseRunScript implements Runnable {
    /**
     * 执行中的缓存
     */
    private static final ConcurrentHashMap<String, NodeScriptProcessBuilder> FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private final ProcessBuilder processBuilder;
    private final Set<Session> sessions = new HashSet<>();
    private final String executeId;
    private final File scriptFile;
    private final EnvironmentMapBuilder environmentMapBuilder;

    private NodeScriptProcessBuilder(NodeScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        super(nodeScriptModel.logFile(executeId), CharsetUtil.CHARSET_UTF_8);
        this.executeId = executeId;
        //
        String dataPath = JpomApplication.getInstance().getDataPath();
        scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));

        FileUtils.writeScript(nodeScriptModel.getContext(), scriptFile, ExtConfigBean.getConsoleLogCharset());
        //
        String script = FileUtil.getAbsolutePath(scriptFile);
        processBuilder = new ProcessBuilder();
        List<String> command = StrUtil.splitTrim(args, StrUtil.SPACE);
        command.add(0, script);
        CommandUtil.paddingPrefix(command);
        log.debug(CollUtil.join(command, StrUtil.SPACE));
        String workspaceId = nodeScriptModel.getWorkspaceId();
        // 添加环境变量
        Map<String, String> environment = processBuilder.environment();
        AgentWorkspaceEnvVarService workspaceService = SpringUtil.getBean(AgentWorkspaceEnvVarService.class);
        environmentMapBuilder = workspaceService.getEnv(workspaceId);
        environmentMapBuilder.putStr(paramMap);
        environment.putAll(environmentMapBuilder.environment());
        processBuilder.redirectErrorStream(true);
        processBuilder.command(command);
        processBuilder.directory(scriptFile.getParentFile());
    }

    /**
     * 创建执行 并监听
     *
     * @param nodeScriptModel 脚本模版
     * @param executeId       执行ID
     * @param args            参数
     * @param paramMap        执行环境变量参数
     */
    public static NodeScriptProcessBuilder create(NodeScriptModel nodeScriptModel, String executeId, String args, Map<String, String> paramMap) {
        return FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.computeIfAbsent(executeId, file1 -> {
            NodeScriptProcessBuilder nodeScriptProcessBuilder1 = new NodeScriptProcessBuilder(nodeScriptModel, executeId, args, paramMap);
            ThreadUtil.execute(nodeScriptProcessBuilder1);
            return nodeScriptProcessBuilder1;
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
    public static void addWatcher(NodeScriptModel nodeScriptModel, String executeId, String args, Session session) {
        NodeScriptProcessBuilder nodeScriptProcessBuilder = create(nodeScriptModel, executeId, args, null);
        //
        if (nodeScriptProcessBuilder.sessions.add(session)) {
            if (FileUtil.exist(nodeScriptProcessBuilder.logFile)) {
                // 读取之前的信息并发送
                FileUtil.readLines(nodeScriptProcessBuilder.logFile, CharsetUtil.CHARSET_UTF_8, (LineHandler) line -> {
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
    public static void stopWatcher(Session session) {
        Collection<NodeScriptProcessBuilder> nodeScriptProcessBuilders = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.values();
        for (NodeScriptProcessBuilder nodeScriptProcessBuilder : nodeScriptProcessBuilders) {
            Set<Session> sessions = nodeScriptProcessBuilder.sessions;
            sessions.removeIf(session1 -> session1.getId().equals(session.getId()));
        }
    }

    /**
     * 停止脚本命令
     *
     * @param executeId 执行ID
     */
    public static void stopRun(String executeId) {
        NodeScriptProcessBuilder nodeScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.get(executeId);
        if (nodeScriptProcessBuilder != null) {
            nodeScriptProcessBuilder.end("停止运行");
        }
    }

    @Override
    public void run() {
        //初始化ProcessBuilder对象
        try {
            environmentMapBuilder.eachStr(this::info);
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), (LineHandler) NodeScriptProcessBuilder.this::info);
            int waitFor = process.waitFor();
            this.system("执行结束:{}", waitFor);
            JsonMessage<String> jsonMessage = new JsonMessage<>(200, "执行完毕:" + waitFor);
            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put(Const.SOCKET_MSG_TAG, Const.SOCKET_MSG_TAG);
            jsonObject.put("op", ConsoleCommandOp.stop.name());
            this.end(jsonObject.toString());
        } catch (Exception e) {
            log.error("执行异常", e);
            this.systemError("执行异常", e.getMessage());
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
        Iterator<Session> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, msg);
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
            iterator.remove();
        }
        NodeScriptProcessBuilder nodeScriptProcessBuilder = FILE_SCRIPT_PROCESS_BUILDER_CONCURRENT_HASH_MAP.remove(this.executeId);
        IoUtil.close(nodeScriptProcessBuilder);
    }

    @Override
    public void close() {
        super.close();
        try {
            FileUtil.del(this.scriptFile);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void msgCallback(String info) {
        //
        Iterator<Session> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            Session session = iterator.next();
            try {
                SocketSessionUtil.send(session, info);
            } catch (IOException e) {
                log.error("发送消息失败", e);
                iterator.remove();
            }
        }
    }
}
