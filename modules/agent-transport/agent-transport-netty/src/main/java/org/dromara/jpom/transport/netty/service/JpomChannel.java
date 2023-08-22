package org.dromara.jpom.transport.netty.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.dromara.jpom.transport.netty.ChannelSupport;
import org.dromara.jpom.transport.protocol.Message;
import org.dromara.jpom.transport.protocol.RegisterMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通道对象
 *
 * @author Hong
 * @since 2023/08/22
 */
public class JpomChannel implements ChannelSupport {

    private final String name;

    private final List<ChannelItem> channelItems = new CopyOnWriteArrayList<>();

    public JpomChannel(String name) {
        this.name = name;
    }

    public void addChannel(RegisterMessage message, Channel channel) {
        channelItems.add(new ChannelItem(message, channel));
    }

    public void removeChannel(Channel channel) {
        for (ChannelItem channelItem : channelItems) {
            if (channelItem.hit(channel)) {
                channelItems.remove(channelItem);
                return;
            }
        }
    }

    public boolean write(Message message) {
        for (ChannelItem channelItem : channelItems) {
            if (!channelItem.write) {
                return channelItem.write(message);
            }
        }
        return false;
    }

    @Override
    public boolean support(String name) {
        return this.name.equals(name);
    }

    public String getName(Channel channel) {
        for (ChannelItem channelItem : channelItems) {
            if (channelItem.hit(channel)) {
                return this.name;
            }
        }
        return null;
    }

    public static class ChannelItem {

        /**
         * 是否在写状态
         **/
        private volatile boolean write;

        private final Channel channel;
        private final Message message;

        public ChannelItem(RegisterMessage message, Channel channel) {
            this.message = message;
            this.channel = channel;
        }

        public boolean write(Message message) {
            if (channel.isActive()) {
                this.write = true;
                channel.writeAndFlush(message).addListener((ChannelFutureListener) channelFuture -> write = false);
                return true;
            }
            return false;
        }

        public boolean isWrite() {
            return write;
        }

        public void setWrite(boolean write) {
            this.write = write;
        }

        public boolean hit(Channel channel) {
            return this.channel.equals(channel);
        }
    }
}
