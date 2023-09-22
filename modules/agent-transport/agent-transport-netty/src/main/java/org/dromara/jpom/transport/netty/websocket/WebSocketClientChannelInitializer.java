package org.dromara.jpom.transport.netty.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.dromara.jpom.transport.properties.NettyProperties;
import org.dromara.jpom.transport.properties.WebSocketProperties;
import org.dromara.jpom.transport.netty.ChannelClient;
import org.dromara.jpom.transport.netty.ChannelInitializerClient;
import org.dromara.jpom.transport.netty.handler.ExceptionHandler;
import org.dromara.jpom.transport.netty.handler.HeartbeatHandler;
import org.dromara.jpom.transport.netty.handler.MessageHandler;
import org.dromara.jpom.transport.netty.handler.client.ClientReconnectHandler;
import org.dromara.jpom.transport.netty.websocket.codec.BinaryMessageDecoder;
import org.dromara.jpom.transport.netty.websocket.codec.MessageEncoder;
import org.dromara.jpom.transport.netty.websocket.codec.TextMessageDecoder;
import org.dromara.jpom.transport.netty.websocket.handler.WebSocketClientMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * 初始化通道 WebSocket
 *
 * @author Hong
 * @since 2023/08/22
 */
@Component
public class WebSocketClientChannelInitializer extends ChannelInitializer<SocketChannel> implements ChannelInitializerClient {

    private static final String tcp = "websocket";

    private NettyProperties nettyProperties;
    private WebSocketProperties webSocketProperties;
    private ChannelClient channelClient;
    private ApplicationEventMulticaster eventMulticaster;

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        URI websocketURI = new URI(webSocketProperties.getProtocol() + "://" + nettyProperties.getHost() + ":" + nettyProperties.getPort() + webSocketProperties.getPath());
        WebSocketClientMessageHandler handler = new WebSocketClientMessageHandler(WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
        pipeline
                //添加HTTP编码解码器
                .addLast(new HttpClientCodec())
                //添加对大数据流的支持
                .addLast(new ChunkedWriteHandler())
                //添加聚合器
                .addLast(new HttpObjectAggregator(1024 * 64))
                .addLast(new MessageEncoder())
                .addLast(new TextMessageDecoder())
                .addLast(new BinaryMessageDecoder())
                .addLast(new MessageHandler(eventMulticaster))
                .addLast(new IdleStateHandler(60, 60, 0, TimeUnit.SECONDS))
                .addLast(new HeartbeatHandler())
                .addLast(new ClientReconnectHandler(channelClient))
                .addLast(handler)
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
        return WebSocketClientChannelInitializer.tcp.equals(tcp);
    }

    @Override
    public void sync() throws InterruptedException {
        // handler.getHandshakeFuture().sync();
    }

    @Autowired
    public void setNettyProperties(NettyProperties nettyProperties) {
        this.nettyProperties = nettyProperties;
    }

    @Autowired
    public void setWebSocketProperties(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
    }
}
