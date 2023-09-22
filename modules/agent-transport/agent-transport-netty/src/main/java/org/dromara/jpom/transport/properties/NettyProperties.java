package org.dromara.jpom.transport.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Netty参数配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jpom.netty")
public class NettyProperties {

    private String tcp = "websocket";

    private String host = "localhost";

    private int port = 2023;
}
