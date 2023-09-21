package org.dromara.jpom.transport.netty.websocket.codec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.netty.codec.MapToMessage;

import java.util.List;
import java.util.Map;

/**
 * 文本消息解码器
 *
 * @author Hong
 * @since 2023/08/22
 */
@Slf4j
public class TextMessageDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {

    private static final ObjectMapper json = new ObjectMapper();

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame webSocketFrame, List<Object> list) throws Exception {
        try {
            String text = webSocketFrame.text();
            list.add(MapToMessage.toMessage(getData(text)));
        } catch (Exception e) {
            log.error("消息解析失败，" + e.getMessage());
        }
    }

    private Map<String, Object> getData(String text) throws JsonProcessingException {
        return json.readValue(text, Map.class);
    }
}
