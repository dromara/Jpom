/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.system.extconf.BuildExtConfig;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Objects;

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
    private final DockerInfoService dockerInfoService;
    private final MachineDockerServer machineDockerServer;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final ScriptServer scriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;
    private final BuildExtConfig buildExtConfig;
    private final FileStorageService fileStorageService;

    public BuildExecuteService(BuildInfoService buildService,
                               DbBuildHistoryLogService dbBuildHistoryLogService,
                               RepositoryService repositoryService,
                               DockerInfoService dockerInfoService,
                               WorkspaceEnvVarService workspaceEnvVarService,
                               ScriptServer scriptServer,
                               ScriptExecuteLogServer scriptExecuteLogServer,
                               BuildExtConfig buildExtConfig,
                               MachineDockerServer machineDockerServer,
                               FileStorageService fileStorageService) {
        this.buildService = buildService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.repositoryService = repositoryService;
        this.dockerInfoService = dockerInfoService;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.scriptServer = scriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
        this.buildExtConfig = buildExtConfig;
        this.machineDockerServer = machineDockerServer;
        this.fileStorageService = fileStorageService;
    }


    /**
     * check status
     *
     * @param buildInfoModel 构建信息
     * @return 错误消息
     */
    public String checkStatus(BuildInfoModel buildInfoModel) {
        if (buildInfoModel == null) {
            return "不存在对应的构建信息";
        }
        Integer status = buildInfoModel.getStatus();
        if (status == null) {
            return null;
        }
        BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
        Objects.requireNonNull(nowStatus);
        if (nowStatus.isProgress()) {
            return buildInfoModel.getName() + " 当前还在：" + nowStatus.getDesc();
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
            Assert.state(!containsKey, "当前构建还在进行中");
            //
            BuildExtraModule buildExtraModule = StringUtil.jsonConvert(buildInfoModel.getExtraData(), BuildExtraModule.class);
            Assert.notNull(buildExtraModule, "构建信息缺失");
            // load repository
            RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId(), false);
            Assert.notNull(repositoryModel, "仓库信息不存在");
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
            String msg = (delay == null || delay <= 0) ? "开始构建中" : "延迟" + delay + "秒后开始构建";
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
            buildService.updateStatus(buildHistoryLog.getBuildDataId(), pubIng, "开始回滚执行");

            BuildExtraModule buildExtraModule = BuildExtraModule.build(buildHistoryLog);
            //
            EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
            //
            ReleaseManage manage = ReleaseManage.builder()
                .buildExtraModule(buildExtraModule)
                .logId(buildHistoryLog.getId())
                .userModel(userModel)
                .machineDockerServer(machineDockerServer)
                .dockerInfoService(dockerInfoService)
                .fileStorageService(fileStorageService)
                .buildExtConfig(buildExtConfig)
                .buildNumberId(buildHistoryLog.getBuildNumberId())
                .fromBuildNumberId(fromBuildNumberId)
                .buildExecuteService(this)
                .buildEnv(environmentMapBuilder)
                .build();
            File logFile = BuildUtil.getLogFile(item.getId(), buildId);
            LogRecorder logRecorder = LogRecorder.builder().file(logFile).build();
            //
            logRecorder.system("开始准备回滚：{} -> {}", fromBuildNumberId, buildId);
            //
            ThreadUtil.execute(() -> manage.rollback(item));
            return buildId;
        }
    }

    private int nextBuildId(BuildInfoModel buildInfoModel) {
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
            .scriptServer(scriptServer)
            .dockerInfoService(dockerInfoService)
            .machineDockerServer(machineDockerServer)
            .fileStorageService(fileStorageService)
            .buildService(buildService)
            .scriptExecuteLogServer(scriptExecuteLogServer)
            .buildExtConfig(buildExtConfig)
            .dbBuildHistoryLogService(dbBuildHistoryLogService)
            .buildExtraModule(buildExtraModule)
            .buildExecuteService(this);
        builder.build().submitTask();
    }


    /**
     * 插入记录
     */
    private String insertLog(BuildExtraModule buildExtraModule, TaskData taskData) {
        BuildInfoModel buildInfoModel = taskData.buildInfoModel;
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
        buildHistoryLog.setExtraData(buildInfoModel.getExtraData());
        dbBuildHistoryLogService.insert(buildHistoryLog);
        //
        buildService.updateStatus(buildHistoryLog.getBuildDataId(), waitExec, "开始排队等待执行");
        return buildHistoryLog.getId();
    }

    /**
     * 更新状态
     *
     * @param buildId     构建ID
     * @param logId       记录ID
     * @param buildStatus to status
     */
    public void updateStatus(String buildId, String logId, int buildNumberId, BuildStatus buildStatus, String msg) {
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
