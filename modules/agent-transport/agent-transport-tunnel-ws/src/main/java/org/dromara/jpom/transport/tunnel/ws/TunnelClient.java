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
import org.dromara.jpom.transport.tunnel.TunnelAuth;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.TunnelDataFrame;
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.dromara.jpom.transport.tunnel.TunnelFrameCodec;
import org.dromara.jpom.transport.tunnel.TunnelFrameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插件端隧道客户端：主动拨号服务端，握手、心跳、断线重连，并把服务端下发的请求/双向流转发到本机接口。
 * <p>
 * 只使用 Spring WebSocket 客户端，{@code ws} 与 {@code wss} 都由服务端地址的协议前缀决定。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelClient {

    private static final Logger log = LoggerFactory.getLogger(TunnelClient.class);

    /**
     * 需要停止重试的拒绝码（配置类错误，重试没有意义）
     */
    private static final List<Integer> FATAL_CODES = Arrays.asList(4001, 4002, 4003);
    private static final int INITIAL_RETRY_SECONDS = 3;
    private static final int MAX_RETRY_SECONDS = 60;
    private static final int SEND_TIME_LIMIT = 60 * 1000;
    private static final int SEND_BUFFER_LIMIT = 16 * 1024 * 1024;

    private final TunnelClientOptions options;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService requestExecutor;
    private final ExecutorService streamExecutor;
    private final StandardWebSocketClient webSocketClient = TunnelWsContainer.createClient();

    private final Map<String, TunnelFrame> requestFrames = new ConcurrentHashMap<>();
    private final Map<String, ByteArrayOutputStream> requestBodies = new ConcurrentHashMap<>();
    private final Map<String, ClientStream> streams = new ConcurrentHashMap<>();

    private volatile WebSocketSession session;
    private volatile boolean running;
    private volatile boolean connected;
    /**
     * 服务端是否在 HELLO_ACK 中声明支持主动推送（{@code PUSH}），未声明时不发送推送帧
     */
    private volatile boolean pushSupported;
    private volatile boolean connecting;
    private volatile long lastReceived = System.currentTimeMillis();
    private volatile long nextAttemptAt;
    private volatile int heartbeatSeconds = TunnelConst.DEFAULT_HEARTBEAT_SECONDS;
    private volatile int retrySeconds = INITIAL_RETRY_SECONDS;
    private volatile String lastError;
    private volatile CountDownLatch helloLatch;

    public TunnelClient(TunnelClientOptions options) {
        this.options = options;
        AtomicInteger counter = new AtomicInteger();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jpom-tunnel-client");
            thread.setDaemon(true);
            return thread;
        });
        this.requestExecutor = Executors.newFixedThreadPool(options.getMaxConcurrency(), runnable -> {
            Thread thread = new Thread(runnable, "jpom-tunnel-req-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        this.streamExecutor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "jpom-tunnel-stream-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 启动（幂等）
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        this.scheduler.scheduleWithFixedDelay(this::tick, 1, 1, TimeUnit.SECONDS);
        log.info("反向隧道客户端已启动，目标 {} 节点 {}", options.getServerUrl(), options.getMachineId());
    }

    /**
     * 停止
     */
    public synchronized void stop() {
        running = false;
        this.closeSession(CloseStatus.NORMAL.getCode(), "客户端停止");
        scheduler.shutdownNow();
        requestExecutor.shutdownNow();
        streamExecutor.shutdownNow();
        log.info("反向隧道客户端已停止");
    }

    /**
     * 是否已连上并完成握手
     *
     * @return true 在线
     */
    public boolean isConnected() {
        return connected && session != null && session.isOpen();
    }

    /**
     * 服务端是否声明支持主动推送（{@code HELLO_ACK.push=true}）
     *
     * @return true 可以发送 {@code PUSH} 帧
     */
    public boolean isPushSupported() {
        return pushSupported;
    }

    /**
     * 最近一次错误
     *
     * @return 错误信息
     */
    public String getLastError() {
        return lastError;
    }

    // ---------------------------------------------------------------- 连接

    private void tick() {
        if (!running) {
            return;
        }
        if (this.isConnected()) {
            long idle = System.currentTimeMillis() - lastReceived;
            long limit = heartbeatSeconds * 1000L * (TunnelConst.DEFAULT_MAX_MISSED_PONG + 1);
            if (idle > limit) {
                lastError = "服务端心跳超时";
                log.warn("反向隧道心跳超时（{}ms 未收到数据），准备重连", idle);
                this.closeSession(4000, "服务端心跳超时");
            }
            return;
        }
        if (connecting || System.currentTimeMillis() < nextAttemptAt) {
            return;
        }
        this.attemptConnect();
    }

    private void attemptConnect() {
        connecting = true;
        // 每次尝试都清空上一次的错误，避免旧错误让成功的握手被误判为失败
        lastError = null;
        try {
            String url = normalizeUrl(options.getServerUrl());
            helloLatch = new CountDownLatch(1);
            ClientHandler handler = new ClientHandler();
            this.webSocketClient.doHandshake(handler, null, URI.create(url))
                .get(options.getTimeoutSeconds(), TimeUnit.SECONDS);
            CountDownLatch latch = helloLatch;
            if (latch == null || !latch.await(options.getTimeoutSeconds(), TimeUnit.SECONDS)) {
                throw new TunnelException(408, "等待握手响应超时");
            }
            if (!connected) {
                throw new TunnelException(401, StrUtil.emptyToDefault(lastError, "握手未完成"));
            }
            retrySeconds = INITIAL_RETRY_SECONDS;
            log.info("反向隧道已连接 {}，session={}", options.getServerUrl(), session == null ? StrUtil.EMPTY : session.getId());
        } catch (Exception e) {
            lastError = e.getMessage();
            this.closeSession(4000, "连接失败");
            retrySeconds = Math.min(retrySeconds * 2, MAX_RETRY_SECONDS);
            nextAttemptAt = System.currentTimeMillis() + retrySeconds * 1000L;
            log.info("反向隧道连接失败：{}，{} 秒后重试", lastError, retrySeconds);
        } finally {
            connecting = false;
        }
    }

    /**
     * 把服务端地址规范化为 WebSocket 地址（http/https 自动转 ws/wss，缺路径补 {@code /agent/tunnel}）
     *
     * @param serverUrl 服务端地址
     * @return WebSocket 地址
     */
    static String normalizeUrl(String serverUrl) {
        String url = StrUtil.trim(serverUrl);
        if (StrUtil.isEmpty(url)) {
            throw new TunnelException(400, "未配置服务端隧道地址");
        }
        if (url.startsWith("http://")) {
            url = "ws://" + url.substring("http://".length());
        } else if (url.startsWith("https://")) {
            url = "wss://" + url.substring("https://".length());
        }
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            url = "ws://" + url;
        }
        String afterScheme = url.substring(url.indexOf("://") + 3);
        if (afterScheme.indexOf('/') < 0) {
            url = url + TunnelConst.ENDPOINT_PATH;
        }
        return url;
    }

    private void sendHello() {
        long ts = System.currentTimeMillis() / 1000;
        String nonce = TunnelAuth.nonce();
        TunnelFrame hello = TunnelFrame.of(TunnelFrameType.HELLO, null);
        hello.setMachineId(options.getMachineId());
        hello.setKey(options.getKey());
        hello.setAgentVersion(options.getAgentVersion());
        hello.setOs(options.getOs());
        hello.setArch(options.getArch());
        hello.setTs(ts);
        hello.setNonce(nonce);
        hello.setSign(TunnelAuth.sign(options.getKey(), options.getMachineId(), ts, nonce));
        this.sendControl(hello);
    }

    private void closeSession(int code, String reason) {
        connected = false;
        // 重连后需重新等新服务端声明能力位，不能沿用上一条连接的结果
        pushSupported = false;
        requestFrames.clear();
        requestBodies.clear();
        for (ClientStream stream : streams.values()) {
            stream.peerClosed(code);
        }
        streams.clear();
        WebSocketSession current = session;
        session = null;
        if (current != null && current.isOpen()) {
            try {
                current.close(new CloseStatus(Math.max(code, 1000), StrUtil.maxLength(reason, 100)));
            } catch (Exception e) {
                log.debug("关闭隧道连接异常", e);
            }
        }
    }

    // ---------------------------------------------------------------- 发送

    private void sendText(String text) {
        WebSocketSession current = session;
        if (current == null || !current.isOpen()) {
            return;
        }
        try {
            current.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.warn("反向隧道发送控制帧失败", e);
            this.closeSession(1011, "发送失败");
        }
    }

    private void sendBinary(byte[] payload) {
        WebSocketSession current = session;
        if (current == null || !current.isOpen()) {
            return;
        }
        try {
            current.sendMessage(new BinaryMessage(payload));
        } catch (Exception e) {
            log.warn("反向隧道发送数据帧失败", e);
            this.closeSession(1011, "发送失败");
        }
    }

    /**
     * 主动推送一条数据（插件端 -> 服务端，单向）
     * <p>
     * 仅在隧道已连接时发送；未连接返回 {@code false}，调用方下一轮再试，不做缓存重放。
     *
     * @param topic 主题（见 {@code TunnelConst#TOPIC_NODE_STAT}）
     * @param seq   序号（可空）
     * @param body  推送内容（JSON 文本）
     * @return true 已提交发送
     */
    public boolean sendPush(String topic, Long seq, String body) {
        if (!this.pushSupported) {
            // 老服务端把未知帧当作非法帧直接断开，能力位未声明时宁可不推
            log.debug("服务端未支持主动推送，跳过 topic={}", topic);
            return false;
        }
        if (!this.isConnected()) {
            log.debug("反向隧道未连接，跳过推送 topic={}", topic);
            return false;
        }
        this.sendControl(TunnelFrame.push(topic, seq, body));
        return true;
    }

    private void sendControl(TunnelFrame frame) {
        if (!this.isConnected() && frame.frameType() != TunnelFrameType.HELLO) {
            log.debug("反向隧道未连接，跳过发送 {}", frame.frameType());
        }
        this.sendText(TunnelFrameCodec.encodeControl(frame));
    }

    private void sendBody(String sid, byte[] payload, boolean fin) {
        for (byte[] frame : TunnelFrameCodec.split(sid, payload)) {
            this.sendBinary(frame);
        }
        if (fin) {
            this.sendBinary(TunnelFrameCodec.encodeData(sid, null, true));
        }
    }

    // ---------------------------------------------------------------- 接收

    private void onText(String text) {
        lastReceived = System.currentTimeMillis();
        TunnelFrame frame;
        try {
            frame = TunnelFrameCodec.decodeControl(text);
        } catch (Exception e) {
            log.warn("反向隧道收到非法控制帧: {}", StrUtil.maxLength(text, 200));
            return;
        }
        switch (frame.frameType()) {
            case HELLO_ACK:
                if (frame.getHeartbeat() != null && frame.getHeartbeat() > 0) {
                    heartbeatSeconds = frame.getHeartbeat();
                }
                connected = true;
                // 服务端能力位：老服务端不认识 PUSH 帧（收到会按非法帧断开连接），未声明时插件端不推送
                this.pushSupported = Boolean.TRUE.equals(frame.getPush());
                if (!this.pushSupported) {
                    log.info("服务端未声明支持主动推送（老版本服务端），插件端不发送 PUSH 帧");
                }
                CountDownLatch latch = helloLatch;
                if (latch != null) {
                    latch.countDown();
                }
                break;
            case REJECT:
                lastError = StrUtil.emptyToDefault(frame.getMsg(), "握手被拒绝");
                log.warn("反向隧道握手被拒绝：code={} msg={}", frame.getCode(), lastError);
                if (frame.getCode() != null && FATAL_CODES.contains(frame.getCode())) {
                    running = false;
                }
                CountDownLatch rejectLatch = helloLatch;
                if (rejectLatch != null) {
                    rejectLatch.countDown();
                }
                this.closeSession(frame.getCode() == null ? 4001 : frame.getCode(), lastError);
                break;
            case PING:
                this.sendControl(TunnelFrame.pong());
                break;
            case PONG:
                break;
            case REQ: {
                String sid = frame.getSid();
                requestFrames.put(sid, frame);
                requestBodies.put(sid, new ByteArrayOutputStream());
                // 空请求体时节点可能只发 REQ + REQ_BODY_END
                if (frame.getLen() != null && frame.getLen() == 0) {
                    // 等待 REQ_BODY_END 触发，避免与数据帧竞争
                    log.debug("隧道请求 {} 空请求体", sid);
                }
                break;
            }
            case REQ_BODY_END:
                this.dispatchRequest(frame.getSid());
                break;
            case ABORT: {
                requestFrames.remove(frame.getSid());
                requestBodies.remove(frame.getSid());
                ClientStream abortStream = streams.remove(frame.getSid());
                if (abortStream != null) {
                    abortStream.peerClosed(frame.getCode() == null ? 500 : frame.getCode());
                }
                break;
            }
            case OPEN:
                this.handleOpen(frame);
                break;
            case CLOSE: {
                ClientStream closedStream = streams.remove(frame.getSid());
                if (closedStream != null) {
                    closedStream.peerClosed(frame.getCode() == null ? 1000 : frame.getCode());
                }
                break;
            }
            case END: {
                ClientStream endedStream = streams.remove(frame.getSid());
                if (endedStream != null) {
                    endedStream.peerClosed(1000);
                }
                break;
            }
            default:
                log.debug("反向隧道忽略控制帧: {}", frame.frameType());
        }
    }

    private void onBinary(byte[] payload) {
        lastReceived = System.currentTimeMillis();
        TunnelDataFrame frame;
        try {
            frame = TunnelFrameCodec.decodeData(payload);
        } catch (Exception e) {
            log.warn("反向隧道收到非法数据帧", e);
            return;
        }
        ClientStream stream = streams.get(frame.getSid());
        if (stream != null) {
            stream.peerData(frame.getPayload());
            return;
        }
        ByteArrayOutputStream body = requestBodies.get(frame.getSid());
        if (body == null) {
            return;
        }
        if (frame.getPayloadLength() > 0) {
            synchronized (body) {
                body.write(frame.getPayload(), 0, frame.getPayloadLength());
            }
        }
        if (frame.isFin()) {
            this.dispatchRequest(frame.getSid());
        }
    }

    private void dispatchRequest(String sid) {
        TunnelFrame frame = requestFrames.remove(sid);
        ByteArrayOutputStream buffer = requestBodies.remove(sid);
        if (frame == null) {
            return;
        }
        byte[] body = buffer == null ? new byte[0] : buffer.toByteArray();
        this.requestExecutor.submit(() -> {
            ClientResponseSender sender = new ClientResponseSender(sid);
            try {
                TunnelRequestHandler handler = options.getRequestHandler();
                if (handler == null) {
                    sender.abort(501, "插件端未配置请求处理器");
                    return;
                }
                handler.handle(frame, body, sender);
            } catch (Throwable e) {
                log.warn("隧道请求处理失败 sid={} path={}", sid, frame.getPath(), e);
                sender.abort(500, StrUtil.emptyToDefault(e.getMessage(), e.getClass().getSimpleName()));
            }
        });
    }

    private void handleOpen(TunnelFrame frame) {
        String sid = frame.getSid();
        ClientStream stream = new ClientStream(sid);
        streams.put(sid, stream);
        this.streamExecutor.submit(() -> {
            try {
                TunnelStreamHandler handler = options.getStreamHandler();
                if (handler == null) {
                    throw new TunnelException(501, "插件端未配置双向流处理器");
                }
                handler.open(frame, stream);
                TunnelFrame ack = TunnelFrame.of(TunnelFrameType.OPEN_ACK, sid);
                ack.setOk(true);
                this.sendControl(ack);
            } catch (Throwable e) {
                log.warn("隧道双向流打开失败 sid={} path={}", sid, frame.getPath(), e);
                streams.remove(sid);
                TunnelFrame ack = TunnelFrame.of(TunnelFrameType.OPEN_ACK, sid);
                ack.setOk(false);
                ack.setMsg(StrUtil.emptyToDefault(e.getMessage(), e.getClass().getSimpleName()));
                this.sendControl(ack);
            }
        });
    }

    // ---------------------------------------------------------------- 内部类型

    private class ClientHandler extends AbstractWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession newSession) {
            session = new ConcurrentWebSocketSessionDecorator(newSession, SEND_TIME_LIMIT, SEND_BUFFER_LIMIT);
            lastReceived = System.currentTimeMillis();
            sendHello();
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            onText(message.getPayload());
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            byte[] payload = new byte[message.getPayloadLength()];
            message.getPayload().get(payload);
            onBinary(payload);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            lastError = exception.getMessage();
            log.warn("反向隧道传输异常", exception);
            closeSession(1011, "传输异常");
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            lastError = StrUtil.emptyToDefault(status.getReason(), "连接关闭");
            log.info("反向隧道连接已关闭 code={} reason={} 已连={}", status.getCode(), lastError, connected);
            closeSession(status.getCode(), lastError);
        }
    }

    private class ClientResponseSender implements TunnelResponseSender {
        private final String sid;
        private volatile boolean finished;

        ClientResponseSender(String sid) {
            this.sid = sid;
        }

        @Override
        public void head(int status, Map<String, String> headers) {
            TunnelFrame frame = TunnelFrame.of(TunnelFrameType.RES, sid);
            frame.setStatus(status);
            frame.setHeaders(headers);
            sendControl(frame);
        }

        @Override
        public void write(byte[] payload) {
            if (finished) {
                return;
            }
            sendBody(sid, payload, false);
        }

        @Override
        public void end() {
            if (finished) {
                return;
            }
            finished = true;
            sendBinary(TunnelFrameCodec.encodeData(sid, null, true));
            sendControl(TunnelFrame.end(sid));
        }

        @Override
        public void abort(int code, String msg) {
            if (finished) {
                return;
            }
            finished = true;
            sendControl(TunnelFrame.abort(sid, code, msg));
        }
    }

    private class ClientStream implements TunnelStream {
        private final String sid;
        private volatile boolean closed;
        private volatile java.util.function.Consumer<byte[]> dataConsumer;
        private volatile java.util.function.Consumer<Integer> closeConsumer;

        ClientStream(String sid) {
            this.sid = sid;
        }

        void peerData(byte[] payload) {
            java.util.function.Consumer<byte[]> consumer = dataConsumer;
            if (consumer != null && payload != null && payload.length > 0) {
                consumer.accept(payload);
            }
        }

        void peerClosed(int code) {
            if (closed) {
                return;
            }
            closed = true;
            java.util.function.Consumer<Integer> consumer = closeConsumer;
            if (consumer != null) {
                consumer.accept(code);
            }
        }

        @Override
        public String sid() {
            return sid;
        }

        @Override
        public void onData(java.util.function.Consumer<byte[]> consumer) {
            this.dataConsumer = consumer;
        }

        @Override
        public void onClose(java.util.function.Consumer<Integer> consumer) {
            this.closeConsumer = consumer;
        }

        @Override
        public void write(byte[] payload) {
            if (closed) {
                return;
            }
            sendBody(sid, payload, false);
        }

        @Override
        public void close(int code, String reason) {
            if (closed) {
                return;
            }
            closed = true;
            streams.remove(sid);
            sendControl(TunnelFrame.close(sid, code, reason));
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
    }
}
