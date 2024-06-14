/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy
 * @since 2022/8/3
 */
@TableName(value = "USER_PERMISSION_GROUP",
    nameKey = "i18n.user_permission_group.52a4")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPermissionGroupBean extends BaseUserModifyDbModel {

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 禁止执行时间段，优先判断禁止执行
     * <pre>
     * [{
     *     "startTime": 1,
     *     "endTime": 1,
     *     "reason": ""
     * }]
     * </pre>
     */
    private String prohibitExecute;

    /**
     * 允许执行的时间段
     * <pre>
     * [{
     *     "week": [1,2],
     *     "startTime":"08:00:00"
     *     "endTime": "18:00:00"
     * }]
     * </pre>
     */
    private String allowExecute;
}
