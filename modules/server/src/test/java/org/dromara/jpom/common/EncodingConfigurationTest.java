/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.FilterChain;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 响应编码兜底行为测试：未配置兜底 UTF-8、显式配置优先、不改变请求解码。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class EncodingConfigurationTest {

    private static final FilterChain WRITE_CHINESE = (request, response) -> response.getWriter().write("中文提示");

    /**
     * 老安装实例的 conf 没有 server.servlet.encoding 配置块：响应必须兜底 UTF-8，请求解码保持原样
     */
    @Test
    public void unconfiguredFallsBackToUtf8Response() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        EncodingConfiguration.createFilter(environment).doFilter(request, response, WRITE_CHINESE);
        Assertions.assertEquals("UTF-8", response.getCharacterEncoding());
        Assertions.assertEquals("中文提示", new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    /**
     * 兜底不覆盖客户端显式声明的请求编码（未开启 force-request 时）
     */
    @Test
    public void explicitRequestCharsetIsNotOverridden() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCharacterEncoding("GBK");
        MockHttpServletResponse response = new MockHttpServletResponse();
        EncodingConfiguration.createFilter(environment).doFilter(request, response, (req, res) -> res.getWriter().write("ok"));
        Assertions.assertEquals("GBK", request.getCharacterEncoding());
    }

    /**
     * 用户显式配置 charset 时以配置为准
     */
    @Test
    public void explicitCharsetWins() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(EncodingConfiguration.CHARSET_KEY, "GBK");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        EncodingConfiguration.createFilter(environment).doFilter(request, response, WRITE_CHINESE);
        Assertions.assertEquals("GBK", response.getCharacterEncoding());
        Assertions.assertEquals("中文提示", new String(response.getContentAsByteArray(), Charset.forName("GBK")));
    }

    /**
     * 用户显式关闭 force 时不再兜底，保持容器默认行为
     */
    @Test
    public void explicitForceFalseDisablesFallback() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(EncodingConfiguration.FORCE_KEY, "false");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        EncodingConfiguration.createFilter(environment).doFilter(request, response, WRITE_CHINESE);
        Assertions.assertNotEquals("UTF-8", response.getCharacterEncoding());
    }

    /**
     * 新安装实例配置 force/force-request=true 时，请求解码仍按配置强制 UTF-8（行为与自动配置一致）
     */
    @Test
    public void forceRequestConfigIsHonoured() throws Exception {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(EncodingConfiguration.FORCE_KEY, "true")
            .withProperty(EncodingConfiguration.FORCE_REQUEST_KEY, "true");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCharacterEncoding("GBK");
        MockHttpServletResponse response = new MockHttpServletResponse();
        EncodingConfiguration.createFilter(environment).doFilter(request, response, WRITE_CHINESE);
        Assertions.assertEquals("UTF-8", request.getCharacterEncoding());
        Assertions.assertEquals("UTF-8", response.getCharacterEncoding());
    }

    /**
     * 过滤顺序必须与自动配置一致（最高优先级），避免被其它过滤器提前写出响应
     */
    @Test
    public void filterRunsAtHighestPrecedence() {
        CharacterEncodingFilter filter = EncodingConfiguration.createFilter(new MockEnvironment());
        Assertions.assertTrue(filter instanceof Ordered);
        Assertions.assertEquals(Ordered.HIGHEST_PRECEDENCE, ((Ordered) filter).getOrder());
    }
}
