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
 * 指令信息
 *
 * @author : Arno
 * @since : 2021/12/4 18:38
 */
@TableName(value = "COMMAND_INFO",
    nameKey = "i18n.command_management.621f")
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandModel extends BaseWorkspaceModel {
    /**
     * 命令名称
     */
    private String name;
    /**
     * 命令描述
     */
    private String desc;
    /**
     * 指令内容
     */
    private String command;
    /**
     * 命令默认参数
     */
    private String defParams;
    /**
     * 默认关联大 ssh id
     */
    private String sshIds;
    /**
     * 自动执行的 cron
     */
    private String autoExecCron;
    /**
     * 触发器 token
     */
    private String triggerToken;
}
