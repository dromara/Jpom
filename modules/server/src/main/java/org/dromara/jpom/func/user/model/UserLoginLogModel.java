/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.user.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy
 * @since 2023/3/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "USER_LOGIN_LOG",
    nameKey = "i18n.user_login_log.0c00")
@Data
@NoArgsConstructor
public class UserLoginLogModel extends BaseUserModifyDbModel {

    /**
     * 操作ip
     */
    private String ip;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 浏览器标识
     */
    private String userAgent;

    /**
     * 是否使用 mfa
     */
    private Boolean useMfa;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误原因
     * <p>
     * 0 正常登录
     * 1 密码错误
     * 2 被锁定
     * 3 续期
     * 4 账号被禁用
     * 5 登录成功，但是需要 mfa 验证
     * 6 oauth2 登录成功
     */
    private Integer operateCode;
}
