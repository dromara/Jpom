/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.tunnel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.transport.tunnel.ws.LocalHttpTunnelHandler;
import org.dromara.jpom.transport.tunnel.ws.LocalWebSocketTunnelHandler;
import org.dromara.jpom.transport.tunnel.ws.TunnelClient;
import org.dromara.jpom.transport.tunnel.ws.TunnelClientOptions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件端反向隧道启动器：配置齐全时主动连接服务端，并把请求转发到本机接口。
 * <p>
 * 只做出站连接，插件端不需要对服务端开放任何端口。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
@Component
public class TunnelAgentRunner implements ApplicationRunner, DisposableBean {

    private final TunnelAgentProperties properties;
    private final AgentConfig agentConfig;
    private final JpomApplication jpomApplication;

    private volatile TunnelClient tunnelClient;

    public TunnelAgentRunner(TunnelAgentProperties properties, AgentConfig agentConfig, JpomApplication jpomApplication) {
        this.properties = properties;
        this.agentConfig = agentConfig;
        this.jpomApplication = jpomApplication;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("未完整配置 jpom.agent.tunnel.server-url/key/machine-id，反向隧道未启用（保持仅监听本机端口）");
            return;
        }
        int port = jpomApplication.getPort();
        // 授权头由服务端随请求/流打开参数转发（插件端不读取自身授权摘要，AgentAuthorize#getAuthorize 亦不对外暴露）
        Map<String, String> authorize = java.util.Collections.emptyMap();
        TunnelClientOptions options = new TunnelClientOptions();
        options.setServerUrl(properties.getServerUrl());
        options.setMachineId(properties.getMachineId());
        options.setKey(properties.getKey());
        options.setAgentVersion(JpomManifest.getInstance().getVersion());
        options.setOs(SystemUtil.getOsInfo().getName());
        options.setArch(SystemUtil.getOsInfo().getArch());
        options.setLocalHttpBase("http://127.0.0.1:" + port);
        options.setLocalWsBase("ws://127.0.0.1:" + port);
        options.setLocalHeaders(authorize);
        options.setLocalQuery(authorize);
        options.setMaxConcurrency(properties.getMaxConcurrency());
        options.setTimeoutSeconds(properties.getTimeoutSeconds());
        options.setRequestHandler(new LocalHttpTunnelHandler(options.getLocalHttpBase(), authorize, properties.getTimeoutSeconds()));
        options.setStreamHandler(new LocalWebSocketTunnelHandler(options.getLocalWsBase(), authorize, properties.getTimeoutSeconds()));
        this.tunnelClient = new TunnelClient(options);
        this.tunnelClient.start();
        log.info("反向隧道已启动，服务端 {} 节点 {}", properties.getServerUrl(), properties.getMachineId());
    }

    /**
     * 隧道是否在线（供状态接口使用）
     *
     * @return true 在线
     */
    public boolean online() {
        TunnelClient client = this.tunnelClient;
        return client != null && client.isConnected();
    }

    @Override
    public void destroy() {
        TunnelClient client = this.tunnelClient;
        if (client != null) {
            client.stop();
        }
    }
}
