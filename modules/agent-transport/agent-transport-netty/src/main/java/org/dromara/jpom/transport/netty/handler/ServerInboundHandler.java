package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.dromara.jpom.transport.netty.service.NettyProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端-新的客户端连接/断开
 *
 * @author Hong
 * @since 2023/08/22
 */
public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ServerInboundHandler.class);

    /**
     * 创建ChannelGroup对象存储所有连接的用户
     */

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyProducer.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NettyProducer.remove(ctx.channel());
    }
}
