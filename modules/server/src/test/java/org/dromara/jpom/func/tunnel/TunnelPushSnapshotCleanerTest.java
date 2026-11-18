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

import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.TunnelFrame;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannel;
import org.dromara.jpom.transport.tunnel.ws.TunnelChannelRegistry;
import org.dromara.jpom.transport.tunnel.ws.TunnelServerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.WebSocketSession;

/**
 * 推送快照清理的回归：只有节点真正下线才丢弃快照，被新连接顶替时必须保留。
 * <p>
 * 「被新连接顶替」同样触发 {@code offline(CLOSE_REPLACED)}，如果无条件清理快照，
 * 资产监控下一轮读不到数据，会把仍在线并持续推送的节点判成「长时间未主动上报」。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelPushSnapshotCleanerTest {

    private static TunnelChannel channel(String sessionId) {
        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        Mockito.when(session.getId()).thenReturn(sessionId);
        Mockito.when(session.isOpen()).thenReturn(true);
        TunnelChannel channel = new TunnelChannel(session, new TunnelServerOptions());
        channel.setMachineId("m-1");
        return channel;
    }

    private static TunnelPushService pushService() {
        TunnelConfig config = new TunnelConfig();
        config.setPushMaxAgeSeconds(90);
        return new TunnelPushService(config);
    }

    @Test
    public void keepsSnapshotWhenConnectionReplaced() {
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        TunnelPushService pushService = this.pushService();
        registry.addListener(new TunnelPushSnapshotCleaner(registry, pushService));
        pushService.accept("m-1", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 1L, "{\"simpleStatus\":{}}"));

        TunnelChannel first = channel("s-1");
        registry.register(first);
        TunnelChannel second = channel("s-2");
        // 顶替旧连接：会触发 offline(m-1, CLOSE_REPLACED)
        registry.register(second);

        Assertions.assertTrue(registry.isOnline("m-1"), "新连接应在线");
        Assertions.assertNotNull(pushService.latest("m-1"), "被新连接顶替时不能清掉推送快照");
        Assertions.assertNotNull(pushService.fresh("m-1"), "快照必须仍然新鲜，监控不能闪成无法连接");
    }

    @Test
    public void removesSnapshotWhenNodeReallyOffline() {
        TunnelChannelRegistry registry = new TunnelChannelRegistry(new TunnelServerOptions());
        TunnelPushService pushService = this.pushService();
        registry.addListener(new TunnelPushSnapshotCleaner(registry, pushService));
        pushService.accept("m-1", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 1L, "{\"simpleStatus\":{}}"));

        TunnelChannel only = channel("s-1");
        registry.register(only);
        registry.remove(only);

        Assertions.assertFalse(registry.isOnline("m-1"));
        Assertions.assertNull(pushService.latest("m-1"), "节点真正下线后必须丢弃快照，避免续用过期数据");
    }
}
