package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.protocol.HeartbeatMessage;

/**
 * Netty心跳
 *
 * @author Hong
 * @since 2023/08/22
 */
@Slf4j
public class HeartbeatHandler extends SimpleChannelInboundHandler<HeartbeatMessage> {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()) {
                case READER_IDLE:
                    ctx.writeAndFlush(new HeartbeatMessage());
                    log.debug("读空闲");
                    break;
                case WRITER_IDLE:
                    ctx.writeAndFlush(new HeartbeatMessage());
                    log.debug("写空闲");
                    break;
                case ALL_IDLE:
                    log.debug("读写空闲");
                    break;
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, HeartbeatMessage heartbeatMessage) throws Exception {
        log.debug("收到心跳");
    }
}
