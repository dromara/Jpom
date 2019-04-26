package cn.keepbx.jpom.socket;

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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 控制台
        registry.addHandler(new ServerWebSocketConsoleHandler(), "/console")
                .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 脚本模板
        registry.addHandler(new ServerWebSocketScriptHandler(), "/script_run")
                .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
    }
}
