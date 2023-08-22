package org.dromara.jpom.transport.netty.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.dromara.jpom.transport.properties.WebSocketProperties;
import org.dromara.jpom.transport.netty.ChannelInitializerServer;
import org.dromara.jpom.transport.netty.handler.ExceptionHandler;
import org.dromara.jpom.transport.netty.handler.HeartbeatHandler;
import org.dromara.jpom.transport.netty.handler.MessageHandler;
import org.dromara.jpom.transport.netty.handler.ServerInboundHandler;
import org.dromara.jpom.transport.netty.websocket.codec.BinaryMessageDecoder;
import org.dromara.jpom.transport.netty.websocket.codec.MessageEncoder;
import org.dromara.jpom.transport.netty.websocket.codec.TextMessageDecoder;
import org.dromara.jpom.transport.netty.websocket.handler.WebSocketServerMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 初始化通道 WebSocket
 *
 * @author Hong
 * @since 2023/08/22
 */
@Component
public class WebSocketServerChannelInitializerInitializer extends ChannelInitializer<SocketChannel> implements ChannelInitializerServer {

    private static final String tcp = "websocket";

    private WebSocketProperties webSocketProperties;
    private ApplicationEventMulticaster eventMulticaster;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline
                //添加HTTP编码解码器
                .addLast(new HttpServerCodec())
                //添加对大数据流的支持
                .addLast(new ChunkedWriteHandler())
                //添加聚合器
                .addLast(new HttpObjectAggregator(1024 * 64))
                .addLast(new MessageEncoder())
                .addLast(new TextMessageDecoder())
                .addLast(new BinaryMessageDecoder())
                .addLast(new ServerInboundHandler())
                .addLast(new MessageHandler(eventMulticaster))
                .addLast(new IdleStateHandler(90, 90, 0, TimeUnit.SECONDS))
                .addLast(new HeartbeatHandler())
                .addLast(new WebSocketServerMessageHandler(webSocketProperties))
                .addLast(new ExceptionHandler());
    }

    @Autowired
    public void setEventMulticaster(ApplicationEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
    }

    @Override
    public boolean support(String tcp) {
        return WebSocketServerChannelInitializerInitializer.tcp.equals(tcp);
    }

    @Autowired
    public void setWebSocketProperties(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
    }
}
