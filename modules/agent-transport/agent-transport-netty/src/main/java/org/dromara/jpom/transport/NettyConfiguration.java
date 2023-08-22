package org.dromara.jpom.transport;

import org.dromara.jpom.transport.properties.ThreadPoolProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池、广播事件配置
 *
 * @author Hong
 * @since 2023/08/22
 */
@ComponentScan("org.dromara.jpom.transport")
@Configuration
public class NettyConfiguration {

    @Bean("JPomThreadPool")
    public ThreadPoolExecutor receiveThreadPool(ThreadPoolProperties properties) {
        return new ThreadPoolExecutor(
            properties.getCoreSize(),//核心数，一直都能工作的数量
            properties.getMaxSize(),//请求处理大时，可以开放的最大工作数
            properties.getKeepAliveTime().toMillis(),//开启最大工作数后，当无请求时，还让其存活的时间
            TimeUnit.MICROSECONDS,//存活时间单位
            new LinkedBlockingDeque<>(properties.getQueueCapacity()),//阻塞队列，保存操作请求线程
            Executors.defaultThreadFactory(),//创建线程的工厂类
            new ThreadPoolExecutor.AbortPolicy());
    }


    @Bean
    public ApplicationEventMulticaster applicationEventMulticaster(@Qualifier("JPomThreadPool") ThreadPoolExecutor executor) { //@1
        //创建一个事件广播器
        SimpleApplicationEventMulticaster result = new SimpleApplicationEventMulticaster();
        //设置异步执行器,来完成异步执行监听事件这样会导致所有的监听器都异步执行
        result.setTaskExecutor(executor);
        return result;
    }
}
