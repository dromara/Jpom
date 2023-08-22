package org.dromara.jpom.transport.netty.websocket.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.dromara.jpom.transport.netty.codec.MessageToByteBuf;
import org.dromara.jpom.transport.protocol.Message;

import java.util.List;

/**
 * 消息编码器-转二进制
 *
 * @author Hong
 * @since 2023/08/22
 */
public class MessageEncoder extends MessageToMessageEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message, List<Object> list) throws Exception {
        list.add(new BinaryWebSocketFrame(MessageToByteBuf.toByteBuf(message)));
    }
}
