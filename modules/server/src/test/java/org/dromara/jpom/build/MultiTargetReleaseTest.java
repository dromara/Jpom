/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 多目标发布失败策略：继续 / 终止 / 失败聚合（2.11.14.6 SSH 项目多选发布的守卫）
 *
 * @author bwcx_jzy
 * @since 2026/11/23
 */
public class MultiTargetReleaseTest {

    @Test
    public void allSuccessReleasesEveryTargetInOrder() {
        List<String> released = new ArrayList<>();
        MultiTargetRelease.Outcome outcome = MultiTargetRelease.execute("p1,p2", true, released::add);
        Assertions.assertEquals(Arrays.asList("p1", "p2"), released);
        Assertions.assertFalse(outcome.hasFailure());
        Assertions.assertFalse(outcome.isAborted());
        Assertions.assertEquals(0, outcome.getSkipped());
    }

    @Test
    public void failFastStopsAtFirstFailureAndSkipsRemaining() {
        List<String> released = new ArrayList<>();
        MultiTargetRelease.Outcome outcome = MultiTargetRelease.execute("p1,p2,p3", true, targetId -> {
            if ("p2".equals(targetId)) {
                throw new IllegalStateException("connect timeout");
            }
            released.add(targetId);
        });
        // p3 不能被发布：开启「任一目标失败即终止」后只剩失败与跳过
        Assertions.assertEquals(Arrays.asList("p1"), released);
        Assertions.assertTrue(outcome.isAborted());
        Assertions.assertEquals(1, outcome.getSkipped());
        Assertions.assertEquals(1, outcome.getFailures().size());
        Assertions.assertEquals("p2", outcome.getFailures().get(0).getTargetId());
        Assertions.assertEquals("connect timeout", outcome.getFailures().get(0).getMessage());
    }

    @Test
    public void continueModePublishesRemainingTargetsButKeepsFailure() {
        List<String> released = new ArrayList<>();
        MultiTargetRelease.Outcome outcome = MultiTargetRelease.execute("p1,p2,p3", false, targetId -> {
            if ("p2".equals(targetId)) {
                throw new IllegalStateException("no such project");
            }
            released.add(targetId);
        });
        // 关闭开关：p3 必须照常发布，但失败仍然要上报（最终由调用方判定发布失败）
        Assertions.assertEquals(Arrays.asList("p1", "p3"), released);
        Assertions.assertFalse(outcome.isAborted());
        Assertions.assertEquals(0, outcome.getSkipped());
        Assertions.assertEquals(1, outcome.getFailures().size());
        Assertions.assertTrue(outcome.hasFailure());
    }

    @Test
    public void blankTargetIdsReleasesNothing() {
        List<String> released = new ArrayList<>();
        MultiTargetRelease.Outcome nullOutcome = MultiTargetRelease.execute(null, true, released::add);
        MultiTargetRelease.Outcome blankOutcome = MultiTargetRelease.execute("  ,  ", true, released::add);
        Assertions.assertTrue(released.isEmpty());
        Assertions.assertFalse(nullOutcome.hasFailure());
        Assertions.assertFalse(blankOutcome.hasFailure());
        Assertions.assertFalse(nullOutcome.isAborted());
        Assertions.assertFalse(blankOutcome.isAborted());
    }

    @Test
    public void exceptionWithoutMessageFallsBackToClassName() {
        MultiTargetRelease.Outcome outcome = MultiTargetRelease.execute("p1", true, targetId -> {
            throw new NullPointerException();
        });
        Assertions.assertEquals("NullPointerException", outcome.getFailures().get(0).getMessage());
    }

    @Test
    public void trimsWhitespaceAroundTargetIds() {
        List<String> released = new ArrayList<>();
        MultiTargetRelease.execute(" p1 , p2 ", true, released::add);
        Assertions.assertEquals(Arrays.asList("p1", "p2"), released);
    }
}
