/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.BuildExecutorPoolService;
import org.dromara.jpom.build.ExecutionRegistry;
import org.dromara.jpom.core.exec.ProcessKillResult;
import org.dromara.jpom.core.exec.ProcessTree;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.PipelineEngine;
import org.dromara.jpom.pipeline.engine.PipelineLog;
import org.dromara.jpom.pipeline.engine.PipelineRunResult;
import org.dromara.jpom.pipeline.engine.PipelineWorkspace;
import org.dromara.jpom.pipeline.engine.StageExecutorRegistry;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.pipeline.executor.BuildStageExecutor;
import org.dromara.jpom.pipeline.executor.CommandStageExecutor;
import org.dromara.jpom.pipeline.executor.NotifyStageExecutor;
import org.dromara.jpom.pipeline.executor.PipelineContainerBuildRunner;
import org.dromara.jpom.pipeline.executor.PipelineNodeBuildRunner;
import org.dromara.jpom.pipeline.executor.PipelineNotifier;
import org.dromara.jpom.pipeline.executor.DefaultPipelineCommandRunner;
import org.dromara.jpom.pipeline.executor.PipelineReleaseInvoker;
import org.dromara.jpom.pipeline.executor.ReleaseStageExecutor;
import org.dromara.jpom.pipeline.model.PipelineDefinition;
import org.dromara.jpom.pipeline.model.PipelineInfoModel;
import org.dromara.jpom.pipeline.model.PipelineSource;
import org.dromara.jpom.pipeline.model.PipelineStageType;
import org.dromara.jpom.pipeline.model.PipelineRunModel;
import org.dromara.jpom.pipeline.repo.PipelineSourceSync;
import org.dromara.jpom.pipeline.service.PipelineInfoService;
import org.dromara.jpom.pipeline.service.PipelineRunService;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流水线运行编排（Spring 层）
 * <p>
 * 只做「装配 + 状态 + 取消登记」，真正的执行规则在 {@link PipelineEngine} 与各 StageExecutor：
 * <ul>
 *     <li>源同步 → 引擎跑阶段 → 汇总状态 → 写运行记录；</li>
 *     <li>运行登记进 {@link ExecutionRegistry}（与老构建同一套取消语义：确认进程树退出才算取消）；</li>
 *     <li>每次运行独立工作区与日志文件，互不串。</li>
 * </ul>
 *
 * @author jpom
 * @since 2.11.16
 */
@Slf4j
@Service
public class PipelineRunner {

    private final PipelineInfoService pipelineInfoService;
    private final PipelineRunService pipelineRunService;
    private final PipelineSourceSync sourceSync;
    private final PipelineReleaseInvoker releaseInvoker;
    private final BuildExecutorPoolService executorPoolService;
    private final PipelineNotifier notifier;
    private final PipelineContainerBuildRunner containerBuildRunner;
    private final PipelineNodeBuildRunner nodeBuildRunner;
    private final PipelineEngine engine = new PipelineEngine();

    /**
     * 正在运行的流水线（pipelineId）。
     * <p>
     * 取消登记（{@link ExecutionRegistry}）与流水线状态都以 pipelineId 为键：同一条流水线若允许并发运行，
     * 后一次登记会覆盖前一次的句柄（取消打到错误的运行），先结束的那次还会把后一次的句柄注销掉。
     * 因此这里以 pipelineId 做一次性占位，保证「同一条流水线同时只有一个运行」；
     * 不同流水线之间互不影响（各自独立工作区与日志）。
     * </p>
     */
    private final Set<String> activeRuns = ConcurrentHashMap.newKeySet();

