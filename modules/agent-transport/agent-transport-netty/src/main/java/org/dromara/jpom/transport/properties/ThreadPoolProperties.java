package org.dromara.jpom.transport.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * 线程池参数配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jpom.thread-pool")
public class ThreadPoolProperties {

    private int coreSize = 8;

    private int maxSize = 16;

    private Duration keepAliveTime = Duration.of(1, ChronoUnit.MINUTES);

    private int queueCapacity = 10000;
}
