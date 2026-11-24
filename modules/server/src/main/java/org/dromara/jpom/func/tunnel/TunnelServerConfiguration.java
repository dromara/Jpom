/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.transport.TransportServer;
import org.dromara.jpom.transport.TransportServerFactory;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.dromara.jpom.transport.tunnel.ws.TunnelServerHandler;
import org.dromara.jpom.transport.tunnel.ws.TunnelServerOptions;
import org.dromara.jpom.transport.tunnel.ws.TunnelWsContainer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 反向隧道服务端装配：注册 {@code /agent/tunnel} 端点、启动心跳巡检、把路由门面注册到传输工厂。
 * <p>
 * 未配置 {@code jpom.agent.tunnel.key} 时不注册端点、不改动传输工厂，正向模式行为不变。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TunnelConfig.class)
public class TunnelServerConfiguration implements WebSocketConfigurer {

    private final TunnelConfig tunnelConfig;
    private final TunnelServerAuth tunnelServerAuth;
    private final TunnelPushService tunnelPushService;
    private final TunnelServerOptions serverOptions = new TunnelServerOptions();
    private final TunnelChannelRegistry registry;
    private final TransportServer forwardTransport;

    public TunnelServerConfiguration(TunnelConfig tunnelConfig, TunnelServerAuth tunnelServerAuth,
                                     TunnelPushService tunnelPushService) {
        this.tunnelConfig = tunnelConfig;
        this.tunnelServerAuth = tunnelServerAuth;
        this.tunnelPushService = tunnelPushService;
        this.serverOptions.setHeartbeatSeconds(tunnelConfig.getHeartbeatSeconds());
        this.serverOptions.setMaxMissedPong(tunnelConfig.getMaxMissedPong());
        this.serverOptions.setMaxConcurrency(tunnelConfig.getMaxConcurrency());
        this.serverOptions.setHelloTimeoutSeconds(tunnelConfig.getHelloTimeoutSeconds());
        this.registry = new TunnelChannelRegistry(this.serverOptions);
        this.forwardTransport = TransportServerFactory.of();
    }

    @Bean
    public TunnelChannelRegistry tunnelChannelRegistry() {
        return this.registry;
    }

    @Bean
    public ServletServerContainerFactoryBean tunnelWebSocketContainer() {
        ServletServerContainerFactoryBean factoryBean = new ServletServerContainerFactoryBean();
        factoryBean.setMaxBinaryMessageBufferSize(TunnelWsContainer.MESSAGE_BUFFER_BYTES);
        factoryBean.setMaxTextMessageBufferSize(TunnelWsContainer.MESSAGE_BUFFER_BYTES);
        return factoryBean;
    }

    @PostConstruct
    public void init() {
        if (!tunnelConfig.enabled()) {
            log.info("未配置 {}，反向连接隧道未启用（保持正向拉取模式）", TunnelConst.CONFIG_KEY);
            return;
        }
        this.registry.addPushListener(this.tunnelPushService);
        this.registry.addListener(new TunnelPushSnapshotCleaner(this.registry, this.tunnelPushService));
        this.registry.start();
        TransportServerFactory.set(new RoutingTransportServer(this.registry, this.forwardTransport));
        log.info("反向连接隧道已启用，端点 {}，自动注册={}{}", TunnelConst.ENDPOINT_PATH, tunnelConfig.isAllowAutoRegister(),
            StrUtil.isNotEmpty(tunnelConfig.getAutoRegisterWorkspace())
                ? "，自动分配到工作空间 " + tunnelConfig.getAutoRegisterWorkspace() : "");
    }

    @PreDestroy
    public void destroy() {
        TransportServerFactory.set(null);
        this.registry.shutdown();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        if (!tunnelConfig.enabled()) {
            return;
        }
        TunnelServerHandler handler = new TunnelServerHandler(this.registry, tunnelServerAuth, serverOptions,
            JpomManifest.getInstance().getVersion());
        registry.addHandler(handler, TunnelConst.ENDPOINT_PATH).setAllowedOrigins("*");
    }
}