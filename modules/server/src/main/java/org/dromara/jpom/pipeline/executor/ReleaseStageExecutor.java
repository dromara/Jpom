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
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.dromara.jpom.build.MultiTargetRelease;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.StageExecutor;
import org.dromara.jpom.pipeline.engine.StageResult;
import org.dromara.jpom.pipeline.model.PipelineStageDef;
import org.dromara.jpom.pipeline.model.PipelineStageType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 发布阶段执行器：一个阶段可跨方式、多目标，顺序或并行，失败即停或继续
 * <p>
 * 配置：{@code from}（产物来源阶段）、{@code parallel}、{@code failFast}、
 * {@code targets}（{@link ReleaseTargetConfig} 数组）。
 * 顺序模式直接复用 {@link MultiTargetRelease}（老构建的多目标失败策略，已有单测），
 * 不另写一套「继续/终止/聚合」逻辑。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class ReleaseStageExecutor implements StageExecutor {

    private final PipelineReleaseInvoker invoker;

    public ReleaseStageExecutor(PipelineReleaseInvoker invoker) {
        this.invoker = invoker;
    }

    @Override
    public PipelineStageType type() {
        return PipelineStageType.RELEASE;
    }

    @Override
    public StageResult execute(PipelineStageDef stage, PipelineContext context) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> config = stage.getConfig() == null ? new LinkedHashMap<>() : stage.getConfig();
        List<ReleaseTargetConfig> targets = parseTargets(config.get("targets"));
        if (targets.isEmpty()) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "发布阶段未配置目标", startTime);
        }
        String fromStage = StrUtil.nullToEmpty((String) config.get("from"));
        String artifact = this.resolveArtifact(context, fromStage);
        if (artifact == null) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED, "没有可发布的产物", startTime);
        }
        boolean parallel = Boolean.TRUE.equals(config.get("parallel"));
        boolean failFast = !Boolean.FALSE.equals(config.get("failFast"));
        List<String> errors = parallel
            ? this.releaseParallel(stage, context, targets, artifact)
            : this.releaseSequential(stage, context, targets, artifact, failFast);
        if (!errors.isEmpty()) {
            return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.FAILED,
                "失败 " + errors.size() + "/" + targets.size() + "：" + String.join("; ", errors), startTime);
        }
        return StageResult.of(stage.getId(), stage.getName(), StageResult.Status.SUCCESS,
            "已发布 " + targets.size() + " 个目标" + (parallel ? "（并行）" : ""), startTime);
    }

    /**
     * 顺序发布：复用 MultiTargetRelease 的「继续/终止/失败聚合」
     */
    private List<String> releaseSequential(PipelineStageDef stage, PipelineContext context,
                                           List<ReleaseTargetConfig> targets, String artifact, boolean failFast) {
        // 用下标做 key，避免同一目标 id 在多方式下重复
        Map<String, ReleaseTargetConfig> indexed = new LinkedHashMap<>();
        for (int i = 0; i < targets.size(); i++) {
            indexed.put(String.valueOf(i), targets.get(i));
        }
        MultiTargetRelease.Outcome outcome = MultiTargetRelease.execute(String.join(",", indexed.keySet()), failFast,
            key -> this.releaseOne(stage, context, indexed.get(key), artifact));
        List<String> errors = new ArrayList<>();
        for (MultiTargetRelease.Failure failure : outcome.getFailures()) {
            ReleaseTargetConfig target = indexed.get(failure.getTargetId());
            errors.add(target.getId() + ": " + failure.getMessage());
        }
        if (outcome.isAborted() && outcome.getSkipped() > 0) {
            errors.add("失败即停，跳过 " + outcome.getSkipped() + " 个目标");
        }
        return errors;
    }

    /**
     * 并行发布：全部下发后等待收敛（并行下「失败即停」无法安全实现，改为全部结束后聚合）
     */
    private List<String> releaseParallel(PipelineStageDef stage, PipelineContext context,
                                         List<ReleaseTargetConfig> targets, String artifact) {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(targets.size(), 4));
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (ReleaseTargetConfig target : targets) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        this.releaseOne(stage, context, target, artifact);
                    } catch (Exception e) {
                        errors.add(target.getId() + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                    }
                }, pool));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.MINUTES);
        } catch (Exception e) {
            errors.add("并行发布异常：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            pool.shutdownNow();
        }
        return errors;
    }

    private void releaseOne(PipelineStageDef stage, PipelineContext context, ReleaseTargetConfig target, String artifact) throws Exception {
        ReleaseRequest request = new ReleaseRequest();
        request.setMethod(target.getMethod());
        request.setTargetId(target.getId());
        request.setArtifactPath(artifact);
        request.setFromStageId(StrUtil.nullToEmpty(target.getFrom()));
        request.setVariables(new LinkedHashMap<>(context.getVariables()));
        request.setPipelineId(context.getPipelineId());
        request.setRunNumber(context.getRunNumber());
        request.setWorkspaceId(context.getWorkspaceId());
        request.setExtra(target.getExtra());
        String error = this.invoker.release(request);
        if (error != null) {
            throw new IllegalStateException(error);
        }
        context.getLog().info("[" + stage.getName() + "] 目标发布完成：" + request.getTargetId());
    }

    private String resolveArtifact(PipelineContext context, String fromStage) {
        if (StrUtil.isNotEmpty(fromStage)) {
            return context.getStageArtifacts().get(fromStage);
        }
        // 默认取「最近一个非空产物」：命令阶段也会登记产物目录，但通常是空的，
        // 直接取最后一个会把空目录交给发布器（实测报「发布的文件或者文件夹为空」）
        String last = null;
        for (String path : context.getStageArtifacts().values()) {
            if (StrUtil.isEmpty(path)) {
                continue;
            }
            File dir = FileUtil.file(path);
            if (dir.exists() && !FileUtil.isDirEmpty(dir)) {
                last = path;
            }
        }
        if (last != null) {
            return last;
        }
        // 全部为空时仍返回最后一个，交给发布器给出明确错误
        for (String path : context.getStageArtifacts().values()) {
            last = path;
        }
        return last;
    }

    /**
     * 目标数组按 DTO 解析（不再对 Map/List 做 instanceof 分派）
     */
    private static List<ReleaseTargetConfig> parseTargets(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }
        List<ReleaseTargetConfig> list = JSON.parseArray(JSON.toJSONString(value), ReleaseTargetConfig.class);
        return list == null ? new ArrayList<>() : list;
    }
}