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
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * 服务端 {@code /agent/tunnel} 端点处理器：握手鉴权、心跳、把帧分发给对应通道。
 * <p>
 * 该端点不参与用户体系鉴权（插件端没有平台账号），鉴权由 {@link TunnelAuthenticator} 完成。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelServerHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TunnelServerHandler.class);
    private static final String CHANNEL_KEY = "jpom-tunnel-channel";
    /**
     * 发送时间上限
     */
    private static final int SEND_TIME_LIMIT = 60 * 1000;
    /**
     * 发送缓冲上限（16MB）
     */
    private static final int SEND_BUFFER_SIZE_LIMIT = 16 * 1024 * 1024;

    private final TunnelChannelRegistry registry;
    private final TunnelAuthenticator authenticator;
    private final TunnelServerOptions options;
    private final String serverVersion;

    public TunnelServerHandler(TunnelChannelRegistry registry, TunnelAuthenticator authenticator,
                               TunnelServerOptions options, String serverVersion) {
        this.registry = registry;
        this.authenticator = authenticator;
        this.options = options == null ? new TunnelServerOptions() : options;
        this.serverVersion = serverVersion;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession decorator = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, SEND_BUFFER_SIZE_LIMIT);
        TunnelChannel channel = new TunnelChannel(decorator, options);
        channel.setRemoteAddress(remoteAddress(session));
        channel.setPushHandler(frame -> registry.dispatchPush(channel, frame));
        session.getAttributes().put(CHANNEL_KEY, channel);
        log.debug("反向隧道连接建立 session={} ip={}", channel.sessionId(), channel.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        TunnelChannel channel = channel(session);
        if (channel == null) {
            return;
        }
        TunnelFrame frame;
        try {
            frame = TunnelFrameCodec.decodeControl(message.getPayload());
        } catch (Exception e) {
            log.warn("反向隧道[{}]收到非法控制帧", channel.getRemoteAddress(), e);
            channel.close(4008, "非法帧格式");
            return;
        }
        if (frame.frameType() == TunnelFrameType.HELLO) {
            this.handleHello(channel, frame);
            return;
        }
        if (!channel.isHelloPassed()) {
            channel.close(TunnelChannel.CLOSE_HELLO_TIMEOUT, "未完成握手");
            return;
        }
        channel.onControl(frame);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        TunnelChannel channel = channel(session);
        if (channel == null || !channel.isHelloPassed()) {
            return;
        }
        try {
            byte[] payload = new byte[message.getPayloadLength()];
            message.getPayload().get(payload);
            TunnelDataFrame frame = TunnelFrameCodec.decodeData(payload);
            channel.onData(frame);
        } catch (Exception e) {
            log.warn("反向隧道[{}]收到非法数据帧", channel.machineId(), e);
            channel.close(4008, "非法数据帧");
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        TunnelChannel channel = channel(session);
        if (channel != null) {
            log.warn("反向隧道[{}]传输异常", channel.machineId(), exception);
            channel.close(1011, "传输异常");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        TunnelChannel channel = channel(session);
        if (channel == null) {
            return;
        }
        channel.close(status.getCode(), StrUtil.emptyToDefault(status.getReason(), "连接关闭"));
        registry.remove(channel);
    }

    private void handleHello(TunnelChannel channel, TunnelFrame frame) {
        if (channel.isHelloPassed()) {
            return;
        }
        TunnelAuthResult result = authenticator.authenticate(frame, channel.getRemoteAddress());
        if (result == null || !result.isAllowed()) {
            int code = result == null ? 4001 : result.getCode();
            String msg = result == null || StrUtil.isEmpty(result.getMsg()) ? "握手校验失败" : result.getMsg();
            log.warn("反向隧道握手被拒 ip={} machineId={} 原因={}", channel.getRemoteAddress(), frame.getMachineId(), msg);
            channel.sendControl(TunnelFrame.reject(code, msg));
            channel.close(code == 0 ? 4001 : code, msg);
            return;
        }
        channel.setMachineId(result.getMachineId());
        channel.setAgentVersion(frame.getAgentVersion());
        channel.setHelloPassed(true);
        registry.register(channel);
        if (!channel.isOpen()) {
            return;
        }
        TunnelFrame ack = TunnelFrame.of(TunnelFrameType.HELLO_ACK, null);
        ack.setSessionId(channel.sessionId());
        ack.setMachineId(result.getMachineId());
        ack.setHeartbeat(options.getHeartbeatSeconds());
        ack.setMaxConcurrency(options.getMaxConcurrency());
        ack.setServerVersion(serverVersion);
        // 能力位：只有本服务端声明支持后，插件端才会发送 PUSH 帧
        // （老服务端不认识 PUSH，收到会按非法帧断开连接，所以插件端不能默认发送）
        ack.setPush(options.isPushSupported());
        ack.setLen((long) options.getWindowBytes());
        channel.sendControl(ack);
    }

    private TunnelChannel channel(WebSocketSession session) {
        Object attribute = session.getAttributes().get(CHANNEL_KEY);
        return attribute instanceof TunnelChannel ? (TunnelChannel) attribute : null;
    }

    private static String remoteAddress(WebSocketSession session) {
        if (session.getRemoteAddress() == null) {
            return StrUtil.EMPTY;
        }
        return session.getRemoteAddress().getAddress() == null
            ? session.getRemoteAddress().getHostString()
            : session.getRemoteAddress().getAddress().getHostAddress();
    }

    /**
     * 端点路径
     *
     * @return 路径
     */
    public static String endpointPath() {
        return TunnelConst.ENDPOINT_PATH;
    }

    /**
     * 会话属性键（服务端需要按 machineId 查询通道时使用注册表，不直接读该键）
     *
     * @return 键
     */
    public static String channelKey() {
        return CHANNEL_KEY;
    }
}
