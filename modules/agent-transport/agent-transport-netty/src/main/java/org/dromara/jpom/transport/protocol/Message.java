package org.dromara.jpom.transport.protocol;

import org.dromara.jpom.transport.netty.enums.MessageCmd;

import java.util.Map;

public interface Message {

    /** 消息ID **/
    String messageId();

    /** 消息指令 **/
    MessageCmd cmd();

    /** 消息头 **/
    Map<String, String> header();

    /** 消息体 **/
    byte[] content();

}
