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

import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.StageResult;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 发布阶段执行器测试：多目标 / 跨方式 / 失败即停 / 失败继续 / 并行
 * <p>
 * 用假 invoker，不依赖真实 SSH/项目/节点：语义（哪些目标被下发、失败如何聚合）在这里确定性可测。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class ReleaseStageExecutorTest {

    private PipelineStageDef stage(List<Map<String, Object>> targets, boolean parallel, boolean failFast) {
        PipelineStageDef def = new PipelineStageDef();
        def.setId("r1");
        def.setName("发布");
        def.setType(PipelineStageType.RELEASE);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("targets", targets);
        config.put("parallel", parallel);
        config.put("failFast", failFast);
        def.setConfig(config);
        return def;
    }

    private Map<String, Object> target(int method, String id) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("method", method);
        map.put("id", id);
        return map;
    }

    private PipelineContext context() {
        PipelineContext context = new PipelineContext();
        context.setPipelineId("p1");
        context.setRunNumber(1);
        context.setWorkspaceDir(FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-ws-" + System.nanoTime()));
        context.putArtifact("b1", "/tmp/artifact");
        return context;
    }

    @Test
    public void releasesEveryTargetInSequentialMode() throws Exception {
        ConcurrentLinkedQueue<String> released = new ConcurrentLinkedQueue<>();
        ReleaseStageExecutor executor = new ReleaseStageExecutor(request -> {
            released.add(request.getTargetId());
            return null;
        });
        StageResult result = executor.execute(
            stage(Arrays.asList(target(3, "ssh-1"), target(7, "proj-1")), false, true), context());

        Assertions.assertEquals(StageResult.Status.SUCCESS, result.getStatus(), result.getMessage());
        Assertions.assertEquals(Arrays.asList("ssh-1", "proj-1"), new ArrayList<>(released));
    }

    @Test
    public void failFastStopsRemainingTargets() throws Exception {
        ConcurrentLinkedQueue<String> released = new ConcurrentLinkedQueue<>();
        ReleaseStageExecutor executor = new ReleaseStageExecutor(request -> {
            released.add(request.getTargetId());
            return "ssh-1".equals(request.getTargetId()) ? "restart failed" : null;
        });
        StageResult result = executor.execute(
            stage(Arrays.asList(target(3, "ssh-1"), target(3, "ssh-2")), false, true), context());

        Assertions.assertEquals(StageResult.Status.FAILED, result.getStatus());
        Assertions.assertEquals(Collections.singletonList("ssh-1"), new ArrayList<>(released), "失败即停后不应再发下一个目标");
        Assertions.assertTrue(result.getMessage().contains("ssh-1"), result.getMessage());
    }

    @Test
    public void continueOnFailureStillReleasesRemainingButRunFails() throws Exception {
        ConcurrentLinkedQueue<String> released = new ConcurrentLinkedQueue<>();
        ReleaseStageExecutor executor = new ReleaseStageExecutor(request -> {
            released.add(request.getTargetId());
            return "ssh-1".equals(request.getTargetId()) ? "boom" : null;
        });
        StageResult result = executor.execute(
            stage(Arrays.asList(target(3, "ssh-1"), target(3, "ssh-2")), false, false), context());

        Assertions.assertEquals(StageResult.Status.FAILED, result.getStatus());
        Assertions.assertEquals(Arrays.asList("ssh-1", "ssh-2"), new ArrayList<>(released));
    }

    @Test
    public void parallelModeReleasesAllTargets() throws Exception {
        ConcurrentLinkedQueue<String> released = new ConcurrentLinkedQueue<>();
        ReleaseStageExecutor executor = new ReleaseStageExecutor(request -> {
            released.add(request.getTargetId());
            return null;
        });
        StageResult result = executor.execute(
            stage(Arrays.asList(target(3, "a"), target(3, "b"), target(3, "c")), true, true), context());

        Assertions.assertEquals(StageResult.Status.SUCCESS, result.getStatus(), result.getMessage());
        Assertions.assertEquals(3, released.size());
    }

    @Test
    public void missingTargetsOrArtifactFails() throws Exception {
        ReleaseStageExecutor executor = new ReleaseStageExecutor(request -> null);
        Assertions.assertEquals(StageResult.Status.FAILED,
            executor.execute(stage(new ArrayList<>(), false, true), context()).getStatus());

        PipelineContext noArtifact = new PipelineContext();
        noArtifact.setPipelineId("p1");
        noArtifact.setRunNumber(1);
        Assertions.assertEquals(StageResult.Status.FAILED,
            executor.execute(stage(Collections.singletonList(target(3, "ssh-1")), false, true), noArtifact).getStatus());
    }
}
