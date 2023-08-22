package org.dromara.jpom.transport.netty;

import io.netty.channel.ChannelHandler;

/**
 * 初始化客户端通道
 *
 * @author Hong
 * @since 2023/08/22
 */
public interface ChannelInitializerClient extends ChannelHandler {

    void setChannelClient(ChannelClient channelClient);

    boolean support(String tcp);

    void sync() throws InterruptedException;
}
