package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nett异常
 *
 * @author Hong
 * @since 2023/08/22
 */
public class ExceptionHandler extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("失败", cause);
    }
}
