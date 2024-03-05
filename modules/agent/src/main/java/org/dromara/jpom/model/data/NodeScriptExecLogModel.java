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

/**
 * 脚本执行记录
 *
 * @author bwcx_jzy
 * @since 2021/12/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeScriptExecLogModel extends BaseWorkspaceModel {

    /**
     * 运行时间
     */
    private Long createTimeMillis;

    /**
     * 脚本ID
     */
    private String scriptId;

    /**
     * 脚本名称
     */
    private String scriptName;

    /**
     * 触发类型 {0，手动，1 自动触发，2 触发器}
     */
    private Integer triggerExecType;
}
