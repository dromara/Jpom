/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.SocketSessionUtil;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
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
        this.sendMsg(session, I18nMessageUtil.get("i18n.connection_successful_with_message.5cf2") + StrUtil.emptyToDefault(dataName, machineDataName) + StrUtil.CRLF);
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
            ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
            super.logOpt(this.getClass(), attributes, json);
            String containerId = json.getString("containerId");
            Map<String, Object> map = machineDockerServer.toParameter(dockerInfoModel);
            map.put("containerId", containerId);
            int tail = json.getIntValue("tail");
            UserModel userModel = (UserModel) attributes.get("userInfo");
            if (userModel == null) {
                return I18nMessageUtil.get("i18n.user_not_exist.4892");
            }
            if (tail > 0) {
                map.put("tail", tail);
            }
            String uuid = IdUtil.fastSimpleUUID();
            File file = FileUtil.file(serverConfig.getUserTempPath(userModel.getId()), "docker-log", uuid + ".log");
            LogRecorder logRecorder = LogRecorder.builder().file(file).build();
            Consumer<String> consumer = s -> {
                try {
                    logRecorder.append(s);
                    SocketSessionUtil.send(session, s);
                } catch (IOException e) {
                    log.error(I18nMessageUtil.get("i18n.send_message_exception.7817"), e);
                }
            };
            attributes.put("uuid", uuid);
            attributes.put("logRecorder", logRecorder);
            map.put("uuid", uuid);
            map.put("charset", CharsetUtil.CHARSET_UTF_8);
            map.put("consumer", consumer);
            map.put("timestamps", json.getBoolean("timestamps"));
            I18nThreadUtil.execute(() -> {
                attributes.put("thread", Thread.currentThread());
                IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_PLUGIN_NAME);
                try {
                    plugin.execute("logContainer", map);
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.container_log_fetch_exception.591a"), e);
                    try {
                        SocketSessionUtil.send(session, I18nMessageUtil.get("i18n.execution_exception_message.ef79") + e.getMessage());
                    } catch (IOException ex) {
                        log.error(I18nMessageUtil.get("i18n.send_message_exception.7817"), e);
                    }
                }
                log.debug(I18nMessageUtil.get("i18n.docker_log_thread_ended.8230"), dockerInfoModel.getName(), uuid);
            });
            SocketSessionUtil.send(session, JsonMessage.getString(200, "JPOM_MSG_UUID", uuid));
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
            log.error(I18nMessageUtil.get("i18n.close_resource_failure.dc66"), e);
        }
        LogRecorder logRecorder = (LogRecorder) attributes.get("logRecorder");
        IoUtil.close(logRecorder);
        // 删除日志缓存
        UserModel userModel = (UserModel) attributes.get("userInfo");
        Optional.ofNullable(userModel).ifPresent(userModel1 -> {
            ServerConfig serverConfig = SpringUtil.getBean(ServerConfig.class);
            File file = FileUtil.file(serverConfig.getUserTempPath(userModel1.getId()), "docker-log", uuid + ".log");
            FileUtil.del(file);
        });
        Thread thread = (Thread) attributes.get("thread");
        Optional.ofNullable(thread).ifPresent(Thread::interrupt);
        SocketSessionUtil.close(session);
    }
}
