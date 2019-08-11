package cn.keepbx.jpom.common.download;


import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * @package cn.myzf.gate.monitor
 * @Date Created in 2019/7/12 14:34
 * @Author myzf
 */
@Service
public class HttpFileServer implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        new NettyThread().start();
        }
}
