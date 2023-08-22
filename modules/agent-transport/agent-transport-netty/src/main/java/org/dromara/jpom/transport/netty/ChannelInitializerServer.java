package org.dromara.jpom.transport.netty;

import io.netty.channel.ChannelHandler;

/**
 * 初始化服务端通道
 *
 * @author Hong
 * @since 2023/08/22
 */
public interface ChannelInitializerServer extends ChannelHandler {

    boolean support(String tcp);
}
