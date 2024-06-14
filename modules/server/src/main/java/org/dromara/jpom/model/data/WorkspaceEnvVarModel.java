/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * 工作空间环境变量
 *
 * @author bwcx_jzy
 * @since 2021/12/10
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "WORKSPACE_ENV_VAR",
    nameKey = "i18n.workspace_env_vars.f7e8", workspaceBind = 2)
@Data
public class WorkspaceEnvVarModel extends BaseWorkspaceModel {

    /**
     * 名称
     */
    private String name;
    /**
     * 值
     */
    private String value;
    /**
     * 描述
     */
    private String description;
    /**
     * 节点ID
     */
    private String nodeIds;
    /**
     * 隐私变量{1，隐私变量，0 非隐私变量（明文回显）}
     */
    private Integer privacy;

    /**
     * 触发器 token
     */
    private String triggerToken;
}
