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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * 推送快照去重：同一份推送只能落库一次，否则节点统计日志会出现相同 monitorTime 的重复行。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class PushSnapshotTrackerTest {

    @Test
    public void firstConsumeOnlyOncePerSnapshot() {
        PushSnapshotTracker tracker = new PushSnapshotTracker();
        Assertions.assertTrue(tracker.firstConsume("m-1", 1000L), "第一份推送必须消费");
        Assertions.assertFalse(tracker.firstConsume("m-1", 1000L), "同一份推送（receivedAt 相同）不得重复消费");
        Assertions.assertTrue(tracker.firstConsume("m-1", 2000L), "新的推送必须重新消费");
        Assertions.assertTrue(tracker.firstConsume("m-2", 1000L), "不同节点互不影响");
        Assertions.assertEquals(2, tracker.size());
    }

    @Test
    public void clearAndRetain() {
        PushSnapshotTracker tracker = new PushSnapshotTracker();
        tracker.firstConsume("m-1", 1L);
        tracker.firstConsume("m-2", 1L);
        tracker.clear("m-1");
        Assertions.assertEquals(1, tracker.size(), "clear 后只应保留 m-2");
        Assertions.assertTrue(tracker.firstConsume("m-1", 1L), "clear 后同一份推送可以重新消费");

        tracker.retain(Collections.singletonList("m-2"));
        Assertions.assertEquals(1, tracker.size(), "retain 必须清掉已删除节点的记录");

        tracker.retain(null);
        Assertions.assertEquals(0, tracker.size(), "节点列表为空时必须清空，避免 Map 无界增长");

        tracker.firstConsume("m-3", 5L);
        tracker.retain(Arrays.asList("m-3", "m-4"));
        Assertions.assertEquals(1, tracker.size());
        Assertions.assertFalse(tracker.firstConsume(null, 1L), "空 machineId 不记录");
    }
}
