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

import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

/**
 * WebSocket 容器参数。
 * <p>
 * JSR-356 的默认消息缓冲只有 8&nbsp;KB，而隧道数据帧上限是 256&nbsp;KiB，
 * 不做统一放大时会出现“收不到数据帧”或“发送缓冲不足”的问题（服务端与插件端都要设置）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public final class TunnelWsContainer {

    /**
     * 单条 WebSocket 消息缓冲（1 MiB，覆盖 256 KiB 数据帧 + 协议头）
     */
    public static final int MESSAGE_BUFFER_BYTES = 1024 * 1024;

    /**
     * 发送超时（毫秒）
     */
    public static final long SEND_TIMEOUT_MILLIS = 60 * 1000L;

    private TunnelWsContainer() {
    }

    /**
     * 创建已放大缓冲区的 WebSocket 客户端（插件端使用）
     *
     * @return 客户端
     */
    public static StandardWebSocketClient createClient() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        if (container != null) {
            container.setDefaultMaxBinaryMessageBufferSize(MESSAGE_BUFFER_BYTES);
            container.setDefaultMaxTextMessageBufferSize(MESSAGE_BUFFER_BYTES);
            container.setAsyncSendTimeout(SEND_TIMEOUT_MILLIS);
        }
        return new StandardWebSocketClient(container);
    }
}
