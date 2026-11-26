/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.transport.TransportUserAgent;
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 插件端双向流处理器：把隧道 {@code OPEN} 还原成一次本机 WebSocket 连接（插件端自己的 {@code /socket} 端点）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class LocalWebSocketTunnelHandler implements TunnelStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(LocalWebSocketTunnelHandler.class);

    private final String baseWsUrl;
    private final java.util.function.Supplier<Map<String, String>> fixedQuery;
    private final int timeoutSeconds;
    private final StandardWebSocketClient webSocketClient = TunnelWsContainer.createClient();

    public LocalWebSocketTunnelHandler(String baseWsUrl, Map<String, String> fixedQuery, int timeoutSeconds) {
        this(baseWsUrl, () -> fixedQuery, timeoutSeconds);
    }

    public LocalWebSocketTunnelHandler(String baseWsUrl, java.util.function.Supplier<Map<String, String>> fixedQuery, int timeoutSeconds) {
        this.baseWsUrl = StrUtil.removeSuffix(baseWsUrl, "/");
        this.fixedQuery = fixedQuery == null ? LinkedHashMap::new : fixedQuery;
        this.timeoutSeconds = Math.max(timeoutSeconds, 1);
    }

    @Override
    public void open(TunnelFrame open, TunnelStream stream) throws Exception {
        String path = StrUtil.emptyToDefault(open.getPath(), "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        Map<String, String> configured = this.fixedQuery.get();
        Map<String, String> query = configured == null ? new LinkedHashMap<>() : new LinkedHashMap<>(configured);
        if (open.getQuery() != null) {
            query.putAll(open.getQuery());
        }
        URI uri = URI.create(this.baseWsUrl + path + buildQuery(query));
        LocalSessionHandler handler = new LocalSessionHandler(stream);
        WebSocketSession session = this.webSocketClient.doHandshake(handler, TunnelClient.wsHeaders(), uri)
            .get(timeoutSeconds, TimeUnit.SECONDS);
        handler.attach(session);
        stream.onData(handler::sendToLocal);
        stream.onClose(code -> handler.closeLocal());
    }

    private static String buildQuery(Map<String, String> query) {
        if (query == null || query.isEmpty()) {
            return StrUtil.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            if (builder.length() == 0) {
                builder.append('?');
            } else {
                builder.append('&');
            }
            builder.append(entry.getKey()).append('=');
            if (entry.getValue() != null) {
                try {
                    // JDK8 只有 String 版本的重载（Charset 版本是 JDK10+）
                    builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
                } catch (java.io.UnsupportedEncodingException e) {
                    throw new IllegalStateException("查询参数编码失败", e);
                }
            }
        }
        return builder.toString();
    }

    private class LocalSessionHandler extends AbstractWebSocketHandler {
        private final TunnelStream stream;
        private volatile WebSocketSession session;

        LocalSessionHandler(TunnelStream stream) {
            this.stream = stream;
        }

        void attach(WebSocketSession session) {
            this.session = session;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            stream.write(message.getPayload().getBytes(StandardCharsets.UTF_8));
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            byte[] payload = new byte[message.getPayloadLength()];
            message.getPayload().get(payload);
            stream.write(payload);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.debug("本机 WebSocket 传输异常", exception);
            stream.close(1011, "本机传输异常");
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            stream.close(status.getCode(), StrUtil.emptyToDefault(status.getReason(), "本机关闭"));
        }

        void sendToLocal(byte[] payload) {
            WebSocketSession current = this.session;
            if (current == null || !current.isOpen() || payload == null) {
                return;
            }
            try {
                current.sendMessage(new TextMessage(new String(payload, StandardCharsets.UTF_8)));
            } catch (Exception e) {
                log.debug("写入本机 WebSocket 失败", e);
                stream.close(1011, "写入本机失败");
            }
        }

        void closeLocal() {
            WebSocketSession current = this.session;
            if (current != null && current.isOpen()) {
                try {
                    current.close(CloseStatus.NORMAL);
                } catch (Exception e) {
                    log.debug("关闭本机 WebSocket 异常", e);
                }
            }
        }
    }
}
