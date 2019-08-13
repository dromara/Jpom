package cn.keepbx.jpom.plugin.netty;

import cn.hutool.core.thread.ThreadUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.keepbx.plugin.netty.NettyThread;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;

/**
 * netty 服务检测
 *
 * @author bwcx_jzy
 * @date 2019/8/12
 */
@Configuration
public class NettyServerConfig implements ApplicationListener {

    private static final String CLASS_NAME = "io.netty.bootstrap.ServerBootstrap";
    /**
     * 程序端口
     */
    @Value("${netty.port:8888}")
    private int port;

    private NettyThread nettyThread;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            try {
                Class.forName(CLASS_NAME);
            } catch (ClassNotFoundException e) {
                return;
            }
            if (port <= 0) {
                DefaultSystemLog.LOG().info("端口配置错误：" + port);
                return;
            }
            nettyThread = new NettyThread(port);
            ThreadUtil.execute(nettyThread);
            return;
        }
        if (event instanceof ContextClosedEvent) {
            if (nettyThread != null) {
                DefaultSystemLog.LOG().info("关闭netty");
                try {
                    nettyThread.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
