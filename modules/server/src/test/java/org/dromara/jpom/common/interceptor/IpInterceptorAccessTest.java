/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.interceptor;

import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.model.data.SystemIpConfigModel;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IP 访问判定回归：**服务端读不到 IP 配置不得伪装成「IP 被拒绝」**。
 * <p>
 * 背景（2026-11-21 重启计划群反馈）：用户没有配置任何 IP 黑白名单，构建卡住后重新构建却整站跳
 * `/prohibit-access` 并返回 `{"code":999,"msg":"Prohibition of access"}`。原因是 `IpInterceptor`
 * 读取系统参数抛异常时一律按 999 处理，把「数据库/系统参数异常」伪装成「IP 限制」，
 * 用户在白名单页面永远查不出问题。
 * <p>
 * 现在必须区分：IP 命中黑名单 → 999（原语义保留）；读不到/解析不了配置 → 500 + 明确文案。
 *
 * @author jpom
 */
public class IpInterceptorAccessTest {

    @BeforeAll
    public static void initI18n() {
        // I18nMessageUtil 依赖 SpringUtil 里的 MessageSource，纯单测需要注入 mock（返回 key 本身）
        TestI18nSupport.init();
    }

    private static IpInterceptor interceptor(SystemIpConfigModel config, RuntimeException error) {
        SystemParametersServer parametersServer = mock(SystemParametersServer.class);
        if (error != null) {
            when(parametersServer.getConfig(anyString(), any(Class.class))).thenThrow(error);
        } else {
            when(parametersServer.getConfig(anyString(), any(Class.class))).thenReturn(config);
        }
        IpInterceptor ipInterceptor = new IpInterceptor();
        try {
            Field field = IpInterceptor.class.getDeclaredField("systemParametersServer");
            field.setAccessible(true);
            field.set(ipInterceptor, parametersServer);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return ipInterceptor;
    }

    private static MockHttpServletRequest request(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index");
        request.setRemoteAddr(ip);
        return request;
    }

    /**
     * 核心回归：读配置抛异常时必须回「配置读取失败」（500），不能回 999、不能跳 /prohibit-access。
     */
    @Test
    public void testConfigReadErrorIsNotIpProhibition() throws Exception {
        IpInterceptor ipInterceptor = interceptor(null, new IllegalStateException("h2 closed channel"));
        MockHttpServletRequest request = request("192.168.9.24");
        assertEquals(IpInterceptor.AccessResult.CONFIG_ERROR, ipInterceptor.checkAccessResult(request));
        assertFalse(ipInterceptor.checkAccess(request), "读不到配置时仍然拒绝访问（fail-closed）");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(ipInterceptor.preHandle(request, response, null));
        String content = response.getContentAsString();
        assertTrue(content.contains("\"code\":500"), content);
        assertFalse(content.contains("999"), "不能再用 999 表示配置读取失败：" + content);
        assertFalse(content.contains("Prohibition of access"), "配置读取失败不能被描述成 IP 拒绝：" + content);
    }

    /**
     * 原语义保留：命中 IP 黑名单仍然是 999 Prohibition of access（前端跳 /prohibit-access）。
     */
    @Test
    public void testProhibitedIpStillProhibition() throws Exception {
        SystemIpConfigModel config = new SystemIpConfigModel();
        config.setProhibited("192.168.9.24");
        IpInterceptor ipInterceptor = interceptor(config, null);
        MockHttpServletRequest request = request("192.168.9.24");
        assertEquals(IpInterceptor.AccessResult.DENY, ipInterceptor.checkAccessResult(request));
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(ipInterceptor.preHandle(request, response, null));
        String content = response.getContentAsString();
        assertTrue(content.contains("999"), content);
        assertTrue(content.contains("Prohibition of access"), content);
    }

    /**
     * 没有配置 IP 限制时正常放行。
     */
    @Test
    public void testNoConfigAllowed() {
        IpInterceptor ipInterceptor = interceptor(null, null);
        assertEquals(IpInterceptor.AccessResult.ALLOW, ipInterceptor.checkAccessResult(request("192.168.9.24")));
    }

    /**
     * 本地回环始终放行，且不依赖配置读取（配置异常也不影响本机访问）。
     */
    @Test
    public void testLoopbackAllowedEvenIfConfigError() {
        IpInterceptor ipInterceptor = interceptor(null, new IllegalStateException("db down"));
        assertEquals(IpInterceptor.AccessResult.ALLOW, ipInterceptor.checkAccessResult(request("127.0.0.1")));
    }
}
