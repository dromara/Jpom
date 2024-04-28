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

import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.socket.handler.*;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * socket 配置
 *
 * @author bwcx_jzy
 */
@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {
    private final ServerWebSocketInterceptor serverWebSocketInterceptor;
    private final SystemParametersServer systemParametersServer;
    private final NodeConfig nodeConfig;
    private final MachineNodeServer machineNodeServer;

    public ServerWebSocketConfig(ServerWebSocketInterceptor serverWebSocketInterceptor,
                                 SystemParametersServer systemParametersServer,
                                 ServerConfig serverConfig,
                                 MachineNodeServer machineNodeServer) {
        this.serverWebSocketInterceptor = serverWebSocketInterceptor;
        this.systemParametersServer = systemParametersServer;
        this.nodeConfig = serverConfig.getNode();
        this.machineNodeServer = machineNodeServer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 控制台
        registry.addHandler(new ConsoleHandler(), "/socket/console")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 节点脚本模板
        registry.addHandler(new NodeScriptHandler(), "/socket/node/script_run")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 系统日志
        registry.addHandler(new SystemLogHandler(), "/socket/system_log")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 插件端日志
        registry.addHandler(new AgentLogHandler(), "/socket/agent_log")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // ssh
        registry.addHandler(new SshHandler(), "/socket/ssh")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 节点升级
        registry.addHandler(new NodeUpdateHandler(machineNodeServer, systemParametersServer, nodeConfig), "/socket/node_update")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 脚本模板
        registry.addHandler(new ServerScriptHandler(), "/socket/script_run")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // docker log
        registry.addHandler(new DockerLogHandler(), "/socket/docker_log")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // docker cli
        registry.addHandler(new DockerCliHandler(), "/socket/docker_cli")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // free script
        registry.addHandler(new FreeScriptHandler(), "/socket/free_script")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
    }
}
