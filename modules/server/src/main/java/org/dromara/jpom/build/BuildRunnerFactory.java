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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.enums.BuildMode;
import org.dromara.jpom.util.LogRecorder;

/**
 * 构建执行器装配点。
 * <p>
 * 唯一决定「这次构建在哪里执行」的地方：构建方式为「节点构建」（{@link BuildMode#Node}）走远程节点执行器，
 * 其余构建方式（本地构建、容器构建）仍由 Server 本机执行（历史行为不变）。
 * <p>
 * 新增一种构建方式时只需在此注册一个执行器分支，调用方（{@code BuildExecuteManage}）零改动。
 *
 * @author jpom
 * @since 2.11.15
 */
public final class BuildRunnerFactory {

    private BuildRunnerFactory() {
    }

    /**
     * 创建执行器
     *
     * @param owner            构建编排对象（本机执行器回调它的具体实现）
     * @param taskData         本次构建数据
     * @param buildExtraModule 构建扩展配置
     * @param logRecorder      构建日志
     * @param logId            构建历史记录 ID
     * @return 执行器
     */
    public static BuildRunner create(BuildExecuteManage owner, TaskData taskData,
                                     BuildExtraModule buildExtraModule, LogRecorder logRecorder, String logId) {
        String buildNodeId = taskData.buildInfoModel.getBuildNodeId();
        // 「节点构建」是构建方式的一类；buildMode=0 + buildNodeId 非空 是 2.11.14.3 之前（该 beta 未发布）的旧表达，
        // 这里一并按节点构建处理，保证已存在的测试数据不需要先迁移就能继续构建。
        int buildMode = BuildMode.normalize(taskData.buildInfoModel.getBuildMode(), buildNodeId);
        if (!BuildMode.isNode(buildMode) || StrUtil.isEmpty(buildNodeId)) {
            return new LocalBuildRunner(owner);
        }
        return new RemoteBuildRunner(owner, taskData, buildExtraModule, logRecorder, logId, buildNodeId);
    }
}
