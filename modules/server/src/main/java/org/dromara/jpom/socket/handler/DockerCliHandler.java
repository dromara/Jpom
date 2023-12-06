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
package org.dromara.jpom.socket.handler;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.util.SocketSessionUtil;
import org.dromara.jpom.util.StringUtil;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * docker cli
 *
 * @author bwcx_jzy
 * @since 2022/02/10
 */
@Feature(cls = ClassFeature.DOCKER, method = MethodFeature.EXECUTE)
@Slf4j
public class DockerCliHandler extends BaseTerminalHandler {

    private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new SafeConcurrentHashMap<>();


    @Override
    public void afterConnectionEstablishedImpl(WebSocketSession session) throws Exception {
        super.afterConnectionEstablishedImpl(session);
        MachineDockerServer machineDockerServer = SpringUtil.getBean(MachineDockerServer.class);
        Map<String, Object> attributes = session.getAttributes();
        MachineDockerModel dockerInfoModel = (MachineDockerModel) attributes.get("machineDocker");
        DockerInfoService dockerInfoService = SpringUtil.getBean(DockerInfoService.class);
        String containerId = (String) attributes.get("containerId");
        //
        HandlerItem handlerItem;
        try {
            DockerInfoModel model = new DockerInfoModel();
            model.setMachineDockerId(dockerInfoModel.getId());
            model = dockerInfoService.queryByBean(model);
            Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel);
            handlerItem = new HandlerItem(session, dockerInfoModel, parameter, containerId);
            handlerItem.startRead();
        } catch (Exception e) {
            // 输出超时日志 @author jzy
            log.error("docker 控制台连接超时", e);
            sendBinary(session, "docker 控制台连接超时");
            this.destroy(session);
            return;
        }
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
        //
        Thread.sleep(1000);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        if (handlerItem == null) {
            sendBinary(session, "已经离线啦");
            IoUtil.close(session);
            return;
        }
        String payload = message.getPayload();
        JSONValidator.Type type = StringUtil.validatorJson(payload);
        if (type == JSONValidator.Type.Object) {
            JSONObject jsonObject = JSONObject.parseObject(payload);
            String data = jsonObject.getString("data");
            if (StrUtil.equals(data, "jpom-heart")) {
                // 心跳消息不转发
                return;
            }
            if (StrUtil.equals(data, "resize")) {
                // 缓存区大小
                handlerItem.resize(jsonObject);
                return;
            }
        }
        try {
            handlerItem.sendCommand(payload);
        } catch (Exception e) {
            sendBinary(session, "Failure:" + e.getMessage());
            log.error("执行命令异常", e);
        }
    }

    private class HandlerItem implements Runnable, AutoCloseable {
        private final WebSocketSession session;
        private final MachineDockerModel dockerInfoModel;
        private final Map<String, Object> map;
        private PipedInputStream inputStream = new PipedInputStream();
        private PipedOutputStream outputStream = new PipedOutputStream(inputStream);
        private String containerId;
        private Thread thread;

        HandlerItem(WebSocketSession session, MachineDockerModel dockerInfoModel, Map<String, Object> map, String containerId) throws IOException {
            this.session = session;
            this.dockerInfoModel = dockerInfoModel;
            this.containerId = containerId;
            this.map = map;
        }

        void startRead() {
            ThreadUtil.execute(this);
        }

        private void sendCommand(String data) throws Exception {
            if (this.outputStream == null) {
                return;
            }
            this.outputStream.write(data.getBytes());
            this.outputStream.flush();
        }

        /**
         * 调整 缓存区大小
         *
         * @param jsonObject 参数
         */
        private void resize(JSONObject jsonObject) {
            Integer rows = Convert.toInt(jsonObject.getString("rows"), 10);
            Integer cols = Convert.toInt(jsonObject.getString("cols"), 10);
            Integer wp = Convert.toInt(jsonObject.getString("wp"), 10);
            Integer hp = Convert.toInt(jsonObject.getString("hp"), 10);
            map.put("sizeHeight", rows);
            map.put("sizeWidth", cols);
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            try {
                plugin.execute("resizeExec", map);
            } catch (Exception e) {
                log.error("执行容器命令异常", e);
                sendBinary(session, "执行异常:" + e.getMessage());
            }
        }

        @Override
        public void run() {
            map.put("containerId", containerId);
            thread = Thread.currentThread();
            Consumer<String> logConsumer = s -> {
                if (StrUtil.startWith(s, "CALLBACK_EXECID:")) {
                    // 终端id
                    String execId = StrUtil.removePrefix(s, "CALLBACK_EXECID:");
                    session.getAttributes().put("execId", execId);
                    map.put("execId", execId);
                    return;
                }
                sendBinary(session, s);
            };
            map.put("charset", CharsetUtil.CHARSET_UTF_8);
            map.put("stdin", inputStream);
            map.put("logConsumer", logConsumer);
            Consumer<String> errorConsumer = s -> {
                sendBinary(session, s);
                if (StrUtil.equals(s, "exit")) {
                    // 退出
                    destroy(session);
                }
            };
            map.put("errorConsumer", errorConsumer);
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            try {
                plugin.execute("exec", map);
            } catch (Exception e) {
                log.error("执行容器命令异常", e);
                sendBinary(session, "执行异常:" + e.getMessage());
            }
            log.debug("[{}] docker exec 终端进程结束", dockerInfoModel.getName());
            // 标记自动结束
            this.containerId = null;
        }

        private void tryExit() throws Exception {
            if (this.containerId == null) {
                // 如果线程已经结束，不再尝试发送消息
                return;
            }
            // ctrl + c
            this.sendCommand(String.valueOf((char) 3));
            ThreadUtil.sleep(100);
            // ctrl + c
            this.sendCommand(String.valueOf((char) 3));
            ThreadUtil.sleep(100);
            // quit
            this.sendCommand("quit");
            this.sendCommand(String.valueOf((char) 13));
            ThreadUtil.sleep(100);
            // exit
            this.sendCommand("exit");
            this.sendCommand(String.valueOf((char) 13));
            ThreadUtil.sleep(100);
        }

        @Override
        public void close() {
            if (this.inputStream == null) {
                // 避免多次调用
                return;
            }
            Object execId = session.getAttributes().get("execId");
            try {
                // 多次尝试退出，可能终端内部进入交互命令行
                for (int i = 0; i < 3; i++) {
                    this.tryExit();
                }
                //
                Optional.ofNullable(this.thread).ifPresent(Thread::interrupt);
            } catch (Exception e) {
                log.error("执行容器命令异常", e);
            }
            log.debug("关闭[{}] docker exec 终端：{}", dockerInfoModel.getName(), execId);
            IoUtil.close(this.inputStream);
            IoUtil.close(this.outputStream);
            this.inputStream = null;
            this.outputStream = null;
        }
    }

    @Override
    public void destroy(WebSocketSession session) {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        IoUtil.close(handlerItem);
        IoUtil.close(session);
        SocketSessionUtil.close(session);
    }
}
