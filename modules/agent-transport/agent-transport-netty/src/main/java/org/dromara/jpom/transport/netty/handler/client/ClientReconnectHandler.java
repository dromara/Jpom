package org.dromara.jpom.transport.netty.handler.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.netty.ChannelClient;
import org.dromara.jpom.transport.netty.service.NettyCustomer;

/**
 * Netty重连
 *
 * @author Hong
 * @since 2023/08/22
 */
@Slf4j
public class ClientReconnectHandler extends ChannelInboundHandlerAdapter {


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
