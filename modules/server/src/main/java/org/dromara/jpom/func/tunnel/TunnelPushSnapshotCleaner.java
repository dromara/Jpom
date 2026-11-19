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

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannel;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.dromara.jpom.transport.tunnel.ws.TunnelConnectionListener;

/**
 * 隧道上下线监听：节点真正下线后丢弃其推送快照，避免资产监控继续使用过期数据。
 * <p>
 * 注意「被新连接顶替」（{@code CLOSE_REPLACED}）也会触发下线事件，但此时节点仍然在线：
 * 推送快照属于机器节点而不是某条连接，这种情况下必须保留，否则监控下一轮会读不到数据，
 * 把节点误判为「长时间未主动上报」并闪一下无法连接。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
public class TunnelPushSnapshotCleaner implements TunnelConnectionListener {

    private final TunnelChannelRegistry registry;
    private final TunnelPushService tunnelPushService;

    public TunnelPushSnapshotCleaner(TunnelChannelRegistry registry, TunnelPushService tunnelPushService) {
        this.registry = registry;
        this.tunnelPushService = tunnelPushService;
    }

    @Override
    public void online(TunnelChannel channel) {
        log.info("节点 {} 反向隧道上线（agent={}, ip={}）", channel.machineId(), channel.getAgentVersion(), channel.getRemoteAddress());
    }

    @Override
    public void offline(String machineId, int code, String reason) {
        log.info("节点 {} 反向隧道下线（code={}, reason={}）", machineId, code, reason);
        if (this.registry.isOnline(machineId)) {
            log.debug("节点 {} 被新连接顶替，保留推送快照", machineId);
            return;
        }
        this.tunnelPushService.remove(machineId);
    }
}
