package org.dromara.jpom.transport.properties;

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
@Configuration
@ConfigurationProperties(prefix = "jpom.thread-pool")
public class ThreadPoolProperties {

    private int coreSize = 8;

    private int maxSize = 16;

    private Duration keepAliveTime = Duration.of(1, ChronoUnit.MINUTES);

    private int queueCapacity = 10000;

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}
