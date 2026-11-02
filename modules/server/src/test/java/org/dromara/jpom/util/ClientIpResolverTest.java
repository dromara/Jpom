/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import org.dromara.jpom.common.ClientIpUtil;
import org.dromara.jpom.configuration.WebConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * {@link ClientIpResolver} 单元测试：验证可信代理 + X-Forwarded-For 解析策略。
 *
 * @author jpom
 * @since 2026/11/2
 */
public class ClientIpResolverTest {

    private static final String TRUSTED_REMOTE = "127.0.0.1";
    private static final String XFF = "X-Forwarded-For";
    private static final String REAL_IP = "X-Real-IP";

    private ClientIpResolver resolver;

    @BeforeEach
    public void setUp() {
        WebConfig webConfig = new WebConfig();
        webConfig.setTrustedProxies(Arrays.asList("127.0.0.1/32", "::1/128", "198.51.100.0/24"));
        this.resolver = new ClientIpResolver(webConfig);
        this.resolver.afterPropertiesSet();
    }

    @AfterEach
    public void tearDown() {
        ClientIpUtil.setResolver(null);
    }

    private static MockHttpServletRequest request(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    public void testUntrustedRemoteIgnoresForwardedHeaders() {
        MockHttpServletRequest request = request("203.0.113.5");
        request.addHeader(XFF, "1.1.1.1");
        request.addHeader(REAL_IP, "2.2.2.2");
        Assertions.assertEquals("203.0.113.5", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxyTakesRightmostForwardedIp() {
        // 攻击者可伪造 XFF 左段，只有最右侧（紧邻可信代理）的地址可信
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "1.1.1.1, 203.0.113.9");
        Assertions.assertEquals("203.0.113.9", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxySkipsTrustedHops() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "203.0.113.9, 127.0.0.1");
        Assertions.assertEquals("203.0.113.9", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxySingleForwardedIp() {
        MockHttpServletRequest request = request("0:0:0:0:0:0:0:1");
        request.addHeader(XFF, "203.0.113.8");
        Assertions.assertEquals("203.0.113.8", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxyFallsBackToXRealIp() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(REAL_IP, "203.0.113.7");
        Assertions.assertEquals("203.0.113.7", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxyFallsBackToRemoteAddr() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        // 整条链都是可信代理、X-Real-IP 非法时回退 remoteAddr
        request.addHeader(XFF, "127.0.0.1, 198.51.100.1");
        request.addHeader(REAL_IP, "unknown");
        Assertions.assertEquals(TRUSTED_REMOTE, this.resolver.resolve(request));
    }

    @Test
    public void testInvalidForwardedValuesSkipped() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "not-an-ip, 203.0.113.6");
        Assertions.assertEquals("203.0.113.6", this.resolver.resolve(request));

        MockHttpServletRequest onlyInvalid = request(TRUSTED_REMOTE);
        onlyInvalid.addHeader(XFF, "not-an-ip");
        onlyInvalid.addHeader(REAL_IP, "also-bad");
        Assertions.assertEquals(TRUSTED_REMOTE, this.resolver.resolve(onlyInvalid));
    }

    @Test
    public void testCustomCidrTrustedProxy() {
        WebConfig webConfig = new WebConfig();
        webConfig.setTrustedProxies(Arrays.asList("198.51.100.0/24"));
        ClientIpResolver cidrResolver = new ClientIpResolver(webConfig);
        cidrResolver.afterPropertiesSet();

        MockHttpServletRequest trusted = request("198.51.100.3");
        trusted.addHeader(XFF, "203.0.113.4");
        Assertions.assertEquals("203.0.113.4", cidrResolver.resolve(trusted));

        MockHttpServletRequest untrusted = request("192.0.2.10");
        untrusted.addHeader(XFF, "203.0.113.4");
        Assertions.assertEquals("192.0.2.10", cidrResolver.resolve(untrusted));
        // CIDR 边界：198.51.100.255 在 198.51.100.0/24 内
        MockHttpServletRequest boundary = request("198.51.100.255");
        boundary.addHeader(XFF, "203.0.113.11");
        Assertions.assertEquals("203.0.113.11", cidrResolver.resolve(boundary));
        // CIDR 边界：198.51.101.1 不在 198.51.100.0/24 内
        MockHttpServletRequest notInRange = request("198.51.101.1");
        notInRange.addHeader(XFF, "203.0.113.11");
        Assertions.assertEquals("198.51.101.1", cidrResolver.resolve(notInRange));
    }

    @Test
    public void testTrustedProxyWithPortNormalized() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "203.0.113.10:8080");
        Assertions.assertEquals("203.0.113.10", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxyIpv6WithBracketsAndPort() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "[2001:db8::2]:443");
        Assertions.assertEquals("2001:db8:0:0:0:0:0:2", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxyIpv6Chain() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "2001:db8::5, ::1");
        Assertions.assertEquals("2001:db8:0:0:0:0:0:5", this.resolver.resolve(request));
    }

    @Test
    public void testTrustedProxyIpv6UntrustedRemote() {
        MockHttpServletRequest request = request("2001:db8::9");
        request.addHeader(XFF, "203.0.113.12");
        Assertions.assertEquals("2001:db8:0:0:0:0:0:9", this.resolver.resolve(request));
    }

    @Test
    public void testDefaultTrustedProxiesAreLoopback() {
        WebConfig webConfig = new WebConfig();
        Assertions.assertTrue(webConfig.getTrustedProxies().contains("127.0.0.1/32"));
        Assertions.assertTrue(webConfig.getTrustedProxies().contains("::1/128"));
        // 空列表同样回退默认回环信任
        webConfig.setTrustedProxies(new ArrayList<>());
        Assertions.assertTrue(webConfig.getTrustedProxies().contains("127.0.0.1/32"));

        ClientIpResolver defaultResolver = new ClientIpResolver(webConfig);
        defaultResolver.afterPropertiesSet();
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "203.0.113.3");
        Assertions.assertEquals("203.0.113.3", defaultResolver.resolve(request));
    }

    @Test
    public void testInvalidRemoteAddrNotTrusted() {
        MockHttpServletRequest request = request("not-an-ip");
        request.addHeader(XFF, "203.0.113.1");
        Assertions.assertEquals("not-an-ip", this.resolver.resolve(request));
    }

    @Test
    public void testNoResolverFallsBackToHutool() {
        ClientIpUtil.setResolver(null);
        MockHttpServletRequest request = request("198.51.100.1");
        request.addHeader(XFF, "203.0.113.1");
        // 无 server resolver（agent 等场景）保持原 Hutool 行为
        Assertions.assertEquals("203.0.113.1", ClientIpUtil.resolve(request));
    }

    @Test
    public void testClientIpUtilUsesResolver() {
        MockHttpServletRequest request = request(TRUSTED_REMOTE);
        request.addHeader(XFF, "1.1.1.1, 203.0.113.2");
        Assertions.assertEquals("203.0.113.2", ClientIpUtil.resolve(request));
    }
}
