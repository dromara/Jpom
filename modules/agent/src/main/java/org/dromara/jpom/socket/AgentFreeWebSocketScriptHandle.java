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
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.websocket.Constants;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.FileUtils;
import org.dromara.jpom.util.SocketSessionUtil;
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

    private final static Map<String, ScriptProcess> CACHE = new SafeConcurrentHashMap<>();

    @Autowired
    public void init(AgentConfig agentConfig) {
        setAgentAuthorize(agentConfig.getAuthorize());
    }

    @OnOpen
    public void onOpen(Session session) {
        try {
            setLanguage(session);
            if (super.checkAuthorize(session)) {
                return;
            }
            SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.connection_successful.b331"));
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
     * @param message 消息
     * @param session 会话
     * @throws Exception 异常
     * @see Constants#DEFAULT_BUFFER_SIZE
     */
    @OnMessage(maxMessageSize = 5 * 1024 * 1024)
    public void onMessage(String message, Session session) throws Exception {
        try {
            setLanguage(session);
            if (CACHE.containsKey(session.getId())) {
                SocketSessionUtil.send(session, JsonMessage.getString(500, I18nMessageUtil.get("i18n.do_not_reopen.f86a")));
                return;
            }
            JSONObject json = JSONObject.parseObject(message);
            String type = json.getString("type");
            if (StrUtil.equals(type, "close")) {
                // 关闭、停止脚本执行
                IoUtil.close(CACHE.remove(session.getId()));
                session.close();
                return;
            }
            String path = json.getString("path");
            String tag = json.getString("tag");
            JSONObject environment = json.getJSONObject("environment");
            String content = json.getString("content");
            if (StrUtil.hasEmpty(path, tag, content)) {
                SocketSessionUtil.send(session, JsonMessage.getString(500, I18nMessageUtil.get("i18n.incorrect_parameter.02ce")));
                return;
            }
            if (environment == null) {
                SocketSessionUtil.send(session, JsonMessage.getString(500, I18nMessageUtil.get("i18n.environment_variables_not_found.dbd4")));
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
                    log.error(I18nMessageUtil.get("i18n.send_message_failure.9621"), e);
                }
            });
        } finally {
            clearLanguage();
        }
    }


    @Override
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
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
            if (StrUtil.isNotEmpty(path)) {
                File directory = FileUtil.file(path).getAbsoluteFile();
                // 需要创建目录
                FileUtil.mkdir(directory);
                processBuilder.directory(directory);
            }
            //
            process = processBuilder.start();
            inputStream = process.getInputStream();
            IoUtil.readLines(inputStream, ExtConfigBean.getConsoleLogCharset(), lineHandler);
            int waitFor = process.waitFor();
            lineHandler.handle(StrUtil.format(I18nMessageUtil.get("i18n.execution_ended.b793"), waitFor));
            // 客户端可以关闭会话啦
            lineHandler.handle("JPOM_SYSTEM_TAG:" + tag);
        }

        @Override
        public void close() throws Exception {
            IoUtil.close(inputStream);
            CommandUtil.kill(process);
            try {
                FileUtil.del(this.scriptFile);
            } catch (Exception ignored) {
            }
        }
    }
}
