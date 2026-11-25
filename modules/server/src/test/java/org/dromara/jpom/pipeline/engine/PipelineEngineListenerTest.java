/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import org.dromara.jpom.pipeline.model.PipelineDefinition;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 引擎监听测试：状态切换事件必须覆盖「成功 / 失败 / 跳过 / 取消」四种阶段结果
 * <p>
 * 事件钩子只在这些回调上触发，如果监听漏了跳过/取消，事件通知就会缺一条，
 * 用户在群里只能看到「开始」看不到「为什么没跑」，所以用单测锁死。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineEngineListenerTest {

    @AfterEach
    public void cleanup() {
        StageExecutorRegistry.clearForTest();
    }

    private PipelineStageDef stage(String id, PipelineStageType type, boolean enabled) {
        PipelineStageDef def = new PipelineStageDef();
        def.setId(id);
        def.setName("阶段" + id);
        def.setType(type);
        def.setEnabled(enabled);
        return def;
    }

    private void register(PipelineStageType type, StageResult.Status status) {
        StageExecutorRegistry.register(new StageExecutor() {
            @Override
            public PipelineStageType type() {
                return type;
            }

            @Override
            public StageResult execute(PipelineStageDef stage, PipelineContext context) {
                return StageResult.of(stage.getId(), stage.getName(), status, null, System.currentTimeMillis());
            }
        });
    }

    @Test
    public void listenerSeesStartAndEndForExecutedStages() {
        register(PipelineStageType.BUILD, StageResult.Status.SUCCESS);
        register(PipelineStageType.RELEASE, StageResult.Status.FAILED);
        List<String> events = new ArrayList<>();
        PipelineDefinition definition = new PipelineDefinition();
        definition.setStages(new ArrayList<>(Arrays.asList(
            stage("s1", PipelineStageType.BUILD, true),
            stage("s2", PipelineStageType.RELEASE, true))));
        new PipelineEngine().run(definition, new PipelineContext(), PipelineEngine.CancelSignal.NONE,
            new PipelineEngine.StageListener() {
                @Override
                public void onStageStart(PipelineStageDef stage) {
                    events.add("start:" + stage.getId());
                }

                @Override
                public void onStageEnd(PipelineStageDef stage, StageResult result) {
                    events.add("end:" + stage.getId() + ":" + result.getStatus());
                }
            });
        Assertions.assertEquals(Arrays.asList("start:s1", "end:s1:SUCCESS", "start:s2", "end:s2:FAILED"), events);
    }

    @Test
    public void listenerSeesSkippedAndCancelledStages() {
        register(PipelineStageType.BUILD, StageResult.Status.SUCCESS);
        register(PipelineStageType.RELEASE, StageResult.Status.SUCCESS);
        List<String> events = new ArrayList<>();
        PipelineDefinition definition = new PipelineDefinition();
        definition.setStages(new ArrayList<>(Arrays.asList(
            stage("s1", PipelineStageType.BUILD, true),
            stage("s2", PipelineStageType.NOTIFY, false),
            stage("s3", PipelineStageType.RELEASE, true))));
        // s1 执行成功；s2 禁用；s3 因取消信号被标 CANCELLED
        new PipelineEngine().run(definition, new PipelineContext(),
            () -> false, new PipelineEngine.StageListener() {
                @Override
                public void onStageEnd(PipelineStageDef stage, StageResult result) {
                    events.add(stage.getId() + ":" + result.getStatus());
                }
            });
        Assertions.assertEquals(Arrays.asList("s1:SUCCESS", "s2:SKIPPED", "s3:SUCCESS"), events,
            "禁用阶段也要有结束事件（不能静默）");

        events.clear();
        definition.setStages(new ArrayList<>(Arrays.asList(stage("s9", PipelineStageType.BUILD, true))));
        new PipelineEngine().run(definition, new PipelineContext(), () -> true,
            new PipelineEngine.StageListener() {
                @Override
                public void onStageEnd(PipelineStageDef stage, StageResult result) {
                    events.add(stage.getId() + ":" + result.getStatus());
                }
            });
        Assertions.assertEquals(Arrays.asList("s9:CANCELLED"), events, "取消后的阶段也要有结束事件");
    }

    @Test
    public void oldSignatureStillWorksWithoutListener() {
        register(PipelineStageType.BUILD, StageResult.Status.SUCCESS);
        PipelineDefinition definition = new PipelineDefinition();
        definition.setStages(new ArrayList<>(Arrays.asList(stage("s1", PipelineStageType.BUILD, true))));
        PipelineRunResult result = new PipelineEngine().run(definition, new PipelineContext(),
            PipelineEngine.CancelSignal.NONE);
        Assertions.assertTrue(result.isSuccess());
    }
}
