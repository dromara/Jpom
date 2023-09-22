package org.dromara.jpom.transport.protocol;

import org.dromara.jpom.transport.netty.enums.MessageCmd;

import java.util.Map;

/**
 * 消息实体接口
 *
 * @author Hong
 * @since 2023/09/21
**/
public interface Message {

    /**
     * 消息ID
     * @return 消息ID
     */
    String messageId();

    /**
     * 消息指令
     * @return 消息指令
     */
    MessageCmd cmd();

    /**
     * 消息头
     * @return 消息头
     */
    Map<String, String> header();

    /**
     * 消息体
     * @return 消息体
     */
    byte[] content();

}
