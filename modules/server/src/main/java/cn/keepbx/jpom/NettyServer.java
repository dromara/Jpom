package cn.keepbx.jpom;

import cn.hutool.core.thread.ThreadUtil;
import cn.keepbx.netty.NettyThread;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author myzf
 * @date 2019/7/12 14:34
 */
@Configuration
public class NettyServer implements ApplicationListener<ContextRefreshedEvent> {

    private static final String CLASS_NAME = "io.netty.bootstrap.ServerBootstrap";

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        try {
            Class.forName(CLASS_NAME);
        } catch (ClassNotFoundException e) {
            return;
        }
        ThreadUtil.execute(new NettyThread());
    }
}
