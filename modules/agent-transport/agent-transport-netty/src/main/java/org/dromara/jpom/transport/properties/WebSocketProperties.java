package org.dromara.jpom.transport.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket参数配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jpom.web-socket")
public class WebSocketProperties {

    private String protocol = "ws";

    private String path = "/ws";
}
