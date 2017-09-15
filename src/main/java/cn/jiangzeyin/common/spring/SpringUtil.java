package cn.jiangzeyin.common.spring;

import cn.jiangzeyin.system.log.LogType;
import cn.jiangzeyin.system.log.SystemLog;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.util.Assert;
import org.springframework.web.context.support.ServletRequestHandledEvent;

/**
 * @author jiangzeyin
 * Created by jiangzeyin on 2017/1/5.
 */
@Configuration
public class SpringUtil implements ApplicationListener, ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * 容器加载完成
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.applicationContext = applicationContext;
        //SystemLog.init();
    }

    /**
     * 启动完成
     *
     * @param event
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 初始化页面模板
        if (event instanceof ApplicationEnvironmentPreparedEvent) { // 初始化环境变量
            //System.out.println("0");
        } else if (event instanceof ApplicationPreparedEvent) { // 初始化完成
            //System.out.println("1");
        } else if (event instanceof ContextRefreshedEvent) { // 应用刷新
        } else if (event instanceof ApplicationReadyEvent) {// 应用已启动完成
            SystemLog.LOG().info(" 启动完成");
        } else if (event instanceof ContextStartedEvent) { // 应用启动，需要在代码动态添加监听器才可捕获
            System.out.println("3");
        } else if (event instanceof ContextStoppedEvent) { // 应用停止
            System.out.println("stop");
        } else if (event instanceof ContextClosedEvent) { // 应用关闭
            SystemLog.LOG().info("关闭程序");
        } else if (event instanceof ServletRequestHandledEvent) {
            ServletRequestHandledEvent servletRequestHandledEvent = (ServletRequestHandledEvent) event;
            if ("/favicon.ico".equals(servletRequestHandledEvent.getRequestUrl()))
                return;
            int code = servletRequestHandledEvent.getStatusCode();
            if (servletRequestHandledEvent.wasFailure()) {
                SystemLog.LOG(LogType.REQUEST).info("code:" + code + " error:" + servletRequestHandledEvent.toString());
            } else if (code != 200) {
                SystemLog.LOG(LogType.REQUEST).info("code:" + code + " " + servletRequestHandledEvent.toString());
            }
        } else if (event instanceof EmbeddedServletContainerInitializedEvent) {

        } else {
            System.out.println("else");
            System.out.println(event);
        }
    }

    /**
     * 获取applicationContext
     *
     * @return
     */
    public static ApplicationContext getApplicationContext() {
        Assert.notNull(applicationContext, "application is null");
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     *
     * @param name
     * @return
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);

    }

    /**
     * 通过class获取Bean.
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     *
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }


}
