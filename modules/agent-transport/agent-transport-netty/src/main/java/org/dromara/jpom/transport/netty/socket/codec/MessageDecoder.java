package org.dromara.jpom.transport.netty.socket.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.dromara.jpom.transport.netty.codec.ByteBufToMessage;
import org.dromara.jpom.transport.protocol.Message;

import java.util.List;

/**
 * Netty 消息解码器
 *
 * @author Hong
 * @since 2023/08/22
 */
public class MessageDecoder extends ByteToMessageDecoder {

    private int length = 0;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (byteBuf.readableBytes() < 8) {
            return;
        }
        if (length == 0) {
            length = byteBuf.readInt();
        }
        if (length <= byteBuf.readableBytes()) {
            Message message = ByteBufToMessage.toMessage(byteBuf);
            list.add(message);
            length = 0;
        }
    }
}
