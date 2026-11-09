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
 * <p>装饰器缓存只在会话关闭时清理，若关闭与发送并发（例如文件监听 Tailer 在会话销毁后
 * 继续发送），旧的 computeIfAbsent 会把已关闭会话重新放回缓存且不再有第二次关闭回调，
 * 装饰器（含 5MB 消息缓冲）会一直常驻。这里改为：
 * <ul>
 *     <li>计算缓存时检查会话状态，已关闭直接返回 null 不缓存；</li>
 *     <li>发送失败立即按“键+实例”精确移除；</li>
 *     <li>发送前再确认一次装饰器状态，避免向已关闭会话写入。</li>
 * </ul>
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
            SOCKET_MAP.remove(session.getId());
            return false;
        }
        String sessionId = session.getId();
        WebSocketSession webSocketSession = SOCKET_MAP.compute(sessionId, (key, oldSession) -> {
            if (!session.isOpen()) {
                // 构造缓存期间会话被关闭，不写入缓存
                return null;
            }
            if (oldSession != null && oldSession.isOpen()) {
                return oldSession;
            }
            return new ConcurrentWebSocketSessionDecorator(session, 60 * 1000, (int) DataSize.ofMegabytes(5).toBytes());
        });
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            SOCKET_MAP.remove(sessionId, webSocketSession);
            return false;
        }
        try {
            webSocketSession.sendMessage(message);
            return true;
        } catch (IOException e) {
            // 发送失败说明连接已不可用，及时移除装饰器缓存，避免 5MB 缓冲随会话常驻
            SOCKET_MAP.remove(sessionId, webSocketSession);
            throw e;
        }
    }

    public static void close(WebSocketSession session) {
        SOCKET_MAP.remove(session.getId());
    }
}
