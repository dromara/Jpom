package org.dromara.jpom.transport.netty.service;

import org.dromara.jpom.transport.protocol.Message;

/**
 * Netty 通道服务接口
 *
 * @author Hong
 * @since 2023/08/22
 */
public interface ChannelService {

    /** 写入消息-客户端 **/
    void writeAndFlush(Message message);

    /** 根据注册名写入消息-服务端 **/
    void writeAndFlush(Message message, String... name);

    /** 向全部客户端写入消息-服务端 **/
    void writeAndFlushAll(Message message);
}
