/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.sshproject;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * SSH 项目执行审计
 * <p>
 * 每次启停/重载都落一条：谁、哪台机、哪个钩子、退出码、耗时。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@TableName(value = "SSH_PROJECT_EXEC_LOG",
    nameKey = "i18n.ssh_project_exec_log.6b93")
@Data
@EqualsAndHashCode(callSuper = true)
public class SshProjectExecLogModel extends BaseWorkspaceModel {

    private String projectId;
    private String projectName;
    private String machineSshId;
    /**
     * 执行的钩子：start/stop/restart/reload/status
     */
    private String hook;
    private Integer exitCode;
    /**
     * 耗时毫秒
     */
    private Long cost;
    /**
     * 是否成功
     */
    private Boolean success;
    private String message;
}
