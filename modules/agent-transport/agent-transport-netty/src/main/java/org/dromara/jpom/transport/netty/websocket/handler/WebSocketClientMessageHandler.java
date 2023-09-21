package org.dromara.jpom.transport.netty.websocket.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.dromara.jpom.transport.netty.service.NettyCustomer;

/**
 * WebSocket 协议握手处理
 *
 * @author Hong
 * @since 2023/08/22
 */
@ChannelHandler.Sharable
public class WebSocketClientMessageHandler extends SimpleChannelInboundHandler<Object> {


    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public WebSocketClientMessageHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        handshaker.close(ctx.channel(), new CloseWebSocketFrame());
        NettyCustomer.remove(ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) throws Exception {
        // 处理握手
        if (!handshaker.isHandshakeComplete()) {
            try {
                this.handshaker.finishHandshake(channelHandlerContext.channel(), (FullHttpResponse) message);
                this.handshakeFuture.setSuccess();
            } catch (Exception e) {
                this.handshakeFuture.setFailure(e);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
        NettyCustomer.remove(ctx.channel());
    }

    public ChannelPromise getHandshakeFuture() {
        return handshakeFuture;
    }
}
