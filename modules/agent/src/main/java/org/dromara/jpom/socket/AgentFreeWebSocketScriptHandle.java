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
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.SocketSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自由脚本socket
 *
 * @author bwcx_jzy
 * @since 2023/03/28
 */
@ServerEndpoint(value = "/free-script-run")
@Component
@Slf4j
public class AgentFreeWebSocketScriptHandle extends BaseAgentWebSocketHandle {

    private final static Map<String, ScriptProcess> CACHE = new ConcurrentHashMap<>();

    @Autowired
    public void init() {

    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            if (super.checkAuthorize(session)) {
                return;
            }
            SocketSessionUtil.send(session, "连接成功");
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

    @OnMessage
    public void onMessage(String message, Session session) throws Exception {
        if (CACHE.containsKey(session.getId())) {
            SocketSessionUtil.send(session, JsonMessage.getString(500, "不要重复打开"));
            return;
        }
        JSONObject json = JSONObject.parseObject(message);
        String path = json.getString("path");
        String tag = json.getString("tag");
        JSONObject environment = json.getJSONObject("environment");
        String content = json.getString("content");
        if (StrUtil.hasEmpty(path, tag, content)) {
            SocketSessionUtil.send(session, JsonMessage.getString(500, "参数存在不正确"));
            return;
        }
        if (environment == null) {
            SocketSessionUtil.send(session, JsonMessage.getString(500, "没有环境变量"));
            return;
        }
        Map<String, EnvironmentMapBuilder.Item> map = environment.to(new TypeReference<Map<String, EnvironmentMapBuilder.Item>>() {
        });
        ScriptProcess scriptProcess = new ScriptProcess(content, map, path, tag);
        CACHE.put(session.getId(), scriptProcess);
        scriptProcess.run(line -> {
            try {
                SocketSessionUtil.send(session, line);
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        });
    }


    @Override
    @OnClose
    public void onClose(Session session) {
        super.onClose(session);
        IoUtil.close(CACHE.remove(session.getId()));
    }

    @OnError
    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
        IoUtil.close(CACHE.remove(session.getId()));
    }

    public static class ScriptProcess implements AutoCloseable {
        private final String content;
        private final EnvironmentMapBuilder environment;
        private final String path;
        private final String tag;

        private Process process;
        private InputStream inputStream;
        private File scriptFile;

        public ScriptProcess(String content, Map<String, EnvironmentMapBuilder.Item> environment, String path, String tag) {
            this.content = content;
            this.environment = EnvironmentMapBuilder.builder(environment);
            this.path = path;
            this.tag = tag;
        }

        /**
         * 开始执行脚本
         *
         * @param lineHandler 回调
         * @throws IOException          io 异常
         * @throws InterruptedException 中断
         */
        public void run(LineHandler lineHandler) throws IOException, InterruptedException {
            String dataPath = JpomApplication.getInstance().getDataPath();
            this.scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, StrUtil.format("{}.{}", IdUtil.fastSimpleUUID(), CommandUtil.SUFFIX));
            FileUtils.writeScript(this.content, scriptFile, ExtConfigBean.getConsoleLogCharset());
            //
            String script = FileUtil.getAbsolutePath(scriptFile);
            ProcessBuilder processBuilder = new ProcessBuilder();
            List<String> command = new ArrayList<>();
            command.add(0, script);
            CommandUtil.paddingPrefix(command);
            log.debug(CollUtil.join(command, StrUtil.SPACE));
            // 添加环境变量
            this.environment.eachStr(lineHandler::handle);
            Map<String, String> environment = processBuilder.environment();
            environment.putAll(this.environment.environment());
            processBuilder.redirectErrorStream(true);
            processBuilder.command(command);
            //
            File directory = FileUtil.file(path).getAbsoluteFile();
            // 需要创建目录
            FileUtil.mkdir(directory);
            processBuilder.directory(directory);
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), lineHandler);
            int waitFor = process.waitFor();
            lineHandler.handle(StrUtil.format("执行结束:{}", waitFor));
            // 客户端可以关闭会话啦
            lineHandler.handle("JPOM_SYSTEM_TAG:" + tag);
        }

        @Override
        public void close() throws Exception {
            IoUtil.close(inputStream);
            Optional.ofNullable(process).ifPresent(Process::destroy);
            try {
                FileUtil.del(this.scriptFile);
            } catch (Exception ignored) {
            }
        }
    }
}