    public PipelineRunner(PipelineInfoService pipelineInfoService,
                          PipelineRunService pipelineRunService,
                          PipelineSourceSync sourceSync,
                          PipelineReleaseInvoker releaseInvoker,
                          BuildExecutorPoolService executorPoolService,
                          PipelineNotifier notifier,
                          PipelineContainerBuildRunner containerBuildRunner,
                          PipelineNodeBuildRunner nodeBuildRunner) {
        this.pipelineInfoService = pipelineInfoService;
        this.pipelineRunService = pipelineRunService;
        this.sourceSync = sourceSync;
        this.releaseInvoker = releaseInvoker;
        this.executorPoolService = executorPoolService;
        this.notifier = notifier;
        this.containerBuildRunner = containerBuildRunner;
        this.nodeBuildRunner = nodeBuildRunner;
    }

    /**
     * 启动一次运行（异步）
     *
     * @param pipelineId 流水线 ID
     * @param user       操作人
     * @param remark     备注
     * @return 运行记录 ID
     */
    public String start(String pipelineId, UserModel user, String remark) {
        return this.start(pipelineId, user, remark, 0);
    }

    /**
     * 启动一次运行（异步，可指定触发方式）
     *
     * @param pipelineId  流水线 ID
     * @param user        操作人
     * @param remark      备注
     * @param triggerType 触发方式：0 手动 / 2 系统（开放接口） / 3 重跑
     * @return 运行记录 ID
     */
    public String start(String pipelineId, UserModel user, String remark, int triggerType) {
        PipelineInfoModel item = pipelineInfoService.getByKey(pipelineId);
        if (item == null) {
            throw new IllegalArgumentException("pipeline not found: " + pipelineId);
        }
        PipelineDefinition definition = item.definition();
        definition.check();
        // 先占位再动状态：并发两次点击「运行」时，第二次直接拒绝，避免两次运行互相覆盖取消句柄与状态
        if (!this.activeRuns.add(pipelineId)) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.pipeline_already_running.7b31"));
        }
        try {
            return this.startRun(item, definition, remark, triggerType);
        } catch (RuntimeException e) {
            // 启动阶段失败（读配置、建日志、线程池拒绝等）必须归还占位，否则该流水线会一直被判「正在运行」
            this.activeRuns.remove(pipelineId);
            throw e;
        }
    }

    /**
     * 真正启动（已持有 {@link #activeRuns} 占位）
     */
    private String startRun(PipelineInfoModel item, PipelineDefinition definition, String remark, int triggerType) {
        String pipelineId = item.getId();
        this.ensureExecutors();
        int runNumber = pipelineInfoService.nextRunNumber(pipelineId);
        PipelineWorkspace workspace = new PipelineWorkspace(new File(JpomApplication.getInstance().getDataPath()));
        File logFile = workspace.logFile(pipelineId, runNumber);
        LogRecorder logRecorder = LogRecorder.builder().file(logFile).build();

        PipelineRunModel run = new PipelineRunModel();
        run.setId(IdUtil.fastSimpleUUID());
        run.setWorkspaceId(item.getWorkspaceId());
        run.setPipelineId(pipelineId);
        run.setRunNumber(runNumber);
        run.setStatus(BuildStatus.Ing.getCode());
        run.setStatusMsg(StrUtil.emptyToDefault(remark, "运行中"));
        run.setSourcesSnapshot(JSON.toJSONString(definition.getSources()));
        run.setTriggerType(triggerType);
        run.setStartTime(System.currentTimeMillis());
        run.setHasLog(true);
        pipelineRunService.insert(run);
        pipelineInfoService.updateRunStatus(pipelineId, BuildStatus.Ing.getCode(), run.getStatusMsg(), run.getId());

        AtomicReference<ExecutionRegistry.Handle> handleRef = new AtomicReference<>();
        AtomicReference<PipelineContext> contextRef = new AtomicReference<>();
        AtomicBoolean killConfirmed = new AtomicBoolean(false);
        ExecutionRegistry.Handle handle = ExecutionRegistry.register(pipelineId, item.getName(), reason -> {
            ExecutionRegistry.Handle current = handleRef.get();
            Process process = current == null ? null : current.getProcess();
            log.info("pipeline[{}] cancel hook: handle={} process={} stage={}", pipelineId,
                current != null, process == null ? "null" : String.valueOf(ProcessTree.pidOf(process)),
                current == null ? "-" : current.getStage());
            if (process == null) {
                // 容器/节点构建没有本地进程：交给阶段注册的取消动作（删容器 / 下发节点取消并等回执）
                PipelineContext currentContext = contextRef.get();
                Function<String, Boolean> stageCancel = currentContext == null ? null : currentContext.getStageCancel();
                if (stageCancel != null) {
                    Boolean ok = stageCancel.apply(reason);
                    boolean confirmed = Boolean.TRUE.equals(ok);
                    // 这里必须同样登记「已确认」：否则运行线程仍按未确认落「异常关闭」（实测踩过）
                    killConfirmed.set(confirmed);
                    return confirmed;
                }
                killConfirmed.set(true);
                return true;
            }
            ProcessKillResult result = ProcessTree.kill(process);
            log.info("pipeline[{}] cancel hook kill result: {}", pipelineId, result);
            killConfirmed.set(result.isSuccess());
            return result.isSuccess();
        });
        handleRef.set(handle);
        handle.stage("source-sync");

        executorPoolService.execute(() -> this.run(item, definition, run, runNumber, workspace, logRecorder, handle, killConfirmed, contextRef));
        return run.getId();
    }

    /**
     * 取消运行：先登记取消意图，再由执行侧按进程树结果落终态
     *
     * @param pipelineId 流水线 ID
     * @return {@link ExecutionRegistry.CancelOutcome}
     */
    public ExecutionRegistry.CancelOutcome cancel(String pipelineId) {
        return ExecutionRegistry.requestCancel(pipelineId, "manual cancel");
    }

    /**
     * 运行日志文件（日志读取、下载与写入共用同一处路径规则）
     *
     * @param pipelineId 流水线 ID
     * @param runNumber  运行编号
     * @return 日志文件（可能尚未创建）
     */
    public File logFile(String pipelineId, int runNumber) {
        PipelineWorkspace workspace = new PipelineWorkspace(new File(JpomApplication.getInstance().getDataPath()));
        return workspace.logFile(pipelineId, runNumber);
    }

    /**
     * 实际执行
     */
    private void run(PipelineInfoModel item, PipelineDefinition definition, PipelineRunModel run, int runNumber,
                     PipelineWorkspace workspace, LogRecorder logRecorder, ExecutionRegistry.Handle handle,
                     AtomicBoolean killConfirmed, AtomicReference<PipelineContext> contextRef) {
        String pipelineId = item.getId();
        PipelineLog pipelineLog = new PipelineLog() {
            @Override
            public void info(String message) {
                logRecorder.system(message);
            }

            @Override
            public void error(String message) {
                logRecorder.systemError(message);
            }
        };
        PipelineContext context = new PipelineContext();
        contextRef.set(context);
        context.setPipelineId(pipelineId);
        context.setPipelineName(item.getName());
        context.setWorkspaceId(item.getWorkspaceId());
        context.setRunNumber(runNumber);
        context.setSources(definition.getSources());
        context.setWorkspaceDir(workspace.workspace(pipelineId, runNumber));
        context.setLog(pipelineLog);
        context.setProcessObserver(process -> {
            if (process == null) {
                handle.detachProcess();
            } else {
                log.info("pipeline[{}] stage process attached: pid={}", pipelineId, ProcessTree.pidOf(process));
                handle.attachProcess(process);
            }
        });
        BuildStatus finalStatus = BuildStatus.Error;
        String finalMsg = "failed";
        PipelineRunResult result = null;
        try {
            if (ExecutionRegistry.isCancelRequested(pipelineId)) {
                finalStatus = BuildStatus.Cancel;
                finalMsg = "cancelled before start";
            } else {
                handle.stage("source-sync");
                Map<String, String> commits = sourceSync.sync(pipelineId, definition.getSources(),
                    workspace.mountDirs(pipelineId, runNumber, definition.getSources()), new PipelineSourceSync.LogSink() {
                        @Override
                        public void info(String message) {
                            logRecorder.system(message);
                        }

                        @Override
                        public PrintWriter writer() {
                            return logRecorder.getPrintWriter();
                        }
                    });
                for (PipelineSource source : definition.getSources()) {
                    String commit = commits.get(source.getId());
                    String prefix = "PIPELINE_SOURCE_" + source.getId().toUpperCase();
                    context.putVariable(prefix + "_COMMIT", StrUtil.nullToEmpty(commit));
                    context.putVariable(prefix + "_DIR",
                        workspace.mountDir(pipelineId, runNumber, source).getAbsolutePath());
                }
                result = engine.run(definition, context, () -> ExecutionRegistry.isCancelRequested(pipelineId));
                // 取消优先判定：被杀的阶段会返回「命令退出码 143」，不能因此把本次运行记成失败
                boolean cancelRequested = ExecutionRegistry.isCancelRequested(pipelineId);
                if (cancelRequested) {
                    // 取消动作在 HTTP 线程里做（可能阻塞几秒），运行线程往往先返回；
                    // 这里等一小会儿取取消结果，避免把「已确认取消」误记成异常关闭。
                    long deadline = System.currentTimeMillis() + 10_000L;
                    while (!killConfirmed.get() && System.currentTimeMillis() < deadline) {
                        Thread.sleep(200L);
                    }
                }
                if (cancelRequested && killConfirmed.get()) {
                    finalStatus = BuildStatus.Cancel;
                    finalMsg = "cancelled（进程树已确认退出）";
                } else if (cancelRequested) {
                    // 取消意图已登记但没确认杀干净：如实记异常关闭，不假装取消成功
                    finalStatus = BuildStatus.AbnormalShutdown;
                    finalMsg = "cancelled but process not confirmed：进程可能残留，请检查";
                } else if (result.cancelledCount() > 0) {
                    finalStatus = BuildStatus.Cancel;
                    finalMsg = "cancelled";
                } else if (result.isSuccess()) {
                    finalStatus = BuildStatus.Success;
                    finalMsg = "success";
                } else {
                    finalStatus = BuildStatus.Error;
                    finalMsg = result.firstFailure() == null ? "failed" : result.firstFailure().getStageName() + ": " + result.firstFailure().getMessage();
                }
            }
        } catch (Exception e) {
            log.error("流水线运行异常：{}", pipelineId, e);
            logRecorder.systemError("运行异常：" + e.getMessage());
            finalStatus = BuildStatus.Error;
            finalMsg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        } finally {
            handle.stop();
            ExecutionRegistry.unregister(pipelineId);
            this.activeRuns.remove(pipelineId);
            if (result != null) {
                run.setStagesResult(JSON.toJSONString(result.getStages()));
            }
            run.setStatus(finalStatus.getCode());
            run.setStatusMsg(finalMsg);
            run.setEndTime(System.currentTimeMillis());
            run.setLogFileSize(logRecorder.size());
            pipelineRunService.updateById(run);
            pipelineInfoService.updateRunStatus(pipelineId, finalStatus.getCode(), finalMsg, run.getId());
            IoUtil.close(logRecorder);
        }
    }

    /**
     * 注册阶段执行器（首次运行时装配，避免依赖 Spring 初始化顺序）
     */
    private void ensureExecutors() {
        if (StageExecutorRegistry.get(PipelineStageType.BUILD) != null) {
            return;
        }
        PipelineWorkspace workspace = new PipelineWorkspace(new File(JpomApplication.getInstance().getDataPath()));
        DefaultPipelineCommandRunner commandRunner = new DefaultPipelineCommandRunner();
        StageExecutorRegistry.register(new BuildStageExecutor(commandRunner, workspace, containerBuildRunner, nodeBuildRunner));
        StageExecutorRegistry.register(new CommandStageExecutor(commandRunner, workspace, containerBuildRunner, nodeBuildRunner));
        StageExecutorRegistry.register(new ReleaseStageExecutor(releaseInvoker));
        StageExecutorRegistry.register(new NotifyStageExecutor(notifier));
    }
}