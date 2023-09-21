package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.netty.service.NettyProducer;

/**
 * 服务端-新的客户端连接/断开
 *
 * @author Hong
 * @since 2023/08/22
 */
@Slf4j
public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

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
