package org.dromara.jpom.transport.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebSocket参数配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@Configuration
@ConfigurationProperties(prefix = "jpom.web-socket")
public class WebSocketProperties {

    private String protocol = "ws";

    private String path = "/ws";

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
