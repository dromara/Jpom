package org.dromara.jpom.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.dromara.jpom.transport.protocol.Message;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Message对象转成ByteBuf
 *
 * @author Hong
 * @since 2023/08/22
 */
public class MessageToByteBuf {

    public static ByteBuf toByteBuf(Message message) {
        ByteBuf messageBuf = Unpooled.buffer();
        messageBuf.writeInt(message.cmd().getCmd());
        message.header().put("id", message.messageId());
        byte[] header = getHeader(message.header());
        messageBuf.writeInt(header.length);
        messageBuf.writeBytes(header, 0, header.length);
        messageBuf.writeInt(message.content().length);
        messageBuf.writeBytes(message.content(), 0, message.content().length);
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(messageBuf.readableBytes());
        byteBuf.writeBytes(messageBuf);
        return byteBuf;
    }

    private static byte[] getHeader(Map<String, String> header) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : header.entrySet()) {
            if (StringUtils.hasText(entry.getValue())) {
                builder.append("\n").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(0);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
