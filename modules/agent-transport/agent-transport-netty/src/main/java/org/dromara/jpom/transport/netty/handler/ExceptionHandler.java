package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Nett异常
 *
 * @author Hong
 * @since 2023/08/22
 */
@Slf4j
public class ExceptionHandler extends ChannelDuplexHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("失败", cause);
    }
}
