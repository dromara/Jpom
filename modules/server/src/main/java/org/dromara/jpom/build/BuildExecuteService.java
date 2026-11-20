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

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.version.BuildVersionService;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.NodeBuildVersionModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.build.NodeBuildVersionService;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@Service
@Slf4j
public class BuildExecuteService {


    private final BuildInfoService buildService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final RepositoryService repositoryService;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final BuildExecutorPoolService buildExecutorPoolService;
    private final BuildVersionService buildVersionService;
    private final NodeBuildVersionService nodeBuildVersionService;

    public BuildExecuteService(BuildInfoService buildService,
                               DbBuildHistoryLogService dbBuildHistoryLogService,
                               RepositoryService repositoryService,
                               WorkspaceEnvVarService workspaceEnvVarService,
                               BuildExecutorPoolService buildExecutorPoolService,
                               BuildVersionService buildVersionService,
                               NodeBuildVersionService nodeBuildVersionService) {
        this.buildService = buildService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.repositoryService = repositoryService;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.buildExecutorPoolService = buildExecutorPoolService;
        this.buildVersionService = buildVersionService;
        this.nodeBuildVersionService = nodeBuildVersionService;
    }


    /**
     * check status
     *
     * @param buildInfoModel 构建信息
     * @return 错误消息
     */
    public String checkStatus(BuildInfoModel buildInfoModel) {
        if (buildInfoModel == null) {
            return I18nMessageUtil.get("i18n.build_info_not_exist.4470");
        }
        Integer status = buildInfoModel.getStatus();
        if (status == null) {
            return null;
        }
        BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
        Objects.requireNonNull(nowStatus);
        if (nowStatus.isProgress()) {
            return buildInfoModel.getName() + I18nMessageUtil.get("i18n.current_status.81c0") + nowStatus.getDesc();
        }
        return null;
    }

    /**
     * start build
     *
     * @param buildInfoId      构建Id
     * @param userModel        用户信息
     * @param delay            延迟的时间
     * @param triggerBuildType 触发构建类型
     * @param buildRemark      构建备注
     * @param parametersEnv    外部环境变量
     * @return json
     */
    public IJsonMessage<Integer> start(String buildInfoId, UserModel userModel, Integer delay, int triggerBuildType, String buildRemark, Object... parametersEnv) {
        return this.start(buildInfoId, userModel, delay, triggerBuildType, buildRemark, null, parametersEnv);
    }

