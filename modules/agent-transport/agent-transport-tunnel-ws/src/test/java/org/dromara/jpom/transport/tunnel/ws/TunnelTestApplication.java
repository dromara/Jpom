/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 隧道集成测试用的最小 Spring Boot 应用：暴露真实 {@code /agent/tunnel} 端点与 {@code /socket/echo} 回显端点。
 *
 * @author jpom
 * @since 2026/11/18
 */
@SpringBootApplication
@EnableWebSocket
public class TunnelTestApplication implements WebSocketConfigurer {

    /**
     * 测试用连接密钥
     */
    public static final String KEY = "test-tunnel-key";
    /**
     * 测试用机器节点标识
     */
    public static final String MACHINE_ID = "machine-test-1";
    /**
     * 认证通过版本号
     */
    public static final String AGENT_VERSION = "2.11.14";

    private static final TunnelServerOptions OPTIONS = new TunnelServerOptions();

    private static final TunnelChannelRegistry REGISTRY = new TunnelChannelRegistry(OPTIONS);

    @Bean
    public org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean webSocketContainer() {
        org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean factoryBean =
            new org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean();
        factoryBean.setMaxBinaryMessageBufferSize(TunnelWsContainer.MESSAGE_BUFFER_BYTES);
        factoryBean.setMaxTextMessageBufferSize(TunnelWsContainer.MESSAGE_BUFFER_BYTES);
        return factoryBean;
    }

    @Bean
    public TunnelChannelRegistry tunnelChannelRegistry() {
        REGISTRY.start();
        return REGISTRY;
    }

    /**
     * 测试用鉴权：密钥与机器标识必须匹配
     *
     * @return 鉴权器
     */
    @Bean
    public TunnelAuthenticator tunnelAuthenticator() {
        return (hello, remoteAddress) -> {
            if (!KEY.equals(hello.getKey())) {
                return TunnelAuthResult.deny(4001, "连接密钥不正确");
            }
            if (!MACHINE_ID.equals(hello.getMachineId())) {
                return TunnelAuthResult.deny(4002, "节点未注册");
            }
            return TunnelAuthResult.ok(hello.getMachineId());
        };
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TunnelServerHandler(REGISTRY, tunnelAuthenticator(), OPTIONS, AGENT_VERSION),
            TunnelServerHandler.endpointPath()).setAllowedOrigins("*");
        registry.addHandler(new EchoHandler(), "/socket/echo").setAllowedOrigins("*");
    }

    public TunnelChannelRegistry registry() {
        return REGISTRY;
    }

    /**
     * 回显端点：验证反向模式下的双向流
     */
    public static class EchoHandler extends TextWebSocketHandler {
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            session.sendMessage(new TextMessage("echo:" + message.getPayload()));
        }
    }
}
