package org.dromara.jpom.transport.netty.socket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.dromara.jpom.transport.netty.ChannelInitializerServer;
import org.dromara.jpom.transport.netty.handler.ExceptionHandler;
import org.dromara.jpom.transport.netty.handler.HeartbeatHandler;
import org.dromara.jpom.transport.netty.handler.MessageHandler;
import org.dromara.jpom.transport.netty.socket.codec.MessageDecoder;
import org.dromara.jpom.transport.netty.socket.codec.MessageEncoder;
import org.dromara.jpom.transport.netty.handler.ServerInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 初始化通道 Socket
 *
 * @author Hong
 * @since 2023/08/22
 */
@Component
public class SocketServerChannelInitializerInitializer extends ChannelInitializer<SocketChannel> implements ChannelInitializerServer {

    private static final String tcp = "socket";
    private ApplicationEventMulticaster eventMulticaster;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline
                .addLast(new MessageDecoder())
                .addLast(new MessageEncoder())
                .addLast(new ServerInboundHandler())
                .addLast(new MessageHandler(eventMulticaster))
                .addLast(new IdleStateHandler(90, 90, 0, TimeUnit.SECONDS))
                .addLast(new HeartbeatHandler())
                .addLast(new ExceptionHandler());
    }

    @Autowired
    public void setEventMulticaster(ApplicationEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
    }

    @Override
    public boolean support(String tcp) {
        return SocketServerChannelInitializerInitializer.tcp.equals(tcp);
    }

}
