/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.user;

import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 {@link UserService#checkUser(String)} 不会把调用方传入的值当作查询条件解析（issue #121）
 *
 * @author jpom
 */
public class UserServiceCheckUserTest {

    /**
     * 使用 CALLS_REAL_METHODS 的 mock 执行真实的 checkUser 逻辑（不触发构造函数）
     */
    private UserService userService() {
        return Mockito.mock(UserService.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void testRejectConditionExpression() {
        UserService userService = this.userService();
        // Hutool 会把 "is not null" 解析为 SQL 条件，未修复时 checkUser 会继续执行数据库查询
        assertNull(userService.checkUser("is not null"));
        assertNull(userService.checkUser("is null"));
        assertNull(userService.checkUser("= null"));
        assertNull(userService.checkUser("> 0"));
        assertNull(userService.checkUser("in (1,2)"));
        assertNull(userService.checkUser("like %a%"));
    }

    @Test
    public void testRejectEmptyOrPlainToken() {
        UserService userService = this.userService();
        assertNull(userService.checkUser(null));
        assertNull(userService.checkUser(""));
        assertNull(userService.checkUser("   "));
        assertNull(userService.checkUser("jpomadmin"));
        assertNull(userService.checkUser(SecureUtil.sha1("Poc123456") + " "));
        assertNull(userService.checkUser(SecureUtil.md5("Poc123456")));
    }

    @Test
    public void testPasswordDigestFormat() {
        // 密码摘要为 40 位十六进制字符串（sha1），合法值必须被接受
        assertTrue(UserService.isPasswordDigest(SecureUtil.sha1("Poc123456")));
        assertFalse(UserService.isPasswordDigest(null));
        assertFalse(UserService.isPasswordDigest(""));
        assertFalse(UserService.isPasswordDigest("is not null"));
        assertFalse(UserService.isPasswordDigest(SecureUtil.sha1("Poc123456") + " "));
    }
}
