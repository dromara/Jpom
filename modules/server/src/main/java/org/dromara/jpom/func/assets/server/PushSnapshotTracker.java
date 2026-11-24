/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录每个节点「已消费到哪一份推送」，避免同一份推送在多个心跳周期里重复落库。
 * <p>
 * 插件端推送间隔（默认 30 秒）通常略大于服务端监控周期（默认 30 秒 + 采集耗时），
 * 因此相邻两个监控周期可能拿到同一份快照；若不做去重，节点统计日志里会出现
 * 多条 {@code monitorTime} 完全相同的记录（监控曲线重复描点、日志表翻倍）。
 *
 * @author jpom
 * @since 2026/11/18
 */
class PushSnapshotTracker {

    private final Map<String, Long> consumedAt = new ConcurrentHashMap<>();

    /**
     * 是否第一次消费这份推送（是则记录下来）
     *
     * @param machineId  机器节点标识
     * @param receivedAt 推送到达服务端的时间戳（{@code TunnelPushService.Snapshot#getReceivedAt()}）
     * @return true 这份推送还没消费过
     */
    boolean firstConsume(String machineId, long receivedAt) {
        if (machineId == null) {
            return false;
        }
        Long previous = consumedAt.put(machineId, receivedAt);
        return previous == null || previous != receivedAt;
    }

    /**
     * 清掉单个节点的记录（节点回退拉取、被删除时调用）
     *
     * @param machineId 机器节点标识
     */
    void clear(String machineId) {
        if (machineId != null) {
            consumedAt.remove(machineId);
        }
    }

    /**
     * 只保留仍然存在的节点，避免节点删除后 Map 无界增长
     *
     * @param machineIds 本轮机器列表
     */
    void retain(Collection<String> machineIds) {
        if (machineIds == null || machineIds.isEmpty()) {
            consumedAt.clear();
            return;
        }
        consumedAt.keySet().retainAll(machineIds);
    }

    /**
     * 当前记录的节点数（排障用）
     *
     * @return 数量
     */
    int size() {
        return consumedAt.size();
    }
}
