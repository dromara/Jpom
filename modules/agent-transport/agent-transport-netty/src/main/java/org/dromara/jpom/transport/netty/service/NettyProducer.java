package org.dromara.jpom.transport.netty.service;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.dromara.jpom.transport.netty.ChannelSupport;
import org.dromara.jpom.transport.protocol.Message;
import org.dromara.jpom.transport.protocol.RegisterMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Netty消费者/客户端
 *
 * @author Hong
 * @since 2023/08/22
 */
public class NettyProducer implements ChannelService {

    private static final Logger log = LoggerFactory.getLogger(NettyProducer.class);

    private static final ChannelGroup DEFAULT_CLIENT = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static final List<JpomChannel> customer = new CopyOnWriteArrayList<>();

    public static final ChannelService INSTANCE;

    static {
        INSTANCE = new NettyProducer();
    }

    public static void add(Channel channel) {
        DEFAULT_CLIENT.add(channel);
    }

    public static void remove(Channel channel) {
        DEFAULT_CLIENT.remove(channel);
        if (channel.isActive()) {
            channel.close();
        }
        for (JpomChannel channelCustomer : customer) {
            channelCustomer.removeChannel(channel);;
        }
    }

    public static void write(Message message, String ...name) {
        Optional<JpomChannel> optional = customer.stream().filter(it -> name != null && Arrays.stream(name).anyMatch(it::support)).findFirst();
        if (optional.isPresent()) {
            boolean flag = optional.get().write(message);
            log.info("写入消息下发队列状态：{}", flag);
        }
    }

    public static void writeAll(Message message) {
        for (JpomChannel jpomChannel : customer) {
            boolean flag = jpomChannel.write(message);
            log.info("写入消息下发队列状态：{}", flag);
        }
    }

    public static void register(RegisterMessage message, Channel channel) {
        Optional<JpomChannel> optional = customer.stream().filter(it -> ((ChannelSupport)it).support(message.getName())).findFirst();
        if (optional.isPresent()) {
            optional.get().addChannel(message, channel);
        } else {
            JpomChannel channelCustomer = new JpomChannel(message.getName());
            channelCustomer.addChannel(message, channel);
            customer.add(channelCustomer);
        }
        DEFAULT_CLIENT.remove(channel);
    }

    public static String getName(Channel channel) {
        for (JpomChannel jpomChannel : customer) {
            return jpomChannel.getName(channel);
        }
        return null;
    }

    @Override
    public void writeAndFlush(Message message) {
        write(message, "");
    }

    @Override
    public void writeAndFlush(Message message, String... name) {
        write(message, name);
    }

    @Override
    public void writeAndFlushAll(Message message) {
        writeAll(message);
    }
}
