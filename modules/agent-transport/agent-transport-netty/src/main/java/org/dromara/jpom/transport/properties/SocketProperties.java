package org.dromara.jpom.transport.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Socket参数配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@Configuration
@ConfigurationProperties(prefix = "jpom.socket")
public class SocketProperties {
}
