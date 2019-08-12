package cn.keepbx.jpom;

import cn.hutool.core.thread.ThreadUtil;
import cn.keepbx.netty.NettyThread;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * @author myzf
 * @date 2019/7/12 14:34
 */
@Service
public class HttpFileServer implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        ThreadUtil.execute(new NettyThread());
    }
}