    /**
     * start build
     *
     * @param buildInfoId         构建Id
     * @param userModel           用户信息
     * @param delay               延迟的时间
     * @param triggerBuildType    触发构建类型
     * @param buildRemark         构建备注
     * @param checkRepositoryDiff 差异构建
     * @param parametersEnv       外部环境变量
     * @return json
     */
    public IJsonMessage<Integer> start(String buildInfoId, UserModel userModel, Integer delay,
                                       int triggerBuildType, String buildRemark, String checkRepositoryDiff,
                                       Object... parametersEnv) {
        synchronized (buildInfoId.intern()) {
            BuildInfoModel buildInfoModel = buildService.getByKey(buildInfoId);
            String e = this.checkStatus(buildInfoModel);
            Assert.isNull(e, () -> e);
            //
            boolean containsKey = BuildExecuteManage.BUILD_MANAGE_MAP.containsKey(buildInfoModel.getId());
            Assert.state(!containsKey, I18nMessageUtil.get("i18n.build_in_progress.4d33"));
            //
            BuildExtraModule buildExtraModule = buildInfoModel.extraData();
            Assert.notNull(buildExtraModule, I18nMessageUtil.get("i18n.build_info_missing.0ab0"));
            // load repository
            RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId(), false);
            Assert.notNull(repositoryModel, I18nMessageUtil.get("i18n.repository_info_does_not_exist.4142"));
            EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(buildInfoModel.getWorkspaceId());
            // 解析外部变量
            environmentMapBuilder.putObjectArray(parametersEnv).putStr(StringUtil.parseEnvStr(buildInfoModel.getBuildEnvParameter()));
            // set buildId field
            buildInfoModel.setBuildId(this.nextBuildId(buildInfoModel));
            //
            TaskData.TaskDataBuilder taskBuilder = TaskData.builder()
                .buildInfoModel(buildInfoModel)
                .repositoryModel(repositoryModel)
                .userModel(userModel)
                .buildRemark(buildRemark)
                .delay(delay)
                .environmentMapBuilder(environmentMapBuilder)
                .triggerBuildType(triggerBuildType);
            //
            Opt.ofBlankAble(checkRepositoryDiff).map(Convert::toBool).ifPresent(taskBuilder::checkRepositoryDiff);
            this.runTask(taskBuilder.build(), buildExtraModule);
            String startMsg = I18nMessageUtil.get("i18n.start_building.1039");
            String delayMsg = StrUtil.format(I18nMessageUtil.get("i18n.delay_build.7d62"), delay);
            String msg = (delay == null || delay <= 0) ? startMsg : delayMsg;
            return JsonMessage.success(msg, buildInfoModel.getBuildId());
        }
    }

    /**
     * 回滚
     *
     * @param oldLog    构建历史
     * @param item      构建项
     * @param userModel 用户信息
     */
    public int rollback(BuildHistoryLog oldLog, BuildInfoModel item, UserModel userModel) {
        synchronized (item.getId().intern()) {
            String e = this.checkStatus(item);
            Assert.isNull(e, () -> e);
            Integer fromBuildNumberId = ObjectUtil.defaultIfNull(oldLog.getFromBuildNumberId(), oldLog.getBuildNumberId());
            int buildId = this.nextBuildId(item);
            item.setBuildId(buildId);
            // 创建新的构建记录
            BuildHistoryLog buildHistoryLog = oldLog.toJson().to(BuildHistoryLog.class);
            buildHistoryLog.setId(null);
            buildHistoryLog.setCreateUser(null);
            buildHistoryLog.setCreateTimeMillis(null);
            buildHistoryLog.setModifyUser(null);
            buildHistoryLog.setModifyTimeMillis(null);
            buildHistoryLog.setResultFileSize(null);
            BuildStatus pubIng = BuildStatus.PubIng;
            buildHistoryLog.setStatus(pubIng.getCode());
            buildHistoryLog.setTriggerBuildType(3);
            buildHistoryLog.setBuildNumberId(buildId);
            buildHistoryLog.setFromBuildNumberId(fromBuildNumberId);
            buildHistoryLog.setStartTime(SystemClock.now());
            buildHistoryLog.setEndTime(null);
            dbBuildHistoryLogService.insert(buildHistoryLog);
            //
            buildService.updateStatus(buildHistoryLog.getBuildDataId(), pubIng, I18nMessageUtil.get("i18n.start_rolling_back_execution.a019"));

            BuildExtraModule buildExtraModule = BuildExtraModule.build(buildHistoryLog);
            //
            EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
            //
            File logFile = BuildUtil.getLogFile(item.getId(), buildId);
            LogRecorder logRecorder = LogRecorder.builder().file(logFile).build();
            ReleaseManage manage = ReleaseManage.builder()
                .buildExtraModule(buildExtraModule)
                .logId(buildHistoryLog.getId())
                .userModel(userModel)
                .buildNumberId(buildHistoryLog.getBuildNumberId())
                .fromBuildNumberId(fromBuildNumberId)
                .logRecorder(logRecorder)
                .buildEnv(environmentMapBuilder)
                .build();
            //
            logRecorder.system(I18nMessageUtil.get("i18n.prepare_rollback.dba6"), fromBuildNumberId, buildId);
            //
            buildExecutorPoolService.execute(() -> manage.rollback(item));
            return buildId;
        }
    }

    /**
     * 快速回滚：把节点上的历史版本切换为当前版本（不重新传输产物）
     *
     * @param oldLog    目标构建历史
     * @param item      构建配置
     * @param userModel 操作人
     * @param restart   切换后是否重启项目
     * @return 新的构建编号
     */
    public int quickRollback(BuildHistoryLog oldLog, BuildInfoModel item, UserModel userModel, boolean restart) {
        synchronized (item.getId().intern()) {
            String e = this.checkStatus(item);
            Assert.isNull(e, () -> e);
            Integer fromBuildNumberId = ObjectUtil.defaultIfNull(oldLog.getFromBuildNumberId(), oldLog.getBuildNumberId());
            List<NodeBuildVersionModel> targets = nodeBuildVersionService.listByBuild(oldLog.getBuildDataId(), fromBuildNumberId);
            Assert.notEmpty(targets, I18nMessageUtil.get("i18n.no_node_version_for_build.3d18"));
            // 历史记录可能没有 extraData（老数据），这里不能直接 Assert，取不到就按默认保留个数处理
            BuildExtraModule oldExtra = StringUtil.jsonConvert(oldLog.getExtraData(), BuildExtraModule.class);
            int versionKeepCount = BuildVersionService.resolveKeepCount(oldExtra == null ? null : oldExtra.getVersionKeepCount());
            int buildId = this.nextBuildId(item);
            item.setBuildId(buildId);
            BuildHistoryLog buildHistoryLog = oldLog.toJson().to(BuildHistoryLog.class);
            buildHistoryLog.setId(null);
            buildHistoryLog.setCreateUser(null);
            buildHistoryLog.setCreateTimeMillis(null);
            buildHistoryLog.setModifyUser(null);
            buildHistoryLog.setModifyTimeMillis(null);
            buildHistoryLog.setBuildNumberId(buildId);
            buildHistoryLog.setFromBuildNumberId(fromBuildNumberId);
            buildHistoryLog.setTriggerBuildType(3);
            buildHistoryLog.setStartTime(SystemClock.now());
            buildHistoryLog.setEndTime(SystemClock.now());
            //
            File logFile = BuildUtil.getLogFile(item.getId(), buildId);
            LogRecorder logRecorder = LogRecorder.builder().file(logFile).build();
            int success = 0;
            int fail = 0;
            try {
                logRecorder.system(I18nMessageUtil.get("i18n.start_quick_rollback.7a41"), oldLog.getBuildNumberId(), fromBuildNumberId);
                for (NodeBuildVersionModel target : targets) {
                    NodeModel nodeModel = buildVersionService.resolveNode(target.getNodeId());
                    if (nodeModel == null) {
                        fail++;
                        logRecorder.systemError(I18nMessageUtil.get("i18n.quick_rollback_node_missing.b6e2"), target.getNodeId(), target.getProjectId());
                        continue;
                    }
                    try {
                        // 切换时显式带上构建配置的保留个数（0/空归一为默认值），否则插件端会按「未启用」清理到只剩当前版本
                        JsonMessage<JSONObject> jsonMessage = buildVersionService.switchVersion(nodeModel, target.getProjectId(),
                            target.getVersionId(), restart, versionKeepCount);
                        if (jsonMessage.success()) {
                            success++;
                            nodeBuildVersionService.markCurrent(target.getNodeId(), target.getProjectId(), target.getVersionId());
                            NodeBuildVersionModel update = new NodeBuildVersionModel();
                            update.setId(target.getId());
                            update.setStatus(BuildVersionService.STATUS_CURRENT);
                            update.setMessage(I18nMessageUtil.get("i18n.quick_rollback_success.9c15"));
                            nodeBuildVersionService.updateById(update);
                            logRecorder.system(I18nMessageUtil.get("i18n.quick_rollback_node_success.2f83"),
                                nodeModel.getName(), target.getProjectId(), target.getVersionId());
                        } else {
                            fail++;
                            logRecorder.systemError(I18nMessageUtil.get("i18n.quick_rollback_node_failed.4b97"),
                                nodeModel.getName(), target.getProjectId(), target.getVersionId(), jsonMessage);
                        }
                    } catch (Exception exception) {
                        fail++;
                        logRecorder.systemError(I18nMessageUtil.get("i18n.quick_rollback_node_exception.8e26"),
                            nodeModel.getName(), target.getProjectId(), exception.getMessage());
                    }
                }
            } finally {
                String msg = StrUtil.format("{}: {} / {}", I18nMessageUtil.get("i18n.quick_rollback_result.c1d4"), success, fail);
                buildHistoryLog.setStatus(success > 0 ? BuildStatus.PubSuccess.getCode() : BuildStatus.PubError.getCode());
                buildHistoryLog.setStatusMsg(msg);
                buildHistoryLog.setReleaseMethod(oldLog.getReleaseMethod());
                dbBuildHistoryLogService.insert(buildHistoryLog);
                buildService.updateStatus(buildHistoryLog.getBuildDataId(),
                    success > 0 ? BuildStatus.PubSuccess : BuildStatus.PubError, msg);
                IoUtil.close(logRecorder);
            }
            return buildId;
        }
    }

    /**
     * 分配下一个构建编号
     *
     * @param buildInfoModel 构建信息
     * @return 新的构建编号
     */
    public int nextBuildId(BuildInfoModel buildInfoModel) {
        // set buildId field
        int buildId = ObjectUtil.defaultIfNull(buildInfoModel.getBuildId(), 0);
        BuildInfoModel update = new BuildInfoModel();
        update.setBuildId(buildId + 1);
        update.setId(buildInfoModel.getId());
        buildService.updateById(update);
        return update.getBuildId();
    }

    /**
     * 创建构建
     *
     * @param taskData         任务
     * @param buildExtraModule 构建更多配置信息
     */
    private void runTask(TaskData taskData, BuildExtraModule buildExtraModule) {
        String logId = this.insertLog(buildExtraModule, taskData);
        //
        BuildExecuteManage.BuildExecuteManageBuilder builder = BuildExecuteManage.builder()
            .taskData(taskData)
            .logId(logId)
            .buildExtraModule(buildExtraModule);
        builder.build().submitTask();
    }


    /**
     * 插入记录
     */
    private String insertLog(BuildExtraModule buildExtraModule, TaskData taskData) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
        // 新一轮构建开始：清掉上一次遗留的「取消意图」
        CANCEL_REQUESTED.remove(buildInfoModel.getId());
        buildExtraModule.updateValue(buildInfoModel);
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        // 更新其他配置字段
        //buildHistoryLog.fillLogValue(buildExtraModule);
        buildHistoryLog.setTriggerBuildType(taskData.triggerBuildType);
        //
        buildHistoryLog.setBuildNumberId(buildInfoModel.getBuildId());
        buildHistoryLog.setBuildName(buildInfoModel.getName());
        buildHistoryLog.setBuildDataId(buildInfoModel.getId());
        buildHistoryLog.setWorkspaceId(buildInfoModel.getWorkspaceId());
        buildHistoryLog.setResultDirFile(buildInfoModel.getResultDirFile());
        buildHistoryLog.setReleaseMethod(buildExtraModule.getReleaseMethod());
        //
        BuildStatus waitExec = BuildStatus.WaitExec;
        buildHistoryLog.setStatus(waitExec.getCode());
        buildHistoryLog.setStartTime(SystemClock.now());
        buildHistoryLog.setBuildRemark(taskData.buildRemark);
        // 缓存数据 - 保证数据一直
        buildHistoryLog.setExtraData(buildExtraModule.toJson().toString());
        dbBuildHistoryLogService.insert(buildHistoryLog);
        //
        buildService.updateStatus(buildHistoryLog.getBuildDataId(), waitExec, I18nMessageUtil.get("i18n.start_queuing_for_execution.7417"));
        return buildHistoryLog.getId();
    }

    /**
     * 更新状态
     *
     * @param buildId     构建ID
     * @param logId       记录ID
     * @param buildStatus to status
     */
    /**
     * 已请求取消的构建：取消可能先于/晚于构建线程的失败写入，用一个标记把「取消意图」传到状态写入处，
     * 避免「节点构建」被取消后节点回报 cancelled、构建线程把终态写成 Error（BUGS B-001）
     */
    private static final Set<String> CANCEL_REQUESTED = ConcurrentHashMap.newKeySet();

    /**
     * 标记该构建已被要求取消
     *
     * @param buildId 构建ID
     */
    public static void markCancelRequested(String buildId) {
        if (StrUtil.isNotEmpty(buildId)) {
            CANCEL_REQUESTED.add(buildId);
        }
    }

    public void updateStatus(String buildId, String logId, int buildNumberId, BuildStatus buildStatus, String msg) {
        if (buildStatus != BuildStatus.Cancel && !buildStatus.isProgress() && CANCEL_REQUESTED.contains(buildId)) {
            // 取消后构建线程/节点回报的任何终态都归一为「已取消」
            buildStatus = BuildStatus.Cancel;
            msg = I18nMessageUtil.get("i18n.manual_cancel.8464");
        }
        // 注意：这里不能清除取消标记——取消先落库、构建线程随后才把失败写回来，
        // 提前清除会让「取消意图」丢失（BUGS B-001 复现两次的根本原因）。
        // 标记在下一次构建开始时清除（见 insertLog）。
        // 「已取消」是终态：取消要先杀进程再落库，被杀进程的退出码会被构建线程写成「执行命令退出码非0」，
        // 把 Cancel(7) 盖成 Error(3)（BUGS B-001）。这里保护取消终态，后续任何非取消状态写入都忽略。
        if (buildStatus != BuildStatus.Cancel) {
            BuildHistoryLog current = dbBuildHistoryLogService.getByKey(logId);
            if (current != null && current.getStatus() != null && current.getStatus() == BuildStatus.Cancel.getCode()) {
                log.info("构建已取消，忽略后续状态覆盖：logId={} status={} msg={}", logId, buildStatus, msg);
                return;
            }
        }
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setId(logId);
        buildHistoryLog.setStatusMsg(msg);
        buildHistoryLog.setStatus(buildStatus.getCode());
        if (!buildStatus.isProgress()) {
            // 结束
            buildHistoryLog.setEndTime(SystemClock.now());
        }
        dbBuildHistoryLogService.updateById(buildHistoryLog);
        buildService.updateStatus(buildId, buildNumberId, buildStatus, msg);
    }


}
