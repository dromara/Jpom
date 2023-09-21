package org.dromara.jpom.transport.netty.service;

import org.dromara.jpom.transport.protocol.Message;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Netty 通道服务管理
 *
 * @author Hong
 * @since 2023/08/22
 */
@Component
public class ChannelServiceManager implements ChannelService {

    public static ChannelService INSTANCE;

    @Override
    public void writeAndFlush(Message message) {
        INSTANCE.writeAndFlush(message);
    }

    @Override
    public void writeAndFlush(Message message, String... name) {
        INSTANCE.writeAndFlush(message, name);
    }

    @Override
    public void writeAndFlushAll(Message message) {
        INSTANCE.writeAndFlushAll(message);
    }

    @Override
    public void writeAndFlush(Message message, Consumer<Message> consumer) {
        INSTANCE.writeAndFlush(message, consumer);
    }

    @Override
    public void writeAndFlush(Message message, Consumer<Message> consumer, String... name) {
        INSTANCE.writeAndFlush(message, consumer, name);
    }

    @Override
    public void writeAndFlushAll(Message message, Consumer<Message> consumer) {
        INSTANCE.writeAndFlushAll(message, consumer);
    }

    public static void setChannelService(ChannelService channelService) {
        ChannelServiceManager.INSTANCE = channelService;
    }
}
