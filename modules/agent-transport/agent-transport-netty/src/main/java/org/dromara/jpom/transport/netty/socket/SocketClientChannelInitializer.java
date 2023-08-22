package org.dromara.jpom.transport.netty.socket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.dromara.jpom.transport.netty.ChannelClient;
import org.dromara.jpom.transport.netty.ChannelInitializerClient;
import org.dromara.jpom.transport.netty.handler.ExceptionHandler;
import org.dromara.jpom.transport.netty.handler.HeartbeatHandler;
import org.dromara.jpom.transport.netty.handler.MessageHandler;
import org.dromara.jpom.transport.netty.handler.client.ClientReconnectHandler;
import org.dromara.jpom.transport.netty.socket.codec.MessageDecoder;
import org.dromara.jpom.transport.netty.socket.codec.MessageEncoder;
import org.dromara.jpom.transport.netty.socket.handler.SocketClientInboundHandler;
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
public class SocketClientChannelInitializer extends ChannelInitializer<SocketChannel> implements ChannelInitializerClient {

    private static final String tcp = "socket";
    private ChannelClient channelClient;

    private ApplicationEventMulticaster eventMulticaster;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline
                .addLast(new MessageDecoder())
                .addLast(new MessageEncoder())
                .addLast(new SocketClientInboundHandler())
                .addLast(new MessageHandler(eventMulticaster))
                .addLast(new IdleStateHandler(60, 60, 0, TimeUnit.SECONDS))
                .addLast(new HeartbeatHandler())
                .addLast(new ClientReconnectHandler(channelClient))
                .addLast(new ExceptionHandler());
    }

    @Autowired
    public void setEventMulticaster(ApplicationEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
    }

    @Override
    public void setChannelClient(ChannelClient channelClient) {
        this.channelClient = channelClient;
    }

    @Override
    public boolean support(String tcp) {
        return SocketClientChannelInitializer.tcp.equals(tcp);
    }

    @Override
    public void sync() {

    }
}
