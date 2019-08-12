package cn.keepbx.jpom.plugin.netty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author bwcx_jzy
 * @date 2019/8/12
 */
@Configuration
public class NettyConfig {

    /**
     * 程序端口
     */
    @Value("${netty.port}")
    private int port;
}
