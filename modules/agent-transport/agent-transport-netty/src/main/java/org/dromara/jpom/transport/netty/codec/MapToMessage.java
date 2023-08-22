package org.dromara.jpom.transport.netty.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.dromara.jpom.transport.protocol.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Map数据转成Message
 *
 * @author Hong
 * @since 2023/08/22
 */
public class MapToMessage {

    public static Message toMessage(Map<String, Object> map) throws JsonProcessingException {
        // 读取前4个字节，表示CMD
        int cmd = (Integer) map.get("cmd");
        // 读取前4-8个字节，表示Header长度
        Map<String, String> headerMap = (Map<String, String>) map.get("header");
        Object content = map.get("content");

        return ConvertMessage.convert(cmd, headerMap, (content == null ? new byte[0] : String.valueOf(content).getBytes(StandardCharsets.UTF_8)));
    }
}
