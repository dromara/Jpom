package org.dromara.jpom.transport.netty.handler.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.dromara.jpom.transport.netty.ChannelClient;
import org.dromara.jpom.transport.netty.service.NettyCustomer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty重连
 *
 * @author Hong
 * @since 2023/08/22
 */
public class ClientReconnectHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ClientReconnectHandler.class);

    private final ChannelClient channelClient;

    public ClientReconnectHandler(ChannelClient channelClient) {
        this.channelClient = channelClient;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyCustomer.remove(ctx.channel());
        log.info("Disconnected from: " + ctx.channel().remoteAddress());
        channelClient.doConnect(new Bootstrap(), ctx.channel().eventLoop());
    }

}
