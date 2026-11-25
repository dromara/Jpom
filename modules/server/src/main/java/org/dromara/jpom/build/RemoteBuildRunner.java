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

import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.util.CommandUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentPackageResolver;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.enums.BuildBuildNodeState;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.transport.IProxyWebSocket;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 远程构建执行器：把「拉代码 + 执行命令 + 收集产物」下发给 Go 插件端节点执行。
 * <p>
 * 协议见 {@code archive/2026-11-build-node-on-go-agent/PROTOCOL.md}。本类的关键职责：
 * 1) 组装 job 并下发（节点繁忙排队、能力不足直接失败）；
 * 2) 消费节点事件流，日志原样落到 Server 构建日志文件（前端/历史/下载全部零改动）；
 * 3) 三个阶段各自等待对应 stage 结束，保持 webhook 与状态语义与本地构建一致；
 * 4) 拉取产物 zip、校验、解压成与本地构建完全相同的 result 目录结构。
 *
 * @author jpom
 * @since 2.11.15
 */
@Slf4j
public class RemoteBuildRunner implements BuildRunner {

    /**
     * 拉取代码阶段等待上限
     */
    private static final long PULL_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    /**
     * 构建命令阶段等待上限
     */
    private static final long EXECUTE_TIMEOUT_MS = TimeUnit.HOURS.toMillis(3);
    /**
     * 打包/产物阶段等待上限
     */
    private static final long PACKAGE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    /**
     * 节点繁忙时的排队重试次数与间隔
     */
    private static final int BUSY_RETRY_COUNT = 60;
    private static final long BUSY_RETRY_INTERVAL_MS = TimeUnit.SECONDS.toMillis(2);
    /**
     * 产物拉取重试次数（大产物在抖动链路上首轮被截断很常见，配合断点续传重试成本很低）
     */
    private static final int DOWNLOAD_RETRY_COUNT = 5;
    /**
     * 传给节点的默认任务超时（秒），节点侧 watchdog 用
     */
    private static final long DEFAULT_JOB_TIMEOUT_SEC = TimeUnit.HOURS.toSeconds(2);

    private final BuildRunnerOwner owner;
    private final TaskData taskData;
    private final BuildExtraModule buildExtraModule;
    private final LogRecorder logRecorder;
    private final String logId;
    private final String buildNodeId;

    private NodeModel nodeModel;
    private MachineNodeModel machineNodeModel;
    private BuildNodeClient client;
    private RemoteBuildSession session;
    private IProxyWebSocket socket;

    private boolean started;
    private String startError;
    /**
     * 节点返回的产物信息
     */
    private JSONObject artifact;

    public RemoteBuildRunner(BuildRunnerOwner owner, TaskData taskData, BuildExtraModule buildExtraModule,
                      LogRecorder logRecorder, String logId, String buildNodeId) {
        this.owner = owner;
        this.taskData = taskData;
        this.buildExtraModule = buildExtraModule;
        this.logRecorder = logRecorder;
        this.logId = logId;
        this.buildNodeId = buildNodeId;
    }

    // ------------------------------------------------------------------ 阶段实现

    @Override
    public String startReady() {
        logRecorder.system(I18nMessageUtil.get("i18n.build_node_start_ready.8c1d"), this.buildNodeId);
        Integer delay = taskData.delay;
        if (delay != null && delay > 0) {
            logRecorder.system(I18nMessageUtil.get("i18n.wait_for_seconds.ff7b"), delay);
            try {
                TimeUnit.SECONDS.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return InterruptedMsg();
            }
        }
        // 远程构建时源码目录在节点上，BUILD_SOURCE_FILE 由节点回填；这里只补与路径无关的默认变量
        EnvironmentMapBuilder environment = taskData.environmentMapBuilder;
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        environment.put("BUILD_ID", buildInfoModel.getId());
        environment.put("BUILD_NAME", buildInfoModel.getName());
        environment.put("BUILD_NUMBER_ID", String.valueOf(buildInfoModel.getBuildId()));
        environment.put("BUILD_ORIGINAL_RESULT_DIR_FILE", buildInfoModel.getResultDirFile());
        environment.put("BUILD_CONFIG_BRANCH_NAME", buildInfoModel.getBranchName());
        return null;
    }

