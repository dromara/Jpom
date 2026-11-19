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
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.transport.IUrlItem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 反向模式下的 {@link IProxyWebSocket} 实现：把服务端对插件端 {@code /socket} 的连接映射成隧道上的一条双向流。
 * <p>
 * 13 个 {@code /socket} handler 只依赖 {@link IProxyWebSocket}，因此不需要任何改动。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelProxyWebSocket implements IProxyWebSocket {

    private final TunnelChannel channel;
    private final IUrlItem urlItem;
    private final Map<String, String> query;
    private final int timeout;

    private volatile TunnelStream stream;
    private volatile Consumer<String> messageConsumer;
    private volatile String closeStatusMsg;
    private volatile boolean closed;

    public TunnelProxyWebSocket(TunnelChannel channel, IUrlItem urlItem, Map<String, String> query, int timeout) {
        this.channel = channel;
        this.urlItem = urlItem;
        this.query = query == null ? new HashMap<>() : query;
        this.timeout = timeout <= 0 ? 30 : timeout;
    }

    /**
     * 便捷构造：把 {@code parameters} 的 key/value 对转成查询参数
     *
     * @param channel    隧道通道
     * @param urlItem    请求信息
     * @param timeout    打开超时（秒）
     * @param parameters key/value 对
     * @return 代理连接
     */
    public static TunnelProxyWebSocket of(TunnelChannel channel, IUrlItem urlItem, int timeout, Object... parameters) {
        return of(channel, urlItem, timeout, null, parameters);
    }

    /**
     * 便捷构造（可附带额外查询参数，例如节点授权参数）
     *
     * @param channel     隧道通道
     * @param urlItem     请求信息
     * @param timeout     打开超时（秒）
     * @param extraQuery  额外查询参数（优先级高于 parameters）
     * @param parameters  key/value 对
     * @return 代理连接
     */
    public static TunnelProxyWebSocket of(TunnelChannel channel, IUrlItem urlItem, int timeout,
                                          Map<String, String> extraQuery, Object... parameters) {
        Map<String, String> query = new HashMap<>();
        if (extraQuery != null) {
            query.putAll(extraQuery);
        }
        if (parameters != null) {
            for (int i = 0; i + 1 < parameters.length; i += 2) {
                if (parameters[i] != null) {
                    query.put(parameters[i].toString(), parameters[i + 1] == null ? StrUtil.EMPTY : parameters[i + 1].toString());
                }
            }
        }
        return new TunnelProxyWebSocket(channel, urlItem, query, timeout);
    }

    @Override
    public boolean connect() {
        return this.connectBlocking();
    }

    @Override
    public boolean connectBlocking() {
        return this.connectBlocking(Math.max(timeout, 1));
    }

    @Override
    public boolean connectBlocking(int seconds) {
        if (closed) {
            return false;
        }
        try {
            TunnelStream tunnelStream = channel.openStream(urlItem, query, Math.max(seconds, 1));
            tunnelStream.onData(this::onData);
            tunnelStream.onClose(code -> {
                closed = true;
                closeStatusMsg = "隧道双向流已关闭，code=" + code;
            });
            this.stream = tunnelStream;
            return true;
        } catch (Exception e) {
            this.closeStatusMsg = e.getMessage();
            this.closed = true;
            return false;
        }
    }

    @Override
    public boolean reconnect() {
        try {
            this.close();
        } catch (IOException e) {
            this.closeStatusMsg = e.getMessage();
        }
        this.closed = false;
        return this.connectBlocking();
    }

    @Override
    public boolean reconnectBlocking() {
        return this.reconnect();
    }

    @Override
    public void send(String msg) {
        TunnelStream tunnelStream = this.stream;
        if (tunnelStream == null || closed) {
            return;
        }
        tunnelStream.write(msg == null ? new byte[0] : msg.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void send(ByteBuffer bytes) {
        TunnelStream tunnelStream = this.stream;
        if (tunnelStream == null || closed || bytes == null) {
            return;
        }
        byte[] payload = new byte[bytes.remaining()];
        bytes.get(payload);
        tunnelStream.write(payload);
    }

    @Override
    public void onMessage(Consumer<String> consumer) {
        this.messageConsumer = consumer;
    }

    @Override
    public boolean isConnected() {
        TunnelStream tunnelStream = this.stream;
        return !closed && tunnelStream != null && !tunnelStream.isClosed();
    }

    @Override
    public String getCloseStatusMsg() {
        return closeStatusMsg;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        TunnelStream tunnelStream = this.stream;
        if (tunnelStream != null) {
            tunnelStream.close(1000, "server closed");
        }
        this.stream = null;
    }

    private void onData(byte[] payload) {
        Consumer<String> consumer = this.messageConsumer;
        if (consumer != null && payload != null) {
            consumer.accept(new String(payload, StandardCharsets.UTF_8));
        }
    }
}
