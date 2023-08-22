package org.dromara.jpom.transport.netty.socket.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.dromara.jpom.transport.netty.codec.MessageToByteBuf;
import org.dromara.jpom.transport.protocol.Message;

/**
 * Netty 消息编码器
 *
 * @author Hong
 * @since 2023/08/22
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Message message, ByteBuf byteBuf) {
        byteBuf.writeBytes(MessageToByteBuf.toByteBuf(message));
    }

}