    private String InterruptedMsg() {
        return I18nMessageUtil.get("i18n.system_interruption.e37c");
    }

    @Override
    public String pull() {
        String error = this.ensureStarted();
        if (error != null) {
            return error;
        }
        try {
            int code = session.awaitStage("pull", PULL_TIMEOUT_MS);
            this.mergeNodeEnv();
            String state = session.getState();
            if (session.isExited() && BuildBuildNodeState.SKIPPED.equals(state)) {
                String message = StrUtil.emptyToDefault(session.getExitMessage(),
                    I18nMessageUtil.get("i18n.build_node_skip_no_changes.3f7c"));
                logRecorder.systemError(message);
                throw new DiyInterruptException(message);
            }
            if (code != 0) {
                return StrUtil.emptyToDefault(session.stageMessage("pull"),
                    StrUtil.format(I18nMessageUtil.get("i18n.command_non_zero_exit_code.a6e1"), code));
            }
            // 回填本次实际拉取的 commit（差异构建下次比较用）与分支
            if (StrUtil.isNotEmpty(session.getPullCommitId())) {
                taskData.repositoryLastCommitId = session.getPullCommitId();
                taskData.repositoryLastCommitMsg = StrUtil.emptyToDefault(session.getPullBranch(), StrUtil.EMPTY);
            }
            return null;
        } catch (DiyInterruptException e) {
            throw e;
        } catch (Exception e) {
            return this.handleException("pull", e);
        }
    }

    @Override
    public String executeCommand() {
        String error = this.ensureStarted();
        if (error != null) {
            return error;
        }
        try {
            int code = session.awaitStage("executeCommand", EXECUTE_TIMEOUT_MS);
            String state = session.getState();
            if (session.isExited() && BuildBuildNodeState.SKIPPED.equals(state)) {
                throw new DiyInterruptException(StrUtil.emptyToDefault(session.getExitMessage(),
                    I18nMessageUtil.get("i18n.build_node_skip_no_changes.3f7c")));
            }
            if (code != 0) {
                return StrUtil.emptyToDefault(session.stageMessage("executeCommand"),
                    StrUtil.format(I18nMessageUtil.get("i18n.command_non_zero_exit_code.a6e1"), code));
            }
            return null;
        } catch (DiyInterruptException e) {
            throw e;
        } catch (Exception e) {
            return this.handleException("executeCommand", e);
        }
    }

    @Override
    public String packageFile() {
        String error = this.ensureStarted();
        if (error != null) {
            return error;
        }
        try {
            session.awaitExit(PACKAGE_TIMEOUT_MS);
        } catch (Exception e) {
            return this.handleException("packageFile", e);
        }
        String state = session.getState();
        if (BuildBuildNodeState.SKIPPED.equals(state)) {
            throw new DiyInterruptException(StrUtil.emptyToDefault(session.getExitMessage(),
                I18nMessageUtil.get("i18n.build_node_skip_no_changes.3f7c")));
        }
        if (!BuildBuildNodeState.SUCCESS.equals(state)) {
            return StrUtil.emptyToDefault(session.getExitMessage(),
                StrUtil.format(I18nMessageUtil.get("i18n.build_node_build_failed_state.6d31"), state));
        }
        // 再合并一次节点环境变量：env 帧可能晚于 pull 阶段到达
        this.mergeNodeEnv();
        this.artifact = session.getResult();
        if (this.artifact == null) {
            return I18nMessageUtil.get("i18n.build_node_result_missing.9e0f");
        }
        try {
            return this.downloadArtifact();
        } catch (Exception e) {
            logRecorder.error(I18nMessageUtil.get("i18n.build_node_pull_artifact_failed.4a86"), e);
            return StrUtil.emptyToDefault(e.getMessage(), I18nMessageUtil.get("i18n.build_node_pull_artifact_failed.4a86"));
        }
    }

