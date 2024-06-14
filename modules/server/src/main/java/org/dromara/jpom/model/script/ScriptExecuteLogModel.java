/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.script;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.CommandExecLogModel;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SERVER_SCRIPT_EXECUTE_LOG",
    nameKey = "i18n.script_template_execution_record.374b", parents = ScriptModel.class)
@Data
public class ScriptExecuteLogModel extends BaseWorkspaceModel {

    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 脚本名称
     */
    private String scriptName;
    /**
     * 触发类型 {0，手动，1 自动触发，2 触发器，3 构建事件}
     */
    private Integer triggerExecType;

    /**
     * 退出码
     */
    private Integer exitCode;

    /**
     * @see CommandExecLogModel.Status
     */
    private Integer status;
}
