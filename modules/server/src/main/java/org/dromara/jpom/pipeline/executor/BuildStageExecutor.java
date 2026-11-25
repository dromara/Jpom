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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.PipelineLog;
import org.dromara.jpom.pipeline.engine.PipelineWorkspace;
import org.dromara.jpom.pipeline.engine.StageExecutor;
import org.dromara.jpom.pipeline.engine.StageResult;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;
import org.dromara.jpom.util.AntPathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 构建阶段执行器
 * <p>
 * 配置（{@link PipelineStageDef#getConfig()}）：
 * <ul>
 *     <li>{@code cwd}：相对工作区的执行目录（多源时填 mount，例如 backend）</li>
 *     <li>{@code run}：要执行的命令（多行即脚本）</li>
 *     <li>{@code outputs}：产物匹配（Ant 风格，相对 cwd，可多个）</li>
 *     <li>{@code failOnNonZero}：非 0 退出码是否判失败（默认 true）</li>
 * </ul>
 * 产物收集到 {@code artifacts/<pipelineId>/<runNumber>/<stageId>/} 并写入上下文，
 * 后续发布阶段按 stageId 引用，不靠猜目录。
 *
 * @author jpom
 * @since 2.11.16
 */
public class BuildStageExecutor implements StageExecutor {

    private final PipelineCommandRunner commandRunner;
    private final PipelineWorkspace workspace;
    /**
     * 容器构建执行器（可选：单测里传 null 走本机路径）
     */
    private final PipelineContainerBuildRunner containerRunner;
    /**
     * 节点构建执行器（可选）
     */
    private final PipelineNodeBuildRunner nodeBuildRunner;

    public BuildStageExecutor(PipelineCommandRunner commandRunner, PipelineWorkspace workspace) {
        this(commandRunner, workspace, null, null);
    }

    public BuildStageExecutor(PipelineCommandRunner commandRunner, PipelineWorkspace workspace,
                              PipelineContainerBuildRunner containerRunner) {
        this(commandRunner, workspace, containerRunner, null);
    }

    public BuildStageExecutor(PipelineCommandRunner commandRunner, PipelineWorkspace workspace,
                              PipelineContainerBuildRunner containerRunner, PipelineNodeBuildRunner nodeBuildRunner) {
        this.commandRunner = commandRunner;
        this.workspace = workspace;
        this.containerRunner = containerRunner;
        this.nodeBuildRunner = nodeBuildRunner;
    }

    @Override
    public PipelineStageType type() {
        return PipelineStageType.BUILD;
    }

    @Override
    public StageResult execute(PipelineStageDef stage, PipelineContext context) throws Exception {
        long startTime = System.currentTimeMillis();
        Map<String, Object> config = stage.getConfig() == null ? new LinkedHashMap<>() : stage.getConfig();
        String cwd = StrUtil.nullToEmpty((String) config.get("cwd"));
        String script = (String) config.get("run");
        if (StrUtil.isBlank(script)) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "build stage without run command", startTime);
        }
        File workDir = StrUtil.isEmpty(cwd) ? context.getWorkspaceDir() : new File(context.getWorkspaceDir(), cwd);
        if (!workDir.isDirectory()) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "work dir not exists: " + cwd, startTime);
        }
        String executor = StrUtil.nullToEmpty((String) config.get("executor"));
        if ("container".equalsIgnoreCase(executor)) {
            return this.runContainer(stage, context, workDir, script, startTime);
        }
        if ("node".equalsIgnoreCase(executor)) {
            return this.runNode(stage, context, script, startTime);
        }
        Map<String, String> env = new LinkedHashMap<>(context.getVariables());
        env.put("PIPELINE_ID", StrUtil.nullToEmpty(context.getPipelineId()));
        env.put("PIPELINE_RUN", String.valueOf(context.getRunNumber()));
        env.put("PIPELINE_STAGE_ID", stage.getId());
        env.put("PIPELINE_WORKSPACE", FileUtil.getAbsolutePath(context.getWorkspaceDir()));
        // 构建环境变量（对齐在线构建的「构建环境变量」文本域）：阶段级配置优先于上下文变量
        env.putAll(parseEnvLines((String) config.get("env")));
        String logPrefix = "[" + stage.getName() + "] ";
        Consumer<String> sink = line -> context.getLog().info(logPrefix + line);
        int code = this.commandRunner.run(workDir, env, script, sink, context.getProcessObserver());
        boolean failOnNonZero = !Boolean.FALSE.equals(config.get("failOnNonZero"));
        if (code != 0 && failOnNonZero) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "命令退出码 " + code, startTime);
        }
        // 收集产物
        List<String> outputs = toStringList(config.get("outputs"));
        File artifactDir = this.workspace.artifactDir(context.getPipelineId(), context.getRunNumber(), stage.getId());
        int copied = 0;
        for (String pattern : outputs) {
            copied += this.copyMatched(workDir, pattern, artifactDir, context.getLog());
        }
        context.putArtifact(stage.getId(), FileUtil.getAbsolutePath(artifactDir));
        context.putVariable("PIPELINE_STAGE_" + stage.getId().toUpperCase() + "_ARTIFACT", FileUtil.getAbsolutePath(artifactDir));
        String message = "退出码 " + code + "，产物 " + copied + " 个";
        return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS, message, startTime);
    }

    /**
     * 容器构建：在基础镜像里执行命令，产物直接落到阶段产物目录
     */
    private StageResult runContainer(PipelineStageDef stage, PipelineContext context, File workDir,
                                     String script, long startTime) {
        if (this.containerRunner == null) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED,
                "container runner not available", startTime);
        }
        String image = StrUtil.nullToEmpty((String) stage.getConfig().get("image"));
        if (StrUtil.isEmpty(image)) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "容器构建未配置 image", startTime);
        }
        File artifactDir = this.workspace.artifactDir(context.getPipelineId(), context.getRunNumber(), stage.getId());
        Map<String, String> env = new LinkedHashMap<>(context.getVariables());
        env.put("PIPELINE_ID", StrUtil.nullToEmpty(context.getPipelineId()));
        env.put("PIPELINE_RUN", String.valueOf(context.getRunNumber()));
        env.putAll(parseEnvLines((String) stage.getConfig().get("env")));
        try {
            int code = this.containerRunner.run(context.getWorkspaceId(), image, workDir, script, artifactDir, env,
                line -> context.getLog().info("[" + stage.getName() + "]" + line), context);
            context.putArtifact(stage.getId(), FileUtil.getAbsolutePath(artifactDir));
            if (code != 0) {
                return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED,
                    "容器构建退出码 " + code, startTime);
            }
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS,
                "容器构建完成（" + image + "）", startTime);
        } catch (Exception e) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED,
                "容器构建异常：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), startTime);
        }
    }

    /**
     * 节点构建：把仓库与脚本下发到节点执行，产物下载回服务端
     */
    private StageResult runNode(PipelineStageDef stage, PipelineContext context, String script, long startTime) {
        if (this.nodeBuildRunner == null) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "node runner not available", startTime);
        }
        Map<String, Object> config = stage.getConfig();
        String nodeId = StrUtil.nullToEmpty((String) config.get("nodeId"));
        String repoId = StrUtil.nullToEmpty((String) config.get("repoId"));
        String ref = StrUtil.nullToEmpty((String) config.get("ref"));
        if (StrUtil.isEmpty(nodeId) || StrUtil.isEmpty(repoId)) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED,
                "节点构建需要配置 nodeId 与 repoId", startTime);
        }
        // 产物目录：与在线构建的「构建产物目录」同义（Ant 规则），留空表示打包整个工作区
        String resultDirFile = StrUtil.emptyToDefault((String) config.get("resultDirFile"), StrUtil.SLASH);
        try {
            String error = this.nodeBuildRunner.run(context, stage.getId(), nodeId, repoId, ref, script, resultDirFile,
                parseEnvLines((String) config.get("env")), context.getLog());
            if (error != null) {
                return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, error, startTime);
            }
            String artifactPath = FileUtil.getAbsolutePath(
                this.nodeBuildRunner.artifactRoot(context.getPipelineId(), context.getRunNumber()));
            context.putArtifact(stage.getId(), artifactPath);
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS,
                "节点构建完成：" + nodeId, startTime);
        } catch (Exception e) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED,
                "节点构建异常：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), startTime);
        }
    }

    /**
     * 按 Ant 规则拷贝匹配到的文件（相对路径保留），复用既有 AntPathUtil
     */
    private int copyMatched(File workDir, String pattern, File artifactDir, PipelineLog log) {
        List<String> paths = AntPathUtil.antPathMatcher(workDir, pattern);
        int count = 0;
        for (String relative : paths) {
            File source = FileUtil.file(workDir, relative);
            if (!source.exists()) {
                continue;
            }
            File target = FileUtil.file(artifactDir, relative);
            FileUtil.copy(source, target, true);
            count++;
        }
        if (count == 0) {
            log.info("产物规则未匹配到文件：" + pattern);
        }
        return count;
    }

    /**
     * 解析「构建环境变量」文本：每行一个 {@code KEY=VALUE}，支持 {@code export} 前缀、# 注释与空行
     * <p>
     * 与在线构建的文本域同一种写法，避免用户为同一件事学两套语法。
     * </p>
     *
     * @param text 文本
     * @return 变量表（永不为 null）
     */
    static Map<String, String> parseEnvLines(String text) {
        Map<String, String> env = new LinkedHashMap<>();
        if (StrUtil.isBlank(text)) {
            return env;
        }
        for (String rawLine : StrUtil.split(text, '\n')) {
            String line = StrUtil.trim(rawLine);
            if (StrUtil.isEmpty(line) || StrUtil.startWith(line, "#")) {
                continue;
            }
            if (StrUtil.startWithIgnoreCase(line, "export ")) {
                line = StrUtil.trim(StrUtil.subSuf(line, 7));
            }
            int index = line.indexOf('=');
            if (index <= 0) {
                continue;
            }
            String key = StrUtil.trim(StrUtil.subPre(line, index));
            String value = StrUtil.trim(StrUtil.subSuf(line, index + 1));
            if (StrUtil.isEmpty(key)) {
                continue;
            }
            env.put(key, value);
        }
        return env;
    }

    private static List<String> toStringList(Object value) {
        List<String> list = new ArrayList<>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item != null) {
                    list.add(String.valueOf(item));
                }
            }
        } else if (value != null) {
            list.addAll(StrUtil.splitTrim(String.valueOf(value), ','));
        }
        return CollUtil.removeEmpty(list);
    }
}