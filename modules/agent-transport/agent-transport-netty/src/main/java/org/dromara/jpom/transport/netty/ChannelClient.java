package org.dromara.jpom.transport.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;

/**
 * 通道客户端-连接
 *
 * @author Hong
 * @since 2023/08/22
 */
public interface ChannelClient {

    void doConnect(Bootstrap bootstrap, EventLoopGroup eventLoopGroup);
}
