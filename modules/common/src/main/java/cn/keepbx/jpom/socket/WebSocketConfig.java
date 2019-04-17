package cn.keepbx.jpom.socket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * socket 配置
 *
 * @author jiangzeyin
 */
@Configuration
public class WebSocketConfig {

    public static final String SYSTEM_ID = "system";

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
