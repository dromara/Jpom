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

import org.dromara.jpom.pipeline.engine.PipelineLog;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.build.BuildRunnerOwner;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.build.RemoteBuildRunner;
import org.dromara.jpom.build.TaskData;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.RepositoryModel;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.service.dblog.RepositoryService;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * 流水线「节点构建」执行器：把构建下发给节点执行
 * <p>
 * 不在流水线里重写节点协议：抽了 {@link BuildRunnerOwner} 之后，直接复用在线构建的
 * {@link RemoteBuildRunner}（拉代码 → 执行脚本 → 收集产物 → 下载回服务端），
 * 只是把「构建 id」换成流水线 id、「构建编号」换成运行号，产物落在同一套目录约定下。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineNodeBuildRunner {

    private final RepositoryService repositoryService;

    public PipelineNodeBuildRunner(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    /**
     * 在节点上执行一次构建
     *
     * @param context      运行上下文
     * @param stageId      阶段 id（参与 jobId，便于排查）
     * @param nodeId       构建节点 id
     * @param repositoryId 仓库 id
     * @param ref          引用（branch:名称 / tag:名称）
     * @param script       脚本
     * @param resultDirFile 产物目录（Ant 规则，空＝整个工作区）
     * @param stageEnv     阶段配置的构建环境变量（与在线构建「构建环境变量」同义）
     * @param log          日志出口
     * @return 错误信息；null 表示成功
     */
    public String run(PipelineContext context, String stageId, String nodeId, String repositoryId, String ref,
                      String script, String resultDirFile, Map<String, String> stageEnv, PipelineLog log) throws Exception {
        RepositoryModel repositoryModel = repositoryService.getByKey(repositoryId, false);
        if (repositoryModel == null) {
            return "repository not found: " + repositoryId;
        }
        BuildInfoModel buildInfoModel = new BuildInfoModel();
        buildInfoModel.setId(context.getPipelineId());
        buildInfoModel.setName(context.getPipelineName());
        buildInfoModel.setWorkspaceId(context.getWorkspaceId());
        buildInfoModel.setBuildId(context.getRunNumber());
        buildInfoModel.setBuildNodeId(nodeId);
        buildInfoModel.setBuildMode(2);
        String packageDir = StrUtil.emptyToDefault(resultDirFile, StrUtil.SLASH);
        buildInfoModel.setResultDirFile(packageDir);
        String trimmed = StrUtil.trimToEmpty(ref);
        if (StrUtil.startWith(trimmed, "tag:")) {
            buildInfoModel.setBranchTagName(StrUtil.removePrefix(trimmed, "tag:"));
            buildInfoModel.setBranchName(StrUtil.EMPTY);
        } else {
            buildInfoModel.setBranchName(StrUtil.removePrefix(trimmed, "branch:"));
        }
        BuildExtraModule extra = new BuildExtraModule();
        extra.setId(context.getPipelineId());
        extra.setName(context.getPipelineName());
        extra.setWorkspaceId(context.getWorkspaceId());
        extra.setResultDirFile(packageDir);
        EnvironmentMapBuilder environmentMapBuilder = new EnvironmentMapBuilder(10);
        environmentMapBuilder.putStr(context.getVariables());
        if (stageEnv != null) {
            environmentMapBuilder.putStr(stageEnv);
        }
        TaskData taskData = TaskData.builder()
            .buildInfoModel(buildInfoModel)
            .repositoryModel(repositoryModel)
            .environmentMapBuilder(environmentMapBuilder)
            .build();
        String jobId = context.getPipelineId() + "-" + context.getRunNumber() + "-" + stageId;
        LogRecorder logRecorder = BuildUtil.emptyLogRecorder();
        final String[] stageError = {null};
        BuildRunnerOwner owner = new BuildRunnerOwner() {
            @Override
            public String preparedScript() {
                return script;
            }

            @Override
            public void updateResultDirToRoot() {
                // 流水线产物目录由阶段自己管理，不需要改写构建历史
            }
        };
        RemoteBuildRunner runner = new RemoteBuildRunner(owner, taskData, extra, logRecorder, jobId, nodeId);
        // 节点构建没有本地进程：把「取消」注册给运行上下文，取消时下发到节点并等回执
        context.setStageCancel(reason -> runner.cancel(reason));
        stageError[0] = runner.startReady();
        if (stageError[0] == null) {
            stageError[0] = runner.pull();
        }
        if (stageError[0] == null) {
            stageError[0] = runner.executeCommand();
        }
        if (stageError[0] == null) {
            stageError[0] = runner.packageFile();
        }
        log.info("节点构建结束：" + (stageError[0] == null ? "成功" : stageError[0]));
        context.setStageCancel(null);
        return stageError[0];
    }

    /**
     * 节点构建产物目录（产物会下载到这个目录下）
     *
     * @param pipelineId 流水线
     * @param runNumber  运行号
     * @return 目录
     */
    public File artifactRoot(String pipelineId, int runNumber) {
        return BuildUtil.getHistoryPackageFile(pipelineId, runNumber, StrUtil.SLASH);
    }

    /**
     * 环境变量（保留入口，避免调用方直接拼 map）
     *
     * @param variables 变量
     * @return map
     */
    public Map<String, String> environment(Map<String, String> variables) {
        return variables;
    }
}