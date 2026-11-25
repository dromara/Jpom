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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 流水线引擎行为测试：顺序、失败策略、取消、禁用、类型未注册
 * <p>
 * 这些规则如果只靠「点一次真实流水线」验证，回归时根本发现不了；
 * 引擎不依赖 Spring/数据库，就是为了让它们在这里确定性可测。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineEngineTest {

    @AfterEach
    public void cleanup() {
        StageExecutorRegistry.clearForTest();
    }

    private PipelineStageDef stage(String id, PipelineStageType type, boolean failFast) {
        PipelineStageDef def = new PipelineStageDef();
        def.setId(id);
        def.setName("阶段" + id);
        def.setType(type);
        def.setFailFast(failFast);
        return def;
    }

    private PipelineDefinition definition(PipelineStageDef... stages) {
        PipelineDefinition definition = new PipelineDefinition();
        definition.setStages(new ArrayList<>(Arrays.asList(stages)));
        return definition;
    }

    private void registerRecorder(PipelineStageType type, List<String> order, AtomicBoolean shouldFail) {
        StageExecutorRegistry.register(new StageExecutor() {
            @Override
            public PipelineStageType type() {
                return type;
            }

            @Override
            public StageResult execute(PipelineStageDef stage, PipelineContext context) {
                order.add(stage.getId());
                if (shouldFail.get()) {
                    return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "boom", System.currentTimeMillis());
                }
                return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS, null, System.currentTimeMillis());
            }
        });
    }

    @Test
    public void runsStagesInDefinitionOrder() {
        List<String> order = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean(false);
        registerRecorder(PipelineStageType.BUILD, order, fail);
        registerRecorder(PipelineStageType.COMMAND, order, fail);
        registerRecorder(PipelineStageType.RELEASE, order, fail);

        PipelineRunResult result = new PipelineEngine().run(
            definition(stage("s1", PipelineStageType.BUILD, true),
                stage("s2", PipelineStageType.COMMAND, true),
                stage("s3", PipelineStageType.RELEASE, true)),
            new PipelineContext(), PipelineEngine.CancelSignal.NONE);

        Assertions.assertEquals(Arrays.asList("s1", "s2", "s3"), order);
        Assertions.assertTrue(result.isSuccess(), result.summary());
    }

    @Test
    public void failFastStopsRemainingStages() {
        List<String> order = new ArrayList<>();
        AtomicBoolean fail = new AtomicBoolean(true);
        registerRecorder(PipelineStageType.BUILD, order, fail);
        registerRecorder(PipelineStageType.RELEASE, order, new AtomicBoolean(false));

        PipelineRunResult result = new PipelineEngine().run(
            definition(stage("s1", PipelineStageType.BUILD, true),
                stage("s2", PipelineStageType.RELEASE, true)),
            new PipelineContext(), PipelineEngine.CancelSignal.NONE);

        Assertions.assertEquals(Collections.singletonList("s1"), order, "失败即停后不应再执行 s2");
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(1, result.failedCount());
        Assertions.assertEquals(StageResult.Status.SKIPPED, result.getStages().get(1).getStatus());
        Assertions.assertNotNull(result.firstFailure());
    }

    @Test
    public void failureWithoutFailFastStillRunsRemainingButRunFails() {
        List<String> order = new ArrayList<>();
        registerRecorder(PipelineStageType.BUILD, order, new AtomicBoolean(true));
        registerRecorder(PipelineStageType.NOTIFY, order, new AtomicBoolean(false));

        PipelineRunResult result = new PipelineEngine().run(
            definition(stage("s1", PipelineStageType.BUILD, false),
                stage("s2", PipelineStageType.NOTIFY, true)),
            new PipelineContext(), PipelineEngine.CancelSignal.NONE);

        Assertions.assertEquals(Arrays.asList("s1", "s2"), order, "失败继续策略下后续阶段仍要执行");
        Assertions.assertFalse(result.isSuccess(), "有阶段失败则整次运行不成功");
        Assertions.assertEquals(1, result.failedCount());
    }

    @Test
    public void cancelMarksRemainingAsCancelledNeverSuccess() {
        List<String> order = new ArrayList<>();
        registerRecorder(PipelineStageType.BUILD, order, new AtomicBoolean(false));
        registerRecorder(PipelineStageType.RELEASE, order, new AtomicBoolean(false));
        AtomicBoolean cancelled = new AtomicBoolean(true);

        PipelineRunResult result = new PipelineEngine().run(
            definition(stage("s1", PipelineStageType.BUILD, true),
                stage("s2", PipelineStageType.RELEASE, true)),
            new PipelineContext(), cancelled::get);

        Assertions.assertTrue(order.isEmpty(), "已取消的流水线不应再进入任何阶段");
        Assertions.assertEquals(2, result.cancelledCount());
        Assertions.assertFalse(result.isSuccess(), "被取消的运行不能算成功");
    }

    @Test
    public void disabledStageIsSkipped() {
        List<String> order = new ArrayList<>();
        registerRecorder(PipelineStageType.BUILD, order, new AtomicBoolean(false));
        PipelineStageDef disabled = stage("s2", PipelineStageType.NOTIFY, true);
        disabled.setEnabled(false);

        PipelineRunResult result = new PipelineEngine().run(
            definition(stage("s1", PipelineStageType.BUILD, true), disabled),
            new PipelineContext(), PipelineEngine.CancelSignal.NONE);

        Assertions.assertEquals(Collections.singletonList("s1"), order);
        Assertions.assertEquals(StageResult.Status.SKIPPED, result.getStages().get(1).getStatus());
        Assertions.assertEquals("disabled", result.getStages().get(1).getMessage());
    }

    @Test
    public void unregisteredStageTypeFailsInsteadOfSilentlySkipping() {
        PipelineRunResult result = new PipelineEngine().run(
            definition(stage("s1", PipelineStageType.APPROVAL, true)),
            new PipelineContext(), PipelineEngine.CancelSignal.NONE);

        Assertions.assertEquals(StageResult.Status.FAILED, result.getStages().get(0).getStatus());
        Assertions.assertTrue(result.getStages().get(0).getMessage().contains("未注册"));
        Assertions.assertFalse(result.isSuccess());
    }
}