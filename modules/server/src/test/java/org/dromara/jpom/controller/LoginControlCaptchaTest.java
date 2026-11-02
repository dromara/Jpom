/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import cn.keepbx.jpom.IJsonMessage;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.configuration.UserConfig;
import org.dromara.jpom.configuration.WebConfig;
import org.dromara.jpom.func.user.server.UserLoginLogServer;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.system.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 登录验证码校验（问题五修复）单元测试。
 * <p>
 * 覆盖：无验证码 / 空串 / 错误 / 正确（大小写不敏感）/ 过期 / null，
 * 以及验证码一次性（失败也移除）和“验证码先于账号查询”的顺序约束。
 *
 * @author jpom
 */
public class LoginControlCaptchaTest {

    private static final String LOGIN_CODE = "login_code";
    private static final String LOGIN_CODE_TIME = "login_code_time";

    private MockHttpServletRequest request;
    private MockHttpSession session;
    private UserConfig userConfig;
    private WebConfig webConfig;
    private UserService userService;
    private LoginControl loginControl;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        LoginControl.LFU_CACHE.clear();
        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        userConfig = new UserConfig();
        // ipError() 会读取该配置，避免 NPE
        userConfig.setIpErrorLockTime(Duration.ofHours(5));
        webConfig = new WebConfig();
        ServerConfig serverConfig = mock(ServerConfig.class);
        when(serverConfig.getUser()).thenReturn(userConfig);
        when(serverConfig.getWeb()).thenReturn(webConfig);
        userService = mock(UserService.class);
        UserLoginLogServer userLoginLogServer = mock(UserLoginLogServer.class);
        loginControl = new LoginControl(userService, serverConfig, userLoginLogServer);
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        LoginControl.LFU_CACHE.clear();
    }

    private void prepareCaptcha(String code, long createTime) {
        session.setAttribute(LOGIN_CODE, code);
        session.setAttribute(LOGIN_CODE_TIME, createTime);
    }

    private void assertCaptchaConsumed() {
        assertNull(session.getAttribute(LOGIN_CODE));
        assertNull(session.getAttribute(LOGIN_CODE_TIME));
    }

    /**
     * 未请求 /rand-code（session 中无验证码）+ 空串：修复前的绕过场景，必须被拒绝
     */
    @Test
    public void testMissingCaptchaInSessionRejected() {
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha(""));
        assertCaptchaConsumed();
    }

    /**
     * session 中有验证码，但请求提交空串：必须被拒绝
     */
    @Test
    public void testEmptyCodeRejected() {
        prepareCaptcha("AbCd", System.currentTimeMillis());
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha(""));
        assertCaptchaConsumed();
    }

    /**
     * 缺少 code 参数（null）：必须被拒绝
     */
    @Test
    public void testNullCodeRejected() {
        prepareCaptcha("AbCd", System.currentTimeMillis());
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha(null));
        assertCaptchaConsumed();
    }

    /**
     * 错误验证码：必须被拒绝，且验证码、生成时间均被移除（失败也作废）
     */
    @Test
    public void testWrongCodeRejectedAndConsumed() {
        prepareCaptcha("AbCd", System.currentTimeMillis());
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha("zzzz"));
        assertCaptchaConsumed();
    }

    /**
     * 正确验证码：大小写不敏感，校验通过后 session 中的验证码被移除（一次性）
     */
    @Test
    public void testCorrectCodeAcceptedCaseInsensitiveAndConsumed() {
        prepareCaptcha("AbCd", System.currentTimeMillis());
        assertTrue(loginControl.validateLoginCaptcha("abcd"));
        assertCaptchaConsumed();
    }

    /**
     * 超过 5 分钟 TTL：必须被拒绝并作废
     */
    @Test
    public void testExpiredCodeRejected() {
        long expiredTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6);
        prepareCaptcha("AbCd", expiredTime);
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha("abcd"));
        assertCaptchaConsumed();
    }

    /**
     * 验证码一次性：第一次失败即作废，之后即使提交原来的正确验证码也不能通过
     */
    @Test
    public void testFailedAttemptCannotReuseOriginalCode() {
        prepareCaptcha("AbCd", System.currentTimeMillis());
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha("zzzz"));
        assertThrows(IllegalStateException.class, () -> loginControl.validateLoginCaptcha("abcd"));
        assertCaptchaConsumed();
    }

    /**
     * 验证码校验必须先于账号查询：session 无验证码时，连账号是否存在都不会暴露
     */
    @Test
    public void testCaptchaValidatedBeforeAccountLookup() {
        assertThrows(IllegalStateException.class,
            () -> loginControl.userLogin("not-exist-user", "pwd", "", request));
        verify(userService, never()).getByKey(anyString());
    }

    /**
     * 账号枚举修复：不存在账号与“存在账号 + 密码错误”返回相同的业务码和文案
     */
    @Test
    public void testNonexistentUserAndWrongPasswordShareSameResponse() {
        // 关闭验证码，单独验证账号枚举响应的一致性
        webConfig.setDisabledCaptcha(true);
        when(userService.getByKey("missing-user")).thenReturn(null);
        IJsonMessage<Object> missing = loginControl.userLogin("missing-user", "pwd", null, request);

        UserModel userModel = new UserModel("existing-user");
        userModel.setStatus(1);
        when(userService.getByKey("existing-user")).thenReturn(userModel);
        when(userService.simpleLogin("existing-user", "pwd")).thenReturn(null);
        IJsonMessage<Object> wrongPwd = loginControl.userLogin("existing-user", "pwd", null, request);

        assertEquals(501, missing.getCode());
        assertEquals(501, wrongPwd.getCode());
        assertEquals(missing.getMsg(), wrongPwd.getMsg());
    }
}
