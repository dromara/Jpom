package cn.keepbx.jpom.common;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.spring.event.ApplicationEventClient;
import cn.keepbx.jpom.model.JpomManifest;
import cn.keepbx.jpom.system.ConfigBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * 启动 、关闭监听
 *
 * @author jiangzeyin
 * @date 2019/4/7
 */
public class JpomApplicationEvent implements ApplicationEventClient {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 启动最后的预加载
        if (event instanceof ApplicationReadyEvent) {
            JpomManifest jpomManifest = JpomManifest.getInstance();
            FileUtil.writeString(jpomManifest.toString(), ConfigBean.getInstance().getPidFile(), CharsetUtil.CHARSET_UTF_8);
        }// 应用关闭
        else if (event instanceof ContextClosedEvent) {
            FileUtil.del(ConfigBean.getInstance().getPidFile());
        }
    }

    /**
     * 获取当前程序进程id
     *
     * @return pid
     */
    public static int getPid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        // format: "pid@hostname"
        String name = runtime.getName();
        try {
            return Integer.parseInt(name.substring(0, name.indexOf('@')));
        } catch (Exception e) {
            return -1;
        }
    }
}
