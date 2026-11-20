/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.sshproject.SshProjectExecLogModel;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * SSH 项目执行审计
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Service
public class SshProjectExecLogService extends BaseWorkspaceService<SshProjectExecLogModel> {

    /**
     * 记录一次执行
     *
     * @param project 项目
     * @param hook    钩子
     * @param result  结果
     */
    public void record(SshProjectModel project, String hook, SshProjectExecuteResult result) {
        SshProjectExecLogModel logModel = new SshProjectExecLogModel();
        logModel.setWorkspaceId(project.getWorkspaceId());
        logModel.setProjectId(project.getId());
        logModel.setProjectName(project.getName());
        logModel.setMachineSshId(project.getMachineSshId());
        // 触发器/自动启动没有登录用户，按系统操作记账（否则 BaseWorkspaceService.insert 会要求登录用户）
        logModel.setModifyUser(org.dromara.jpom.model.user.UserModel.SYSTEM_ADMIN);
        logModel.setHook(hook);
        logModel.setExitCode(result.getExitCode());
        logModel.setCost(result.getCost());
        logModel.setSuccess(result.isSuccess());
        String message = result.getMessage();
        if (message == null && result.getOutput() != null) {
            message = result.getOutput();
        }
        logModel.setMessage(cn.hutool.core.util.StrUtil.maxLength(message, 400));
        this.insert(logModel);
    }
}
