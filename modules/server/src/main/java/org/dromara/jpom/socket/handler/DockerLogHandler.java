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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 容器
 *
 * @author bwcx_jzy
 * @since 2022/02/10
 */
@Feature(cls = ClassFeature.DOCKER, method = MethodFeature.EXECUTE)
@Slf4j
public class DockerLogHandler extends BaseProxyHandler {


    @Override
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        super.init(session, attributes);
        //
        Object data = attributes.get("dataItem");
        Object machineData = attributes.get("machineDocker");
        String dataName = BeanUtil.getProperty(data, "name");
        String machineDataName = BeanUtil.getProperty(machineData, "name");
        this.sendMsg(session, "连接成功：" + StrUtil.emptyToDefault(dataName, machineDataName) + StrUtil.CRLF);
    }

    public DockerLogHandler() {
        super(null);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[0];
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        MachineDockerModel dockerInfoModel = (MachineDockerModel) attributes.get("machineDocker");
        if (consoleCommandOp == ConsoleCommandOp.heart) {
            return null;
        }
        if (consoleCommandOp == ConsoleCommandOp.showlog) {
            MachineDockerServer machineDockerServer = SpringUtil.getBean(MachineDockerServer.class);
            super.logOpt(this.getClass(), attributes, json);
            String containerId = json.getString("containerId");
            Map<String, Object> map = machineDockerServer.toParameter(dockerInfoModel);
            map.put("containerId", containerId);
            int tail = json.getIntValue("tail");
            if (tail > 0) {
                map.put("tail", tail);
            }
            Consumer<String> consumer = s -> {
                try {
                    SocketSessionUtil.send(session, s);
                } catch (IOException e) {
                    log.error("发消息异常", e);
                }
            };
            attributes.put("uuid", IdUtil.fastSimpleUUID());
            map.put("uuid", attributes.get("uuid"));
            map.put("charset", CharsetUtil.CHARSET_UTF_8);
            map.put("consumer", consumer);
            map.put("timestamps", json.getBoolean("timestamps"));
            ThreadUtil.execute(() -> {
                attributes.put("thread", Thread.currentThread());
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                try {
                    plugin.execute("logContainer", map);
                } catch (Exception e) {
                    log.error("拉取 容器日志异常", e);
                    try {
                        SocketSessionUtil.send(session, "执行异常:" + e.getMessage());
                    } catch (IOException ex) {
                        log.error("发消息异常", e);
                    }
                }
                log.debug("docker log 线程结束：{} {}", dockerInfoModel.getName(), attributes.get("uuid"));
            });
        } else {
            return null;
        }
        return null;
    }


    @Override
    public void destroy(WebSocketSession session) {
        //
        super.destroy(session);
        Map<String, Object> attributes = session.getAttributes();
        String uuid = (String) attributes.get("uuid");
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
            Map<String, Object> map = MapUtil.of("uuid", uuid);
            plugin.execute("closeAsyncResource", map);
        } catch (Exception e) {
            log.error("关闭资源失败", e);
        }
        Thread thread = (Thread) attributes.get("thread");
        Optional.ofNullable(thread).ifPresent(Thread::interrupt);
        SocketSessionUtil.close(session);
    }
}
