package org.dromara.jpom.transport.netty.websocket.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import org.dromara.jpom.transport.properties.WebSocketProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

/**
 * WebDocket协议握手处理
 *
 * @author Hong
 * @since 2023/08/22
 */
@ChannelHandler.Sharable
public class WebSocketServerMessageHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServerMessageHandler.class);
    private WebSocketServerHandshaker handshaker;

    private final WebSocketProperties webSocketProperties;

    public WebSocketServerMessageHandler(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) throws Exception {
        //处理非Text和Binary的内容
        if (message instanceof FullHttpRequest) {
            handleHttpRequest(channelHandlerContext, (FullHttpRequest) message);
        } else if (message instanceof CloseWebSocketFrame) {
            channelHandlerContext.close();
        } else if (message instanceof PingWebSocketFrame) {
            channelHandlerContext.writeAndFlush(new PongWebSocketFrame());
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.protocolVersion(), BAD_REQUEST, ctx.alloc().buffer(0)));
            return;
        }

        // Allow only GET methods.
        if (!GET.equals(req.method())) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(req.protocolVersion(), FORBIDDEN, ctx.alloc().buffer(0)));
            return;
        }

        if ("/favicon.ico".equals(req.uri())) {
            FullHttpResponse res = new DefaultFullHttpResponse(req.protocolVersion(), NOT_FOUND, ctx.alloc().buffer(0));
            sendHttpResponse(ctx, req, res);
            return;
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true, 5 * 1024 * 1024);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        HttpResponseStatus responseStatus = res.status();
        if (responseStatus.code() != 200) {
            ByteBufUtil.writeUtf8(res.content(), responseStatus.toString());
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }
        // Send the response and close the connection if necessary.
        boolean keepAlive = HttpUtil.isKeepAlive(req) && responseStatus.code() == 200;
        HttpUtil.setKeepAlive(res, keepAlive);
        ChannelFuture future = ctx.write(res); // Flushed in channelReadComplete()
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HttpHeaderNames.HOST) + webSocketProperties.getPath();
        return webSocketProperties.getProtocol() + "://" + location;
    }
}
