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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseDbModel;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@TableName(value = "USER_BIND_WORKSPACE",
    nameKey = "i18n.user_workspace_relation_table.851e")
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

    @Builder
    public static class PermissionResult {
        /**
         * 结果
         */
        private PermissionResultEnum state;
        /**
         * 不能执行的原因
         */
        private String msg;

        public boolean isSuccess() {
            return state == PermissionResultEnum.SUCCESS;
        }

        public String errorMsg(String... pars) {
            String errorMsg = StrUtil.emptyToDefault(msg, I18nMessageUtil.get("i18n.no_permission.e343"));
            return StrUtil.format("{} {}", ArrayUtil.join(pars, StrUtil.SPACE), errorMsg);
        }
    }

    public enum PermissionResultEnum {
        /**
         * 允许执行
         */
        SUCCESS,
        /**
         * 没有权限
         */
        FAIL,
        /**
         * 当前禁止执行
         */
        MISS_PROHIBIT,
        /**
         * 不在计划允许时间段
         */
        MISS_PERIOD,
    }
}
