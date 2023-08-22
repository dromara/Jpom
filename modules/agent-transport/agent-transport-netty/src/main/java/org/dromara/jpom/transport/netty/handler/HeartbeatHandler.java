package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.dromara.jpom.transport.protocol.HeartbeatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty心跳
 *
 * @author Hong
 * @since 2023/08/22
 */
public class HeartbeatHandler extends SimpleChannelInboundHandler<HeartbeatMessage> {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatHandler.class);

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
