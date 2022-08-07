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
package io.jpom.model.user;

import cn.hutool.crypto.SecureUtil;
import io.jpom.model.BaseDbModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@TableName(value = "USER_BIND_WORKSPACE", name = "用户(权限组)工作空间关系表")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserBindWorkspaceModel extends BaseDbModel {

    /**
     * 权限组ID
     *
     * @see UserPermissionGroupBean#getId()
     * 兼容旧数据
     * @see UserModel#getId()
     */
    private String userId;

    private String workspaceId;

    /**
     * 生产绑定关系表 主键 ID
     *
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     * @return id
     */
    public static String getId(String userId, String workspaceId) {
        return SecureUtil.sha1(userId + workspaceId);
    }
}
