package org.dromara.jpom.transport;

import org.dromara.jpom.transport.protocol.Message;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 消息订阅器
 *
 * @author Hong
 * @since 2023/09/21
 */
public class MessageSubscribers {

    private final static Map<String, Consumer<Message>> MAP = new ConcurrentHashMap<>();

    /**
     * 添加消费者
     *
     * @param messageId 消息ID
     * @param consumer  消费者
     */
    public static void addConsumer(String messageId, Consumer<Message> consumer) {
        MAP.put(messageId, consumer);
    }

    /**
     * 添加消费者
     *
     * @param messageId 消息ID
     */
    public static void removeConsumer(String messageId) {
        MAP.remove(messageId);
    }

    /**
     * 开始消费
     *
     * @param message 消息
     */
    public static void startConsume(Message message) {
        if (MAP.containsKey(message.messageId())) {
            try {
                MAP.get(message.messageId()).accept(message);
            } finally {
                MAP.remove(message.messageId());
            }
        }
    }
}
