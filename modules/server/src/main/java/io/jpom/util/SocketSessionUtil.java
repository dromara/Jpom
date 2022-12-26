/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * socket 会话对象
 *
 * @author jiangzeyin
 * @since 2018/9/29
 */
@Slf4j
public class SocketSessionUtil {

    private static final Map<String, WebSocketSession> SOCKET_MAP = new ConcurrentHashMap<>();

    public static void send(WebSocketSession session, String msg) throws IOException {
        send(session, new TextMessage(msg));
    }

    public static void send(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        if (!session.isOpen()) {
            // 会话关闭不能发送消息 @author jzy 21-08-04
            log.warn("回话已经关闭啦，不能发送消息：{}", message.getPayload());
            return;
        }
        WebSocketSession webSocketSession = SOCKET_MAP.computeIfAbsent(session.getId(), s -> new ConcurrentWebSocketSessionDecorator(session, 60 * 1000, (int) DataSize.ofMegabytes(5).toBytes()));
        webSocketSession.sendMessage(message);
    }

    public static void close(WebSocketSession session) {
        SOCKET_MAP.remove(session.getId());
    }
}
