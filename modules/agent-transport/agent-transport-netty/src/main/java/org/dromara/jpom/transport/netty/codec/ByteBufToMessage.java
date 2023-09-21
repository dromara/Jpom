package org.dromara.jpom.transport.netty.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.dromara.jpom.transport.protocol.*;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
/**
 * ByteBuf转Message
 *
 * @author Hong
 * @since 2023/08/22
 */
public class ByteBufToMessage {

    private static final ObjectMapper json = new ObjectMapper();

    public static Message toMessage(ByteBuf byteBuf) throws JsonProcessingException {
        // 读取前4个字节，表示CMD
        int cmd = byteBuf.readInt();
        // 读取前4-8个字节，表示Header长度
        int headerLength = byteBuf.readInt();
        // 根据header长度读取Header内容
        byte[] header = new byte[headerLength];
        byteBuf.readBytes(header);
        // 读取header内容后4个字节，表示内容长度
        int bodyLength = byteBuf.readInt();
        // 根据内容长度读取本地消息内容
        byte[] content = new byte[bodyLength];
        byteBuf.readBytes(content);
        Map<String, String> headerMap = getHeader(header);
        return ConvertMessage.convert(cmd, headerMap, content);
    }

    private static Map<String, String> getHeader(byte[] bytes) {
        String header = new String(bytes, StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        if (StringUtils.hasText(header)) {
            Arrays.stream(header.split("\n")).map(it -> it.split("=")).forEach(it -> map.put(it[0], it[1]));
        }
        return map;
    }
}
