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
package org.dromara.jpom.func.user.model;

import org.dromara.jpom.model.BaseUserModifyDbModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;

/**
 * @author bwcx_jzy
 * @since 2023/3/9
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "USER_LOGIN_LOG", name = "用户登录日志")
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
