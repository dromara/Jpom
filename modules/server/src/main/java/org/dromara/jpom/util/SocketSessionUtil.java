/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.map.SafeConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;

/**
 * socket 会话对象
 *
 * @author bwcx_jzy
 * @since 2018/9/29
 */
@Slf4j
public class SocketSessionUtil {

    private static final Map<String, WebSocketSession> SOCKET_MAP = new SafeConcurrentHashMap<>();

    /**
     * 发送文本消息
     *
     * @param session 会话
     * @param msg     消息
     * @return 是否发送成功
     * @throws IOException io
     */
    public static boolean send(WebSocketSession session, String msg) throws IOException {
        return send(session, new TextMessage(msg));
    }

    /**
     * 发送消息
     *
     * @param session 会话
     * @param message 消息
     * @return 是否发送成功
     * @throws IOException io
     */
    public static boolean send(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        if (!session.isOpen()) {
            // 会话关闭不能发送消息 @author jzy 21-08-04
            log.warn(I18nMessageUtil.get("i18n.session_already_closed.8dcc"), message.getPayload());
            return false;
        }
        WebSocketSession webSocketSession = SOCKET_MAP.computeIfAbsent(session.getId(), s -> new ConcurrentWebSocketSessionDecorator(session, 60 * 1000, (int) DataSize.ofMegabytes(5).toBytes()));
        webSocketSession.sendMessage(message);
        return true;
    }

    public static void close(WebSocketSession session) {
        SOCKET_MAP.remove(session.getId());
    }
}
