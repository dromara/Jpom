package org.dromara.jpom.transport.netty.socket.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.dromara.jpom.transport.netty.service.NettyCustomer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端-服务端连接成功
 *
 * @author Hong
 * @since 2023/08/22
 */
public class SocketClientInboundHandler extends ChannelInboundHandlerAdapter {


    /**
     * 创建ChannelGroup对象存储所有连接的用户
     */

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NettyCustomer.add(ctx.channel());
    }
}
