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
package org.dromara.jpom.model.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy
 * @since 2022/8/3
 */
@TableName(value = "USER_PERMISSION_GROUP", name = "用户权限组")
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
