/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannel;
import org.dromara.jpom.transport.tunnel.ws.TunnelPushListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 插件端主动推送的服务端落点：校验推送、保存最新快照，供资产监控消费。
 * <p>
 * 这里只保存“最新一份”数据（节点状态是周期性快照，历史落库由资产监控的
 * {@code MachineNodeStatLogModel} 负责），因此内存占用与节点数成正比、与推送频率无关。
 * 多服务端部署时各实例只持有自己收到的推送，资产监控会保持拉取模式，避免误判节点离线。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
@Service
public class TunnelPushService implements TunnelPushListener {

    private final TunnelConfig tunnelConfig;
    private final Map<String, Snapshot> snapshots = new ConcurrentHashMap<>();
    private final AtomicLong received = new AtomicLong();

    public TunnelPushService(TunnelConfig tunnelConfig) {
        this.tunnelConfig = tunnelConfig;
    }

    @Override
    public void onPush(TunnelChannel channel, TunnelFrame frame) {
        this.accept(channel == null ? null : channel.machineId(), frame);
    }

    /**
     * 校验并保存一条推送（与传输层解耦，便于单测直接投递）
     *
     * @param machineId 机器节点标识
     * @param frame     推送帧
     */
    public void accept(String machineId, TunnelFrame frame) {
        if (StrUtil.isEmpty(machineId)) {
            return;
        }
        String topic = frame.getTopic();
        if (!TunnelConst.TOPIC_NODE_STAT.equals(topic)) {
            log.debug("节点[{}]推送了未知主题 {}，已忽略", machineId, topic);
            return;
        }
        String body = frame.getBody();
        if (StrUtil.isEmpty(body)) {
            log.debug("节点[{}]推送内容为空，已忽略", machineId);
            return;
        }
        int bytes = body.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > TunnelConst.MAX_PUSH_BODY_BYTES) {
            log.warn("节点[{}]推送内容过大（{} 字节，上限 {}），已忽略", machineId, bytes, TunnelConst.MAX_PUSH_BODY_BYTES);
            return;
        }
        Snapshot snapshot = new Snapshot(body, System.currentTimeMillis(), frame.getSeq());
        snapshots.put(machineId, snapshot);
        long count = received.incrementAndGet();
        if (count % 200 == 1) {
            log.debug("节点[{}]推送已接收，累计 {} 条，当前节点数 {}", machineId, count, snapshots.size());
        }
    }

    /**
     * 最新一份推送（不判断新鲜度）
     *
     * @param machineId 机器节点标识
     * @return 快照，未收到过返回 null
     */
    public Snapshot latest(String machineId) {
        if (StrUtil.isEmpty(machineId)) {
            return null;
        }
        return snapshots.get(machineId);
    }

    /**
     * 新鲜的一份推送：在配置的最大年龄内（{@code jpom.agent.tunnel.push-max-age-seconds}）
     *
     * @param machineId 机器节点标识
     * @return 快照，未收到或已过期返回 null
     */
    public Snapshot fresh(String machineId) {
        return this.fresh(machineId, this.maxAgeSeconds() * 1000L);
    }

    /**
     * 指定最大年龄的一份推送
     *
     * @param machineId     机器节点标识
     * @param maxAgeMillis  最大年龄（毫秒）
     * @return 快照，未收到或已过期返回 null
     */
    public Snapshot fresh(String machineId, long maxAgeMillis) {
        Snapshot snapshot = this.latest(machineId);
        if (snapshot == null) {
            return null;
        }
        return System.currentTimeMillis() - snapshot.getReceivedAt() > maxAgeMillis ? null : snapshot;
    }

    /**
     * 清除某节点的推送快照（隧道下线时调用，避免续用过期数据）
     *
     * @param machineId 机器节点标识
     */
    public void remove(String machineId) {
        if (StrUtil.isNotEmpty(machineId)) {
            snapshots.remove(machineId);
        }
    }

    /**
     * 推送最大年龄（秒）
     *
     * @return 秒，最小 1
     */
    public int maxAgeSeconds() {
        return Math.max(tunnelConfig.getPushMaxAgeSeconds(), 1);
    }

    /**
     * 累计接收的推送条数（用于状态接口与排障）
     *
     * @return 条数
     */
    public long receivedCount() {
        return received.get();
    }

    /**
     * 当前持有快照的节点数
     *
     * @return 数量
     */
    public int snapshotSize() {
        return snapshots.size();
    }

    /**
     * 单个节点的推送快照
     */
    public static class Snapshot {
        private final String body;
        private final long receivedAt;
        private final Long seq;

        public Snapshot(String body, long receivedAt, Long seq) {
            this.body = body;
            this.receivedAt = receivedAt;
            this.seq = seq;
        }

        public String getBody() {
            return body;
        }

        public long getReceivedAt() {
            return receivedAt;
        }

        public Long getSeq() {
            return seq;
        }
    }
}
