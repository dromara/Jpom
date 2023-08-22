package org.dromara.jpom.transport.netty.websocket.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.dromara.jpom.transport.netty.codec.ByteBufToMessage;
import org.dromara.jpom.transport.protocol.Message;

import java.util.List;

/**
 * 二级制消息解码器
 *
 * @author Hong
 * @since 2023/08/22
 */
public class BinaryMessageDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, BinaryWebSocketFrame webSocketFrame, List<Object> list) throws Exception {
        ByteBuf byteBuf = webSocketFrame.content();
        byteBuf.readInt(); // 读取总包长度
        Message message = ByteBufToMessage.toMessage(byteBuf);
        list.add(message);
    }
}
