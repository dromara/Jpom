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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ServerWebSocketHandler(), "/console")
                .addInterceptors(new ServerWebSocketInterceptor()).setAllowedOrigins("*");
    }
}