    @Override
    public boolean cancel(String reason) {
        boolean confirmed = false;
        try {
            if (client != null && session != null && !session.isExited()) {
                client.cancel(logId);
                // 等节点回执：节点侧对进程组发 TERM → 5s → KILL，等它真正结束再判「已取消」，
                // 否则就是历史上的「发射后不管」——服务端已放弃，节点进程还在跑。
                session.awaitExit(NODE_CANCEL_WAIT_MILLIS);
                confirmed = true;
            } else if (session != null && session.isExited()) {
                confirmed = true;
            }
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.build_node_cancel_failed.7b52"), e);
            // 回执超时不等于没取消：节点可能已经杀掉进程但终帧丢在关闭的 WS 里。
            // 这里再查一次任务状态，终态即视为确认（实测 sleep 600 已被杀，但回执没等到）。
            confirmed = this.confirmByStatus();
        } finally {
            if (session != null && !session.isExited()) {
                session.markLost(StrUtil.emptyToDefault(reason, I18nMessageUtil.get("i18n.system_interruption.e37c")));
            }
            this.closeSocket();
        }
        return confirmed;
    }

    /**
     * 用节点任务状态兜底确认取消结果
     *
     * @return true 表示节点任务已处于终态
     */
    private boolean confirmByStatus() {
        if (this.client == null) {
            return false;
        }
        try {
            JsonMessage<JSONObject> status = this.client.status(this.logId);
            if (status == null || status.getData() == null) {
                return false;
            }
            Object state = status.getData().get("state");
            String text = state == null ? StrUtil.EMPTY : String.valueOf(state);
            return StrUtil.equalsAnyIgnoreCase(text, "success", "failed", "cancelled", "canceled", "lost", "timeout");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 等待节点回执的超时时间（节点侧 TERM → 5s → KILL，这里留足余量）
     */
    private static final long NODE_CANCEL_WAIT_MILLIS = 15_000L;

    // ------------------------------------------------------------------ 内部实现

    /**
     * 首次调用时解析节点、校验能力、下发任务并订阅事件流
     *
     * @return 错误信息，null 表示启动成功
     */
    private String ensureStarted() {
        if (this.startError != null) {
            return this.startError;
        }
        if (this.started) {
            return null;
        }
        this.started = true;
        try {
            this.startError = this.doStart();
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.build_node_start_failed.1d33"), e);
            this.startError = StrUtil.emptyToDefault(e.getMessage(), I18nMessageUtil.get("i18n.build_node_start_failed.1d33"));
        }
        return this.startError;
    }

    private String doStart() {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        NodeService nodeService = SpringUtil.getBean(NodeService.class);
        MachineNodeServer machineNodeServer = SpringUtil.getBean(MachineNodeServer.class);
        this.nodeModel = nodeService.getByKey(this.buildNodeId, false);
        Assert.notNull(nodeModel, I18nMessageUtil.get("i18n.build_node_not_exist.5c90"));
        Assert.state(nodeModel.getOpenStatus() != null && nodeModel.getOpenStatus() == 1,
            I18nMessageUtil.get("i18n.build_node_disabled.2a77"));
        this.machineNodeModel = machineNodeServer.getByKey(nodeModel.getMachineId(), false);
        Assert.notNull(machineNodeModel, I18nMessageUtil.get("i18n.build_node_not_exist.5c90"));
        Assert.state(machineNodeModel.getBuildEnabled() != null && machineNodeModel.getBuildEnabled() == 1,
            I18nMessageUtil.get("i18n.build_node_disabled.2a77"));

        this.client = new BuildNodeClient(nodeModel);
        JSONObject info = client.info();
        AgentEngine engine = AgentPackageResolver.resolveEngine(info);
        Assert.state(engine == AgentEngine.GO, I18nMessageUtil.get("i18n.build_node_not_go_agent.8e4b"));
        Assert.state(AgentPackageResolver.hasCapability(info, AgentPackageResolver.CAPABILITY_BUILD),
            I18nMessageUtil.get("i18n.build_node_unsupported_capability.6b02"));

        String script = owner.preparedScript();
        Assert.notNull(script, I18nMessageUtil.get("i18n.no_command_to_execute.340b"));

        JSONObject job = this.createJob(buildInfoModel, script);
        this.submitJob(job);
        this.openLogStream();
        this.recordNodeOnHistory(buildInfoModel);
        return null;
    }

    private JSONObject createJob(BuildInfoModel buildInfoModel, String script) {
        RepositoryModel repositoryModel = taskData.repositoryModel;
        JSONObject job = new JSONObject();
        job.put("jobId", this.logId);
        job.put("buildModelId", buildInfoModel.getId());
        job.put("buildNumberId", buildInfoModel.getBuildId());
        job.put("cacheBuild", buildExtraModule.getCacheBuild() == null || buildExtraModule.getCacheBuild());
        job.put("timeoutSec", DEFAULT_JOB_TIMEOUT_SEC);
        job.put("script", script);
        job.put("shellSuffix", CommandUtil.SUFFIX);
        job.put("strict", buildExtraModule.strictlyEnforce());
        job.put("resultDirFile", buildInfoModel.getResultDirFile());
        job.put("excludeReleaseAnt", StrUtil.emptyToDefault(buildExtraModule.getExcludeReleaseAnt(), StrUtil.EMPTY));
        job.put("releaseHideFile", Boolean.TRUE.equals(buildExtraModule.getReleaseHideFile()));
        //
        JSONObject repo = new JSONObject();
        RepositoryModel.RepoType repoType = repositoryModel.getRepoType() == null
            ? RepositoryModel.RepoType.Git
            : RepositoryModel.RepoType.values()[repositoryModel.getRepoType()];
        job.put("repo", repo);
        repo.put("type", repoType == RepositoryModel.RepoType.Git ? "git" : "svn");
        repo.put("url", repositoryModel.getGitUrl());
        repo.put("username", repositoryModel.getUserName());
        repo.put("password", repositoryModel.getPassword());
        repo.put("branchPattern", buildInfoModel.getBranchName());
        repo.put("tagPattern", buildInfoModel.getBranchTagName());
        repo.put("cloneDepth", buildExtraModule.getCloneDepth());
        repo.put("lastCommitId", buildInfoModel.getRepositoryLastCommitId());
        Boolean checkDiff = taskData.checkRepositoryDiff;
        repo.put("checkDiff", checkDiff != null ? checkDiff : Boolean.TRUE.equals(buildExtraModule.getCheckRepositoryDiff()));
        //
        this.appendEnvironment(job);
        return job;
    }

    /**
     * 环境变量：真实值下发、隐私键单独标记（节点打印时按 *** 脱敏）
     */
    private void appendEnvironment(JSONObject job) {
        EnvironmentMapBuilder environmentMapBuilder = taskData.environmentMapBuilder;
        JSONObject env = new JSONObject();
        JSONArray masked = new JSONArray();
        JSONObject items = JSON.parseObject(environmentMapBuilder.toDataJsonStr());
        if (items != null) {
            items.forEach((key, value) -> {
                if (value instanceof JSONObject) {
                    JSONObject item = (JSONObject) value;
                    String itemValue = item.getString("value");
                    if (itemValue != null) {
                        env.put(key, itemValue);
                    }
                    if (item.getBooleanValue("privacy")) {
                        masked.add(key);
                    }
                } else if (value != null) {
                    env.put(key, StrUtil.toStringOrNull(value));
                }
            });
        }
        job.put("env", env);
        job.put("envMaskedKeys", masked);
    }

    private void submitJob(JSONObject job) {
        for (int i = 0; i < BUSY_RETRY_COUNT; i++) {
            JsonMessage<JSONObject> message = client.start(job);
            int code = message.getCode();
            if (code == 200) {
                return;
            }
            if (code == 409) {
                logRecorder.system(I18nMessageUtil.get("i18n.wait_build_node_free.4c81"), nodeModel.getName());
                try {
                    TimeUnit.MILLISECONDS.sleep(BUSY_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(I18nMessageUtil.get("i18n.system_interruption.e37c"));
                }
                continue;
            }
            if (code == 501) {
                throw new IllegalStateException(I18nMessageUtil.get("i18n.build_node_unsupported_capability.6b02"));
            }
            throw new IllegalStateException(StrUtil.emptyToDefault(message.getMsg(),
                I18nMessageUtil.get("i18n.build_node_start_failed.1d33")));
        }
        throw new IllegalStateException(I18nMessageUtil.get("i18n.build_node_busy_timeout.5b71"));
    }

    private void openLogStream() {
        this.session = new RemoteBuildSession();
        this.socket = client.openLog(this.logId, 0);
        this.socket.onMessage(this::onFrame);
        if (!this.socket.connectBlocking()) {
            throw new IllegalStateException(StrUtil.emptyToDefault(this.socket.getCloseStatusMsg(),
                I18nMessageUtil.get("i18n.build_node_log_connect_failed.0e8c")));
        }
    }

    /**
     * 事件帧处理：日志/阶段原样映射到 Server 构建日志，其余交给 session
     *
     * @param message 帧文本
     */
    private void onFrame(String message) {
        if (StrUtil.isEmpty(message)) {
            return;
        }
        JSONObject json;
        try {
            json = JSON.parseObject(message);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.build_node_frame_parse_failed.3d15"), message, e);
            return;
        }
        if (json == null) {
            return;
        }
        try {
            this.session.accept(json);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.build_node_frame_parse_failed.3d15"), message, e);
            return;
        }
        String type = json.getString("type");
        if ("log".equals(type)) {
            this.writeLog(json.getString("line"));
        } else if ("stage".equals(type)) {
            this.writeStageLog(json);
        }
    }

    private void writeLog(String line) {
        if (line == null) {
            return;
        }
        try {
            logRecorder.appendRaw(line);
        } catch (Exception e) {
            // 取消/关闭后日志已关闭，忽略
        }
    }

    private void writeStageLog(JSONObject json) {
        String stage = json.getString("stage");
        String event = json.getString("event");
        String name = stageName(stage);
        if ("start".equals(event)) {
            String text = StrUtil.format(I18nMessageUtil.get("i18n.start_executing_process.9cb8"), name);
            this.writeLog("[SYSTEM-INFO] " + text);
        } else if ("end".equals(event)) {
            int code = json.getIntValue("code");
            if (code == 0) {
                String text = StrUtil.format(I18nMessageUtil.get("i18n.execution_ended_with_duration.a59b"), name, StrUtil.EMPTY);
                this.writeLog("[SYSTEM-INFO] " + text);
            } else {
                String text = StrUtil.format(I18nMessageUtil.get("i18n.execution_exception_with_flow.6d4b"), name,
                    StrUtil.emptyToDefault(json.getString("message"), String.valueOf(code)));
                this.writeLog("[SYSTEM-ERROR] " + text);
            }
        }
    }

    private static String stageName(String stage) {
        if ("pull".equals(stage)) {
            return I18nMessageUtil.get("i18n.pull_repository_code.3f51");
        }
        if ("executeCommand".equals(stage)) {
            return I18nMessageUtil.get("i18n.build_command_execution.a55c");
        }
        if ("packageFile".equals(stage)) {
            return I18nMessageUtil.get("i18n.package_product.bfbb");
        }
        return StrUtil.emptyToDefault(stage, StrUtil.EMPTY);
    }

    /**
     * 节点回填的环境变量合并到 Server 侧（隐私键以节点上报的脱敏值为准，不回写）
     */
    private void mergeNodeEnv() {
        JSONObject env = session.getEnv();
        if (env == null || env.isEmpty()) {
            return;
        }
        EnvironmentMapBuilder environmentMapBuilder = taskData.environmentMapBuilder;
        env.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            environmentMapBuilder.put(key, StrUtil.toStringOrNull(value));
        });
    }

    /**
     * 异常收敛：先查节点状态，避免「WebSocket 断了但任务还在跑」被误判失败
     */
    private String handleException(String stage, Exception e) {
        log.warn(I18nMessageUtil.get("i18n.build_node_stage_exception.1c47"), stage, e.getMessage());
        try {
            if (client != null) {
                JsonMessage<JSONObject> message = client.status(this.logId);
                JSONObject data = message.getData();
                if (data != null) {
                    String state = data.getString("state");
                    if (BuildBuildNodeState.SUCCESS.equals(state)) {
                        session.markLost(null);
                        return null;
                    }
                    if (BuildBuildNodeState.LOST.equals(state)) {
                        return I18nMessageUtil.get("i18n.build_node_task_lost.9a3e");
                    }
                    return StrUtil.emptyToDefault(data.getString("message"),
                        StrUtil.format(I18nMessageUtil.get("i18n.build_node_build_failed_state.6d31"), state));
                }
            }
        } catch (Exception statusError) {
            log.warn(I18nMessageUtil.get("i18n.build_node_status_query_failed.7e1a"), statusError.getMessage());
        }
        return StrUtil.emptyToDefault(e.getMessage(), I18nMessageUtil.get("i18n.build_node_stage_exception.1c47"));
    }

    /**
     * 拉取产物 zip 并解压成与本地构建一致的结果目录
     *
     * @return 错误信息，null 表示成功
     */
    private String downloadArtifact() throws Exception {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        int buildId = buildInfoModel.getBuildId();
        File zipFile = BuildUtil.getHistoryPackageZipFile(buildInfoModel.getId(), buildId);
        Assert.notNull(zipFile, I18nMessageUtil.get("i18n.build_node_result_missing.9e0f"));
        FileUtil.mkParentDirs(zipFile);
        //
        long expectSize = this.artifact.getLongValue("size");
        String expectSha1 = this.artifact.getString("sha1");
        // 重试 + 断点续传 + 字节对账都在下载器里：重试耗尽必须失败退出，
        // 否则会用 404 响应体或残缺 zip 去解压（产物目录为空、错误信息还会漂到发布阶段）
        ArtifactDownloader downloader = new ArtifactDownloader(zipFile, expectSize, expectSha1,
            (offset, target) -> client.download(this.logId, buildInfoModel.getId(), buildId, target, offset),
            logRecorder, DOWNLOAD_RETRY_COUNT);
        long size = downloader.download();
        log.debug("节点构建产物拉取完成 size={} expected={}", size, expectSize);
        long extractedSize = this.unzipSafe(zipFile, this.resultDir(buildInfoModel, buildId));
        taskData.resultFileSize = extractedSize;
        logRecorder.system(I18nMessageUtil.get("i18n.backup_product.53c0"), StrUtil.SLASH, buildId);
        // ant 通配模式在本地构建里会把结果目录改写为 /，这里保持同样的历史语义
        if (ResultDirFileAction.parse(buildInfoModel.getResultDirFile()).getType() == ResultDirFileAction.Type.ANT_PATH) {
            owner.updateResultDirToRoot();
        }
        FileUtil.del(zipFile);
        return null;
    }

    private File resultDir(BuildInfoModel buildInfoModel, int buildId) {
        return FileUtil.file(BuildUtil.getBuildDataFile(buildInfoModel.getId()),
            "history", BuildInfoModel.getBuildIdStr(buildId), "result");
    }

    /**
     * 安全解压：拒绝绝对路径与 ../ 越界条目（zip-slip）
     *
     * @param zipFile zip 文件
     * @param dir     目标目录
     * @return 解压后文件总大小
     */
    private long unzipSafe(File zipFile, File dir) throws Exception {
        FileUtil.mkdir(dir);
        String canonicalDir = dir.getCanonicalPath();
        long total = 0;
        try (InputStream inputStream = Files.newInputStream(zipFile.toPath());
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, CharsetUtil.CHARSET_UTF_8)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();
                if (StrUtil.isEmpty(name) || entry.isDirectory()) {
                    continue;
                }
                File target = FileUtil.file(dir, name);
                String canonical = target.getCanonicalPath();
                Assert.state(canonical.startsWith(canonicalDir + File.separator),
                    StrUtil.format(I18nMessageUtil.get("i18n.build_node_zip_slip.4dc0"), name));
                FileUtil.mkParentDirs(target);
                try (OutputStream outputStream = Files.newOutputStream(target.toPath())) {
                    int read;
                    while ((read = zipInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, read);
                        total += read;
                    }
                }
            }
        }
        return total;
    }

    private void recordNodeOnHistory(BuildInfoModel buildInfoModel) {
        try {
            DbBuildHistoryLogService service = SpringUtil.getBean(DbBuildHistoryLogService.class);
            BuildHistoryLog historyLog = new BuildHistoryLog();
            historyLog.setId(this.logId);
            historyLog.setBuildNodeId(nodeModel.getId());
            historyLog.setBuildNodeName(nodeModel.getName());
            historyLog.setBuildDataId(buildInfoModel.getId());
            service.updateById(historyLog);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.build_node_history_update_failed.9c1b"), e.getMessage());
        }
    }

    private void closeSocket() {
        if (socket != null) {
            IoUtil.close(socket);
            socket = null;
        }
    }
}