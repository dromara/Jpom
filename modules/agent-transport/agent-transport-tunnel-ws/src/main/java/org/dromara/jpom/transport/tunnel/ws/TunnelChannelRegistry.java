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
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 在线隧道注册表：机器节点标识 → 通道，负责心跳巡检、重复连接顶替与上下线事件。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelChannelRegistry {

    private static final Logger log = LoggerFactory.getLogger(TunnelChannelRegistry.class);

    private final Map<String, TunnelChannel> channels = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<TunnelConnectionListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TunnelPushListener> pushListeners = new CopyOnWriteArrayList<>();
    private final TunnelServerOptions options;
    private ScheduledExecutorService scheduler;

    public TunnelChannelRegistry(TunnelServerOptions options) {
        this.options = options == null ? new TunnelServerOptions() : options;
    }

    /**
     * 启动心跳巡检
     */
    public synchronized void start() {
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jpom-tunnel-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        long period = this.options.getHeartbeatSeconds();
        scheduler.scheduleWithFixedDelay(this::checkAll, period, period, TimeUnit.SECONDS);
        log.info("反向隧道心跳巡检已启动，间隔 {} 秒", period);
    }

    /**
     * 停止心跳巡检
     */
    public synchronized void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * 注册上下线监听
     *
     * @param listener 监听器
     */
    public void addListener(TunnelConnectionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 注册主动推送监听器（插件端 {@code PUSH} 帧由这里分发）
     *
     * @param listener 监听器
     */
    public void addPushListener(TunnelPushListener listener) {
        if (listener != null) {
            pushListeners.add(listener);
        }
    }

    /**
     * 分发一条插件端推送（单个监听器抛异常不影响其它监听器）
     *
     * @param channel 来源通道
     * @param frame   推送帧
     */
    public void dispatchPush(TunnelChannel channel, TunnelFrame frame) {
        for (TunnelPushListener listener : pushListeners) {
            try {
                listener.onPush(channel, frame);
            } catch (Exception e) {
                log.warn("节点[{}]推送处理失败 topic={}", channel.machineId(), frame.getTopic(), e);
            }
        }
    }

    /**
     * 注册通道（同一节点重复连接会顶掉旧连接）
     *
     * @param channel 通道
     */
    public void register(TunnelChannel channel) {
        String machineId = channel.machineId();
        if (StrUtil.isEmpty(machineId)) {
            channel.close(TunnelChannel.CLOSE_HELLO_TIMEOUT, "缺少机器节点标识");
            return;
        }
        TunnelChannel old = channels.put(machineId, channel);
        if (old != null && old != channel) {
            log.warn("节点[{}]建立了新的反向隧道，顶替旧连接 {}", machineId, old.sessionId());
            old.close(TunnelChannel.CLOSE_REPLACED, "同一节点建立了新连接");
            listeners.forEach(listener -> listener.offline(machineId, TunnelChannel.CLOSE_REPLACED, "被新连接顶替"));
        }
        log.info("节点[{}]反向隧道上线 session={} agent={} ip={}", machineId, channel.sessionId(),
            channel.getAgentVersion(), channel.getRemoteAddress());
        listeners.forEach(listener -> listener.online(channel));
    }

    /**
     * 移除通道（仅当当前注册的仍是该通道时才移除）
     *
     * @param channel 通道
     */
    public void remove(TunnelChannel channel) {
        String machineId = channel.machineId();
        if (StrUtil.isEmpty(machineId)) {
            return;
        }
        boolean removed = channels.remove(machineId, channel);
        if (removed) {
            log.info("节点[{}]反向隧道下线 session={}", machineId, channel.sessionId());
            listeners.forEach(listener -> listener.offline(machineId, TunnelChannel.CLOSE_NORMAL, "连接关闭"));
        }
    }

    /**
     * 取得在线通道
     *
     * @param machineId 机器节点标识
     * @return 通道，离线返回 null
     */
    public TunnelChannel get(String machineId) {
        if (StrUtil.isEmpty(machineId)) {
            return null;
        }
        TunnelChannel channel = channels.get(machineId);
        if (channel == null) {
            return null;
        }
        if (!channel.isOpen()) {
            channels.remove(machineId, channel);
            return null;
        }
        return channel;
    }

    /**
     * 节点是否通过反向隧道在线
     *
     * @param machineId 机器节点标识
     * @return true 在线
     */
    public boolean isOnline(String machineId) {
        return this.get(machineId) != null;
    }

    /**
     * 在线节点集合
     *
     * @return 只读集合
     */
    public Set<String> onlineMachineIds() {
        return Collections.unmodifiableSet(channels.keySet());
    }

    /**
     * 在线数量
     *
     * @return 数量
     */
    public int size() {
        return channels.size();
    }

    private void checkAll() {
        for (Map.Entry<String, TunnelChannel> entry : channels.entrySet()) {
            TunnelChannel channel = entry.getValue();
            try {
                if (!channel.checkHeartbeat()) {
                    channels.remove(entry.getKey(), channel);
                    listeners.forEach(listener -> listener.offline(entry.getKey(), TunnelChannel.CLOSE_HEARTBEAT_TIMEOUT, "心跳超时"));
                }
            } catch (Exception e) {
                log.warn("节点[{}]心跳巡检异常", entry.getKey(), e);
            }
        }
    }
}
