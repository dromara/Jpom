package org.dromara.jpom.transport.event;

import lombok.Getter;
import org.dromara.jpom.transport.netty.service.ChannelService;
import org.dromara.jpom.transport.protocol.Message;
import org.springframework.context.ApplicationEvent;

/**
 * 消息事件
 *
 * @author Hong
 * @since 2023/08/22
 */
@Getter
public class MessageEvent<T extends Message> extends ApplicationEvent {

    private final String channelName;

    private final String messageId;

    private final T message;

    private final ChannelService channelService;

    public MessageEvent(Object source, T message, ChannelService channelService, String channelName) {
        super(source);
        this.messageId = message.messageId();
        this.message = message;
        this.channelService = channelService;
        this.channelName = channelName;
    }

}
