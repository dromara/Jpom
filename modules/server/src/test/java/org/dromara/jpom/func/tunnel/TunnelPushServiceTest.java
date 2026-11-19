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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 插件端主动推送的服务端落点回归：只接受节点状态主题、校验大小、按最大年龄判定新鲜度。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelPushServiceTest {

    private TunnelPushService service(int maxAgeSeconds) {
        TunnelConfig config = new TunnelConfig();
        config.setPushMaxAgeSeconds(maxAgeSeconds);
        return new TunnelPushService(config);
    }

    @Test
    public void acceptsNodeStat() {
        TunnelPushService service = this.service(90);
        service.accept("m-1", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 2L, "{\"simpleStatus\":{\"cpu\":12.5}}"));

        TunnelPushService.Snapshot snapshot = service.latest("m-1");
        Assertions.assertNotNull(snapshot);
        Assertions.assertTrue(snapshot.getBody().contains("simpleStatus"));
        Assertions.assertEquals(2L, snapshot.getSeq());
        Assertions.assertNotNull(service.fresh("m-1"), "刚推送的数据应判定为新鲜");
        Assertions.assertEquals(1, service.snapshotSize());
        Assertions.assertEquals(1, service.receivedCount());
    }

    @Test
    public void ignoresUnknownTopicAndEmptyBody() {
        TunnelPushService service = this.service(90);
        service.accept("m-1", TunnelFrame.push("unknown-topic", 1L, "{}"));
        service.accept("m-1", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 2L, ""));
        service.accept("", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 3L, "{}"));
        Assertions.assertNull(service.latest("m-1"), "未知主题与空内容不应落快照");
        Assertions.assertNull(service.latest(""));
        Assertions.assertEquals(0, service.receivedCount());
    }

    @Test
    public void rejectsOversizedBody() {
        TunnelPushService service = this.service(90);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= TunnelConst.MAX_PUSH_BODY_BYTES; i++) {
            builder.append('a');
        }
        service.accept("m-1", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 1L, builder.toString()));
        Assertions.assertNull(service.latest("m-1"), "超过上限的推送必须丢弃");
    }

    @Test
    public void freshExpiresAndRemoveClears() {
        TunnelPushService service = this.service(90);
        service.accept("m-1", TunnelFrame.push(TunnelConst.TOPIC_NODE_STAT, 1L, "{}"));
        Assertions.assertNull(service.fresh("m-1", -1L), "负的年龄窗口必须视为过期");

        service.remove("m-1");
        Assertions.assertNull(service.latest("m-1"), "下线清理后不应继续复用快照");
        Assertions.assertNull(service.fresh("never-pushed"), "从未推送的节点没有快照");
    }
}
