package io.jpom.socket;

import io.jpom.socket.handler.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * socket 配置
 *
 * @author jiangzeyin
 */
@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {
    private final ServerWebSocketInterceptor serverWebSocketInterceptor = new ServerWebSocketInterceptor();
    private final ServerNodeUpdateWebSocketInterceptor serverNodeUpdateWebSocketInterceptor = new ServerNodeUpdateWebSocketInterceptor();

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 控制台
        registry.addHandler(new ConsoleHandler(), "/console")
                .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 脚本模板
        registry.addHandler(new ScriptHandler(), "/script_run")
                .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // tomcat
        registry.addHandler(new TomcatHandler(), "/tomcat_log")
                .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // ssh
        registry.addHandler(new SshHandler(), "/ssh")
                .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 节点升级
        registry.addHandler(new NodeUpdateHandler(), "/node_update")
                .addInterceptors(serverNodeUpdateWebSocketInterceptor).setAllowedOrigins("*");
    }
}
