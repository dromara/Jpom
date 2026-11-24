/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel;

import org.dromara.jpom.transport.TransportServer;

/**
 * 反向建连隧道的常量与协议默认值。
 * <p>
 * 服务端与插件端共用本模块的协议定义，任何默认值只在这里改一处。
 *
 * @author jpom
 * @since 2026/11/18
 */
public final class TunnelConst {

    private TunnelConst() {
    }

    /**
     * 隧道 WebSocket 端点路径（服务端暴露的唯一入口）
     */
    public static final String ENDPOINT_PATH = "/agent/tunnel";

    /**
     * 数据帧魔数（帧首字节），用于与文本控制帧区分并做脏数据快速判断
     */
    public static final int DATA_FRAME_MAGIC = 0x01;

    /**
     * 单个二进制数据帧承载的最大字节数
     * <p>
     * 帧过大顶住心跳与其它 sid（队头阻塞），过小则帧头开销高，256 KiB 为折中值。
     */
    public static final int MAX_DATA_FRAME_SIZE = 256 * 1024;

    /**
     * sid（逻辑请求/流标识）最大字节长度
     */
    public static final int MAX_SID_LENGTH = 24;

    /**
     * 心跳间隔（秒）
     */
    public static final int DEFAULT_HEARTBEAT_SECONDS = 5;

    /**
     * 连续未收到 PONG 的次数上限，超过即判定离线
     */
    public static final int DEFAULT_MAX_MISSED_PONG = 3;

    /**
     * 插件端并发处理请求数上限（背压的第一道闸）
     */
    public static final int DEFAULT_MAX_CONCURRENCY = 16;

    /**
     * 流控窗口（字节）：发送方未收到 ACK 的字节数超过该值即暂停发送
     */
    public static final long DEFAULT_WINDOW_BYTES = 4L * 1024 * 1024;

    /**
     * 握手签名允许的时间偏移（秒），超过即认为重放
     */
    public static final long DEFAULT_AUTH_SKEW_SECONDS = 300;

    /**
     * 插件端主动推送节点状态的主题
     */
    public static final String TOPIC_NODE_STAT = "node-stat";

    /**
     * 插件端主动推送的默认间隔（秒）
     */
    public static final int DEFAULT_PUSH_INTERVAL_SECONDS = 30;

    /**
     * 单条推送内容（JSON 文本）允许的最大字节数，超过即丢弃（防止慢速大包顶住心跳）
     */
    public static final int MAX_PUSH_BODY_BYTES = 256 * 1024;

    /**
     * 服务端：推送数据允许的最大年龄（秒），超过即视为节点不再上报（默认 3 个心跳周期）
     */
    public static final int DEFAULT_PUSH_MAX_AGE_SECONDS = 90;

    /**
     * 服务端连接密钥配置项
     */
    public static final String CONFIG_KEY = "jpom.agent.tunnel.key";

    /**
     * 允许未注册节点自动注册的开关配置项（默认 false：未注册节点直接拒绝）
     */
    public static final String CONFIG_ALLOW_AUTO_REGISTER = "jpom.agent.tunnel.allow-auto-register";

    /**
     * 插件端连接服务端的地址配置项（支持 ws / wss）
     */
    public static final String CONFIG_AGENT_SERVER_URL = "jpom.agent.tunnel.server-url";

    /**
     * 插件端连接密钥配置项
     */
    public static final String CONFIG_AGENT_KEY = "jpom.agent.tunnel.key";

    /**
     * 插件端：是否启用主动推送（默认 true，且仅在隧道在线时生效）
     * <p>
     * 插件端配置前缀是 {@code jpom.tunnel}（{@code TunnelAgentProperties}），与服务端的
     * {@code jpom.agent.tunnel} 不是同一层，别写错。
     */
    public static final String CONFIG_AGENT_PUSH_ENABLED = "jpom.tunnel.push-enabled";

    /**
     * 插件端：主动推送间隔（秒）
     */
    public static final String CONFIG_AGENT_PUSH_INTERVAL = "jpom.tunnel.push-interval";

    /**
     * 服务端：推送数据最大年龄（秒），超过即判定节点不再上报
     */
    public static final String CONFIG_PUSH_MAX_AGE = "jpom.agent.tunnel.push-max-age-seconds";

    /**
     * 与正向传输保持一致的授权 header，插件端回环调用本机 HTTP 时必须带上
     */
    public static final String HEADER_AUTHORIZE = TransportServer.JPOM_AGENT_AUTHORIZE;

    /**
     * 工作空间 header，与正向传输保持一致
     */
    public static final String HEADER_WORKSPACE_ID = TransportServer.WORKSPACE_ID_REQ_HEADER;

    /**
     * 传输加密 header，与正向传输保持一致（body 加密规则沿用 transport-encryption）
     */
    public static final String HEADER_TRANSPORT_ENCRYPTION = TransportServer.TRANSPORT_ENCRYPTION;
}
