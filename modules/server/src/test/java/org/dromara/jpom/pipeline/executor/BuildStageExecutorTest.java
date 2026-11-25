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
import org.dromara.jpom.pipeline.engine.PipelineWorkspace;
import org.dromara.jpom.pipeline.engine.StageResult;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 构建阶段执行器测试：退出码判定、产物收集、工作目录校验
 *
 * @author jpom
 * @since 2.11.16
 */
public class BuildStageExecutorTest {

    private PipelineStageDef stage(Map<String, Object> config) {
        PipelineStageDef def = new PipelineStageDef();
        def.setId("b1");
        def.setName("构建");
        def.setType(PipelineStageType.BUILD);
        def.setConfig(config);
        return def;
    }

    private PipelineContext context(File workspace) {
        PipelineContext context = new PipelineContext();
        context.setPipelineId("p1");
        context.setRunNumber(1);
        context.setWorkspaceDir(workspace);
        return context;
    }

    @Test
    public void collectsArtifactMatchedByPattern() throws Exception {
        File dataDir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-test-" + System.nanoTime());
        PipelineWorkspace workspace = new PipelineWorkspace(dataDir);
        File runDir = workspace.workspace("p1", 1);
        FileUtil.writeUtf8String("jar", new File(runDir, "target/app.jar"));
        FileUtil.writeUtf8String("txt", new File(runDir, "target/readme.txt"));

        // 假执行器：不改文件，只返回 0
        PipelineCommandRunner runner = (workDir, env, script, sink, processSink) -> 0;
        BuildStageExecutor executor = new BuildStageExecutor(runner, workspace);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("run", "mvn package");
        config.put("outputs", Arrays.asList("target/*.jar"));
        PipelineContext context = context(runDir);
        StageResult result = executor.execute(stage(config), context);

        Assertions.assertEquals(StageResult.Status.SUCCESS, result.getStatus(), result.getMessage());
        String artifactPath = context.getStageArtifacts().get("b1");
        Assertions.assertNotNull(artifactPath);
        Assertions.assertTrue(new File(artifactPath, "target/app.jar").isFile(), "匹配到的产物应被收集");
        Assertions.assertFalse(new File(artifactPath, "target/readme.txt").exists(), "未匹配的文件不应被收集");
    }

    @Test
    public void nonZeroExitFailsByDefaultButCanBeTolerated() throws Exception {
        File dataDir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-test-" + System.nanoTime());
        PipelineWorkspace workspace = new PipelineWorkspace(dataDir);
        File runDir = workspace.workspace("p1", 1);
        PipelineCommandRunner runner = (workDir, env, script, sink, processSink) -> 3;
        BuildStageExecutor executor = new BuildStageExecutor(runner, workspace);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("run", "exit 3");
        StageResult failed = executor.execute(stage(config), context(runDir));
        Assertions.assertEquals(StageResult.Status.FAILED, failed.getStatus());
        Assertions.assertTrue(failed.getMessage().contains("3"));

        config.put("failOnNonZero", false);
        StageResult tolerated = executor.execute(stage(config), context(runDir));
        Assertions.assertEquals(StageResult.Status.SUCCESS, tolerated.getStatus());
    }

    @Test
    public void missingRunCommandFails() throws Exception {
        File dataDir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-test-" + System.nanoTime());
        PipelineWorkspace workspace = new PipelineWorkspace(dataDir);
        File runDir = workspace.workspace("p1", 1);
        BuildStageExecutor executor = new BuildStageExecutor((w, e, s, sink, processSink) -> 0, workspace);
        StageResult result = executor.execute(stage(new LinkedHashMap<>()), context(runDir));
        Assertions.assertEquals(StageResult.Status.FAILED, result.getStatus());
    }

    @Test
    public void missingCwdFails() throws Exception {
        File dataDir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-test-" + System.nanoTime());
        PipelineWorkspace workspace = new PipelineWorkspace(dataDir);
        File runDir = workspace.workspace("p1", 1);
        BuildStageExecutor executor = new BuildStageExecutor((w, e, s, sink, processSink) -> 0, workspace);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("run", "ls");
        config.put("cwd", "../outside");
        StageResult result = executor.execute(stage(config), context(runDir));
        Assertions.assertEquals(StageResult.Status.FAILED, result.getStatus());
    }

    @Test
    public void environmentContainsPipelineVariables() throws Exception {
        File dataDir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-test-" + System.nanoTime());
        PipelineWorkspace workspace = new PipelineWorkspace(dataDir);
        File runDir = workspace.workspace("p1", 1);
        List<Map<String, String>> captured = new ArrayList<>();
        BuildStageExecutor executor = new BuildStageExecutor((workDir, env, script, sink, processSink) -> {
            captured.add(new LinkedHashMap<>(env));
            return 0;
        }, workspace);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("run", "echo hi");
        executor.execute(stage(config), context(runDir));
        Assertions.assertEquals("p1", captured.get(0).get("PIPELINE_ID"));
        Assertions.assertEquals("1", captured.get(0).get("PIPELINE_RUN"));
    }

    @Test
    public void stageEnvIsParsedAndOverridesContextVariables() throws Exception {
        File dataDir = FileUtil.mkdir(FileUtil.getTmpDirPath() + "/jpom-pipeline-test-" + System.nanoTime());
        PipelineWorkspace workspace = new PipelineWorkspace(dataDir);
        File runDir = workspace.workspace("p1", 1);
        List<Map<String, String>> captured = new ArrayList<>();
        BuildStageExecutor executor = new BuildStageExecutor((workDir, env, script, sink, processSink) -> {
            captured.add(new LinkedHashMap<>(env));
            return 0;
        }, workspace);
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("run", "echo hi");
        // 与在线构建的「构建环境变量」同一种写法：注释、export 前缀、等号两侧空格都要能解析
        config.put("env", "# 注释行\nFOO=bar\nexport BAZ = qux\n不合法的一行\n");
        PipelineContext context = context(runDir);
        context.putVariable("FOO", "from-context");
        StageResult result = executor.execute(stage(config), context);
        Assertions.assertEquals(StageResult.Status.SUCCESS, result.getStatus(), result.getMessage());
        Assertions.assertEquals("bar", captured.get(0).get("FOO"), "阶段环境变量应覆盖上下文变量");
        Assertions.assertEquals("qux", captured.get(0).get("BAZ"));
        Assertions.assertFalse(captured.get(0).containsKey("不合法的一行"));
    }

    @Test
    public void parseEnvLinesIgnoresBlankAndComment() {
        Map<String, String> env = BuildStageExecutor.parseEnvLines("\n# c\nA=1\nB = 2 \n");
        Assertions.assertEquals(2, env.size());
        Assertions.assertEquals("1", env.get("A"));
        Assertions.assertEquals("2", env.get("B"));
        Assertions.assertTrue(BuildStageExecutor.parseEnvLines(null).isEmpty());
    }
}