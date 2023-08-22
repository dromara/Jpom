package org.dromara.jpom.transport.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Netty参数配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@Configuration
@ConfigurationProperties(prefix = "jpom.netty")
public class NettyProperties {

    private String tcp = "websocket";

    private String host = "localhost";

    private int port = 2023;

    public String getTcp() {
        return tcp;
    }

    public void setTcp(String tcp) {
        this.tcp = tcp;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
