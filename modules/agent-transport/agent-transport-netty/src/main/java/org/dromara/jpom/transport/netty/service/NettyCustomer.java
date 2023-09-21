package org.dromara.jpom.transport.netty.service;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.dromara.jpom.transport.MessageSubscribers;
import org.dromara.jpom.transport.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Netty服务端通道
 *
 * @author Hong
 * @since 2023/08/22
 */
public class NettyCustomer implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(NettyCustomer.class);

    private static final ChannelGroup DEFAULT_CLIENT = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static final ChannelService INSTANCE;

    static {
        INSTANCE = new NettyCustomer();
    }

    public static void add(Channel channel) {
        DEFAULT_CLIENT.add(channel);
    }

    public static void remove(Channel channel) {
        DEFAULT_CLIENT.remove(channel);
        if (channel.isActive()) {
            channel.close();
        }
    }

    public static boolean write(Message message) {
        log.info("写入消息下发队列");
        return DEFAULT_CLIENT.writeAndFlush(message).isSuccess();
    }

    public static ChannelGroup getDefaultClient() {
        return DEFAULT_CLIENT;
    }

    @Override
    public void writeAndFlush(Message message) {
        write(message);
    }

    @Override
    public void writeAndFlush(Message message, String... name) {
        write(message);
    }

    @Override
    public void writeAndFlushAll(Message message) {
        write(message);
    }

    @Override
    public void writeAndFlush(Message message, Consumer<Message> consumer) {
        if (write(message)) {
            MessageSubscribers.addConsumer(message.messageId(), consumer);
        }
    }

    @Override
    public void writeAndFlush(Message message, Consumer<Message> consumer, String... name) {
        if (write(message)) {
            MessageSubscribers.addConsumer(message.messageId(), consumer);
        }
    }

    @Override
    public void writeAndFlushAll(Message message, Consumer<Message> consumer) {
        if (write(message)) {
            MessageSubscribers.addConsumer(message.messageId(), consumer);
        }
    }
}
