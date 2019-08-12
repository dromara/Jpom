package cn.keepbx.jpom.plugin.netty;

import cn.hutool.core.thread.ThreadUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.plugin.netty.NettyThread;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * netty 服务检测
 *
 * @author bwcx_jzy
 * @date 2019/8/12
 */
@Configuration
public class NettyServerConfig implements ApplicationListener<ContextRefreshedEvent> {

    private static final String CLASS_NAME = "io.netty.bootstrap.ServerBootstrap";
    /**
     * 程序端口
     */
    @Value("${netty.port:8888}")
    private int port;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            Class.forName(CLASS_NAME);
        } catch (ClassNotFoundException e) {
            return;
        }
        if (port <= 0) {
            DefaultSystemLog.LOG().info("端口配置错误：" + port);
            return;
        }
        ThreadUtil.execute(new NettyThread(port));
    }
}
