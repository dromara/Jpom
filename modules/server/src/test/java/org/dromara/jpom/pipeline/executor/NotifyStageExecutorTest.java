/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.StageResult;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通知阶段测试：多通知、单个失败不影响主流程、无目标视为成功
 *
 * @author jpom
 * @since 2.11.16
 */
public class NotifyStageExecutorTest {

    private PipelineStageDef stage(Object scripts) {
        PipelineStageDef def = new PipelineStageDef();
        def.setId("n1");
        def.setName("通知");
        def.setType(PipelineStageType.NOTIFY);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("scripts", scripts);
        config.put("event", "run_finished");
        def.setConfig(config);
        return def;
    }

    @Test
    public void notifiesEveryTargetAndToleratesSingleFailure() {
        AtomicInteger calls = new AtomicInteger();
        NotifyStageExecutor executor = new NotifyStageExecutor((target, event, payload) -> {
            calls.incrementAndGet();
            return "bad".equals(target) ? "send failed" : null;
        });
        StageResult result = executor.execute(stage(Arrays.asList("a", "bad", "c")), new PipelineContext());
        Assertions.assertEquals(StageResult.Status.SUCCESS, result.getStatus(), "通知失败不应让流水线失败");
        Assertions.assertEquals(3, calls.get(), "每个目标都要尝试");
        Assertions.assertTrue(result.getMessage().contains("2/3"), result.getMessage());
        Assertions.assertTrue(result.getMessage().contains("bad"), result.getMessage());
    }

    @Test
    public void emptyTargetsIsSuccess() {
        NotifyStageExecutor executor = new NotifyStageExecutor((target, event, payload) -> null);
        StageResult result = executor.execute(stage(Arrays.asList()), new PipelineContext());
        Assertions.assertEquals(StageResult.Status.SUCCESS, result.getStatus());
    }
}
