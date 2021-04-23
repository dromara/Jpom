package io.jpom.socket.spring;

import io.jpom.socket.spring.handler.NodeUpdateHandler;
import io.jpom.socket.spring.interceptor.NodeUpdateInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @author lf
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final NodeUpdateInterceptor nodeUpdateInterceptor = new NodeUpdateInterceptor();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 节点升级
        registry.addHandler(new NodeUpdateHandler(), "/node_update").addInterceptors(nodeUpdateInterceptor);
    }
}
