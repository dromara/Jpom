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
import org.dromara.jpom.transport.DataContentType;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.TunnelDataFrame;
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.dromara.jpom.transport.tunnel.TunnelFrameCodec;
import org.dromara.jpom.transport.tunnel.TunnelFrameType;
import org.dromara.jpom.transport.tunnel.TunnelSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 服务端侧的一条隧道连接：把 {@code REQ/RES} 多路复用成逻辑请求，把 {@code OPEN/DATA/CLOSE} 多路复用成双向流。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelChannel implements TunnelSession {

    private static final Logger log = LoggerFactory.getLogger(TunnelChannel.class);

    /**
     * 心跳超时关闭码
     */
    public static final int CLOSE_HEARTBEAT_TIMEOUT = 4000;
    /**
     * 握手超时关闭码
     */
    public static final int CLOSE_HELLO_TIMEOUT = 4001;
    /**
     * 被新连接顶替
     */
    public static final int CLOSE_REPLACED = 4004;
    /**
     * 服务端主动关闭
     */
    public static final int CLOSE_NORMAL = 1000;

    private final WebSocketSession session;
    private final TunnelServerOptions options;
    private final String sessionId = TunnelSid.next();
    private final long establishedAt = System.currentTimeMillis();
    private final AtomicInteger missedPong = new AtomicInteger();

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final Map<String, ServerStream> streams = new ConcurrentHashMap<>();

    private volatile java.util.function.Consumer<TunnelFrame> pushHandler;

    private volatile String machineId;
    private volatile String agentVersion;
    private volatile String remoteAddress;
    private volatile boolean helloPassed;
    private volatile boolean closed;
    private volatile long lastPongAt = System.currentTimeMillis();

    public TunnelChannel(WebSocketSession session, TunnelServerOptions options) {
        this.session = session;
        this.options = options == null ? new TunnelServerOptions() : options;
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String machineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    /**
     * 绑定插件端主动推送的处理器（由端点处理器在连接建立时注册，转发给注册表内的监听器）
     *
     * @param pushHandler 处理器
     */
    public void setPushHandler(java.util.function.Consumer<TunnelFrame> pushHandler) {
        this.pushHandler = pushHandler;
    }

    public boolean isHelloPassed() {
        return helloPassed;
    }

    public void setHelloPassed(boolean helloPassed) {
        this.helloPassed = helloPassed;
    }

    public long getEstablishedAt() {
        return establishedAt;
    }

    public long getLastPongAt() {
        return lastPongAt;
    }

    @Override
    public boolean isOpen() {
        return !closed && session.isOpen();
    }

    // ---------------------------------------------------------------- 发送

    @Override
    public void sendControl(TunnelFrame frame) {
        if (!isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(TunnelFrameCodec.encodeControl(frame)));
        } catch (Exception e) {
            log.warn("隧道[{}]发送控制帧失败: {}", machineId, frame, e);
            this.close(1011, "发送失败");
        }
    }

    @Override
    public void sendData(TunnelDataFrame frame) {
        if (!isOpen()) {
            return;
        }
        try {
            byte[] raw = TunnelFrameCodec.encodeData(frame.getSid(), frame.getPayload(), frame.isFin());
            session.sendMessage(new BinaryMessage(raw));
        } catch (Exception e) {
            log.warn("隧道[{}]发送数据帧失败: {}", machineId, frame, e);
            this.close(1011, "发送失败");
        }
    }

    private void sendData(String sid, byte[] payload, boolean fin) {
        this.sendData(new TunnelDataFrame(sid, payload, fin ? TunnelDataFrame.FLAG_FIN : 0));
    }

    private void sendBody(String sid, byte[] body) {
        this.sendBody(sid, body, true);
    }

    /**
     * 发送数据帧
     *
     * @param sid     逻辑请求 / 流标识
     * @param body    数据
     * @param lastFin 最后一帧是否携带 FIN
     */
    private void sendBody(String sid, byte[] body, boolean lastFin) {
        for (byte[] frame : TunnelFrameCodec.split(sid, body, lastFin)) {
            if (!isOpen()) {
                return;
            }
            try {
                session.sendMessage(new BinaryMessage(frame));
            } catch (Exception e) {
                log.warn("隧道[{}]发送请求体失败: {}", machineId, e);
                this.close(1011, "发送失败");
                return;
            }
        }
    }

    @Override
    public void close(int code, String reason) {
        if (closed) {
            return;
        }
        closed = true;
        Pending[] pendingItems = pending.values().toArray(new Pending[0]);
        for (Pending item : pendingItems) {
            item.abort(code, reason);
        }
        pending.clear();
        ServerStream[] streamItems = streams.values().toArray(new ServerStream[0]);
        for (ServerStream stream : streamItems) {
            stream.peerClosed(code);
        }
        streams.clear();
        try {
            if (session.isOpen()) {
                session.close(new CloseStatus(code, StrUtil.maxLength(reason, 100)));
            }
        } catch (Exception e) {
            log.debug("隧道[{}]关闭异常", machineId, e);
        }
    }

    // ---------------------------------------------------------------- 请求

    /**
     * 执行一次请求并等待完整响应
     *
     * @param urlItem 请求信息
     * @param body    请求体
     * @param timeout 超时（秒）
     * @return 响应
     */
    public TunnelResponse request(IUrlItem urlItem, byte[] body, int timeout) {
        return this.request(urlItem, body, contentType(urlItem.contentType()), "POST", timeout);
    }

    /**
     * 执行一次请求并等待完整响应（可指定方法与 content type，下载走 GET）
     *
     * @param urlItem     请求信息
     * @param body        请求体
     * @param contentType 请求 content type
     * @param method      HTTP 方法
     * @param timeout     超时（秒）
     * @return 响应
     */
    public TunnelResponse request(IUrlItem urlItem, byte[] body, String contentType, String method, int timeout) {
        return this.request(urlItem, body, contentType, method, timeout, null);
    }

    /**
     * 执行一次请求并等待完整响应（可附带额外请求头，例如节点授权头）
     *
     * @param urlItem      请求信息
     * @param body         请求体
     * @param contentType  请求 content type
     * @param method       HTTP 方法
     * @param timeout      超时（秒）
     * @param extraHeaders 额外请求头（优先级高于 urlItem.header()）
     * @return 响应
     */
    public TunnelResponse request(IUrlItem urlItem, byte[] body, String contentType, String method, int timeout,
                                  Map<String, String> extraHeaders) {
        String sid = TunnelSid.next();
        BufferPending item = new BufferPending(sid);
        pending.put(sid, item);
        try {
            this.writeRequest(urlItem, sid, body, contentType, method, extraHeaders);
            return item.future.get(Math.max(timeout, 1), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            this.sendControl(TunnelFrame.abort(sid, 504, "请求超时"));
            throw new TunnelException(504, "节点通过反向隧道响应超时（" + timeout + " 秒）：" + urlItem.path());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TunnelException(500, "隧道请求被中断：" + urlItem.path());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof TunnelException) {
                throw (TunnelException) cause;
            }
            throw new TunnelException(502, "隧道请求失败：" + cause.getMessage(), cause);
        } finally {
            pending.remove(sid);
        }
    }

    /**
     * 发起下载请求，返回流式响应（不缓存完整响应体）
     *
     * @param urlItem 请求信息
     * @param body    请求体
     * @param timeout 超时（秒）
     * @return 下载响应
     */
    public TunnelDownload download(IUrlItem urlItem, byte[] body, int timeout) {
        return this.download(urlItem, body, contentType(urlItem.contentType()), timeout);
    }

    /**
     * 发起下载请求（GET），返回流式响应
     *
     * @param urlItem     请求信息
     * @param body        请求体
     * @param contentType 请求 content type
     * @param timeout     超时（秒）
     * @return 下载响应
     */
    public TunnelDownload download(IUrlItem urlItem, byte[] body, String contentType, int timeout) {
        return this.download(urlItem, body, contentType, timeout, null);
    }

    /**
     * 发起下载请求（GET，可附带额外请求头）
     *
     * @param urlItem      请求信息
     * @param body         请求体
     * @param contentType  请求 content type
     * @param timeout      超时（秒）
     * @param extraHeaders 额外请求头
     * @return 下载响应
     */
    public TunnelDownload download(IUrlItem urlItem, byte[] body, String contentType, int timeout, Map<String, String> extraHeaders) {
        String sid = TunnelSid.next();
        DownloadPending item = new DownloadPending(sid);
        pending.put(sid, item);
        this.writeRequest(urlItem, sid, body, contentType, "GET", extraHeaders);
        try {
            if (!item.headLatch.await(Math.max(timeout, 1), TimeUnit.SECONDS)) {
                this.sendControl(TunnelFrame.abort(sid, 504, "请求超时"));
                throw new TunnelException(504, "节点通过反向隧道响应超时（" + timeout + " 秒）：" + urlItem.path());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TunnelException(500, "隧道下载被中断：" + urlItem.path());
        } finally {
            // 注意：不能在这里移除 pending，响应体还要继续流入；由 END/ABORT 移除
            if (item.aborted) {
                pending.remove(sid);
            }
        }
        TunnelFrame head = item.head;
        return new TunnelDownload(head == null ? 0 : (head.getStatus() == null ? 0 : head.getStatus()),
            item.contentDisposition, item.contentType, item.contentLength, item.inputStream);
    }

    private void writeRequest(IUrlItem urlItem, String sid, byte[] body, String requestContentType, String method,
                             Map<String, String> extraHeaders) {
        if (!isOpen()) {
            throw new TunnelException(503, "反向隧道已离线：" + machineId);
        }
        TunnelFrame frame = TunnelFrame.of(TunnelFrameType.REQ, sid);
        frame.setMethod(StrUtil.emptyToDefault(method, "POST"));
        frame.setPath(urlItem.path());
        frame.setContentType(StrUtil.emptyToDefault(requestContentType, contentType(urlItem.contentType())));
        Map<String, String> headers = new HashMap<>();
        if (urlItem.header() != null) {
            headers.putAll(urlItem.header());
        }
        if (StrUtil.isNotEmpty(urlItem.workspaceId())) {
            headers.put(TunnelConst.HEADER_WORKSPACE_ID, urlItem.workspaceId());
        }
        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }
        frame.setHeaders(headers);
        frame.setLen((long) (body == null ? 0 : body.length));
        frame.setTimeout(urlItem.timeout());
        this.sendControl(frame);
        this.sendBody(sid, body);
        this.sendControl(TunnelFrame.of(TunnelFrameType.REQ_BODY_END, sid));
    }

    private static String contentType(DataContentType dataContentType) {
        if (dataContentType == null) {
            return "application/x-www-form-urlencoded";
        }
        switch (dataContentType) {
            case JSON:
                return "application/json";
            case FORM_URLENCODED:
            default:
                return "application/x-www-form-urlencoded";
        }
    }

    // ---------------------------------------------------------------- 双向流

    /**
     * 打开一条双向流
     *
     * @param urlItem 请求信息（path 即本机 ws 路径）
     * @param query   额外查询参数
     * @param timeout 打开超时（秒）
     * @return 双向流
     */
    public TunnelStream openStream(IUrlItem urlItem, Map<String, String> query, int timeout) {
        String sid = TunnelSid.next();
        ServerStream stream = new ServerStream(sid);
        streams.put(sid, stream);
        TunnelFrame frame = TunnelFrame.of(TunnelFrameType.OPEN, sid);
        frame.setPath(urlItem.path());
        frame.setQuery(query == null ? Collections.emptyMap() : query);
        Map<String, String> headers = new HashMap<>();
        if (urlItem.header() != null) {
            headers.putAll(urlItem.header());
        }
        frame.setHeaders(headers);
        frame.setTimeout(urlItem.timeout());
        this.sendControl(frame);
        boolean ok;
        try {
            ok = stream.openedLatch.await(Math.max(timeout, 1), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ok = false;
        }
        if (!ok || !stream.openedOk) {
            streams.remove(sid);
            this.sendControl(TunnelFrame.close(sid, 4000, "打开双向流失败"));
            throw new TunnelException(502, "反向模式下打开双向流失败：" + urlItem.path() + (stream.openedMsg == null ? "" : "，" + stream.openedMsg));
        }
        return stream;
    }

    // ---------------------------------------------------------------- 接收

    /**
     * 处理控制帧
     *
     * @param frame 控制帧
     */
    public void onControl(TunnelFrame frame) {
        TunnelFrameType type = frame.frameType();
        switch (type) {
            case PING:
                this.sendControl(TunnelFrame.pong());
                break;
            case PONG:
                lastPongAt = System.currentTimeMillis();
                missedPong.set(0);
                break;
            case RES: {
                Pending item = pending.get(frame.getSid());
                if (item != null) {
                    item.head(frame);
                }
                break;
            }
            case END: {
                Pending item = pending.remove(frame.getSid());
                if (item != null) {
                    item.end();
                }
                ServerStream stream = streams.get(frame.getSid());
                if (stream != null) {
                    stream.peerClosed(CLOSE_NORMAL);
                }
                break;
            }
            case ABORT: {
                Pending item = pending.remove(frame.getSid());
                if (item != null) {
                    item.abort(frame.getCode() == null ? 500 : frame.getCode(), frame.getMsg());
                }
                ServerStream stream = streams.remove(frame.getSid());
                if (stream != null) {
                    stream.peerClosed(frame.getCode() == null ? 500 : frame.getCode());
                }
                break;
            }
            case OPEN_ACK: {
                ServerStream stream = streams.get(frame.getSid());
                if (stream != null) {
                    stream.opened(Boolean.TRUE.equals(frame.getOk()), frame.getMsg());
                }
                break;
            }
            case CLOSE: {
                ServerStream stream = streams.remove(frame.getSid());
                if (stream != null) {
                    stream.peerClosed(frame.getCode() == null ? CLOSE_NORMAL : frame.getCode());
                }
                break;
            }
            case PUSH: {
                java.util.function.Consumer<TunnelFrame> handler = this.pushHandler;
                if (handler == null) {
                    log.debug("隧道[{}]收到推送但未注册处理器，丢弃 topic={}", machineId, frame.getTopic());
                    break;
                }
                try {
                    handler.accept(frame);
                } catch (Exception e) {
                    log.warn("隧道[{}]处理推送异常 topic={}", machineId, frame.getTopic(), e);
                }
                break;
            }
            default:
                log.debug("隧道[{}]忽略控制帧: {}", machineId, type);
        }
    }

    /**
     * 处理数据帧
     *
     * @param frame 数据帧
     */
    public void onData(TunnelDataFrame frame) {
        Pending item = pending.get(frame.getSid());
        if (item != null) {
            item.data(frame.getPayload(), frame.isFin());
            return;
        }
        ServerStream stream = streams.get(frame.getSid());
        if (stream != null) {
            stream.peerData(frame.getPayload());
        }
    }

    /**
     * 心跳检查（由注册表的定时任务调用）
     *
     * @return true 仍然存活
     */
    public boolean checkHeartbeat() {
        if (!isOpen()) {
            return false;
        }
        if (!helloPassed) {
            boolean timeout = System.currentTimeMillis() - establishedAt > options.getHelloTimeoutSeconds() * 1000L;
            if (timeout) {
                this.close(CLOSE_HELLO_TIMEOUT, "握手超时");
            }
            return !timeout && isOpen();
        }
        long idle = System.currentTimeMillis() - lastPongAt;
        long limit = options.getHeartbeatSeconds() * 1000L * (options.getMaxMissedPong() + 1);
        if (idle > limit) {
            log.warn("隧道[{}]心跳超时（{}ms 未收到 PONG），断开连接", machineId, idle);
            this.close(CLOSE_HEARTBEAT_TIMEOUT, "心跳超时");
            return false;
        }
        this.sendControl(TunnelFrame.ping());
        return true;
    }

    // ---------------------------------------------------------------- 内部类型

    private abstract class Pending {
        final String sid;
        volatile TunnelFrame head;

        Pending(String sid) {
            this.sid = sid;
        }

        void head(TunnelFrame frame) {
            this.head = frame;
        }

        abstract void data(byte[] payload, boolean fin);

        abstract void end();

        abstract void abort(int code, String msg);
    }

    private class BufferPending extends Pending {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();
        private final CompletableFuture<TunnelResponse> future = new CompletableFuture<>();

        BufferPending(String sid) {
            super(sid);
        }

        @Override
        void data(byte[] payload, boolean fin) {
            if (payload != null && payload.length > 0) {
                synchronized (body) {
                    body.write(payload, 0, payload.length);
                }
            }
        }

        @Override
        void end() {
            TunnelFrame frame = this.head;
            int status = frame == null || frame.getStatus() == null ? 0 : frame.getStatus();
            Map<String, String> headers = frame == null || frame.getHeaders() == null ? Collections.emptyMap() : frame.getHeaders();
            synchronized (body) {
                future.complete(new TunnelResponse(status, headers, body.toByteArray()));
            }
        }

        @Override
        void abort(int code, String msg) {
            future.completeExceptionally(new TunnelException(code, StrUtil.isEmpty(msg) ? "隧道请求被中止" : msg));
        }
    }

    private class DownloadPending extends Pending {
        private final TunnelInputStream inputStream = new TunnelInputStream();
        private final CountDownLatch headLatch = new CountDownLatch(1);
        private volatile String contentDisposition;
        private volatile String contentType;
        private volatile long contentLength = -1L;
        private volatile boolean aborted;

        DownloadPending(String sid) {
            super(sid);
        }

        @Override
        void head(TunnelFrame frame) {
            super.head(frame);
            this.contentDisposition = header(frame, "Content-Disposition");
            this.contentType = header(frame, "Content-Type");
            this.contentLength = parseLength(header(frame, "Content-Length"));
            headLatch.countDown();
        }

        @Override
        void data(byte[] payload, boolean fin) {
            inputStream.offer(payload);
        }

        @Override
        void end() {
            headLatch.countDown();
            inputStream.finish();
        }

        @Override
        void abort(int code, String msg) {
            aborted = true;
            headLatch.countDown();
            inputStream.fail(new TunnelException(code, msg));
        }

        private long parseLength(String value) {
            if (StrUtil.isBlank(value)) {
                return -1L;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                return -1L;
            }
        }

        private String header(TunnelFrame frame, String name) {
            if (frame == null || frame.getHeaders() == null) {
                return null;
            }
            for (Map.Entry<String, String> entry : frame.getHeaders().entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    /**
     * 服务端侧双向流
     */
    private class ServerStream implements TunnelStream {
        private final String sid;
        private final CountDownLatch openedLatch = new CountDownLatch(1);
        private volatile boolean openedOk;
        private volatile String openedMsg;
        private volatile boolean streamClosed;
        private volatile Consumer<byte[]> dataConsumer;
        private volatile Consumer<Integer> closeConsumer;

        ServerStream(String sid) {
            this.sid = sid;
        }

        void opened(boolean ok, String msg) {
            this.openedOk = ok;
            this.openedMsg = msg;
            openedLatch.countDown();
        }

        void peerData(byte[] payload) {
            Consumer<byte[]> consumer = dataConsumer;
            if (consumer != null && payload != null && payload.length > 0) {
                consumer.accept(payload);
            }
        }

        void peerClosed(int code) {
            if (streamClosed) {
                return;
            }
            streamClosed = true;
            Consumer<Integer> consumer = closeConsumer;
            if (consumer != null) {
                consumer.accept(code);
            }
        }

        @Override
        public String sid() {
            return sid;
        }

        @Override
        public void onData(Consumer<byte[]> consumer) {
            this.dataConsumer = consumer;
        }

        @Override
        public void onClose(Consumer<Integer> consumer) {
            this.closeConsumer = consumer;
        }

        @Override
        public void write(byte[] payload) {
            if (streamClosed) {
                return;
            }
            // 双向流的每条消息都不能带 FIN：FIN 表示该方向结束，插件端收到 FIN 会关闭本机
            // WebSocket（Go 插件端 onBinary 即如此），导致 getVersion 等请求刚发出流就断掉
            sendBody(sid, payload, false);
        }

        @Override
        public void close(int code, String reason) {
            if (streamClosed) {
                return;
            }
            streamClosed = true;
            streams.remove(sid);
            sendControl(TunnelFrame.close(sid, code, reason));
        }

        @Override
        public boolean isClosed() {
            return streamClosed;
        }
    }
}
