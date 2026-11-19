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

import me.zhyd.oauth.request.AuthRequest;
import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.configuration.WebConfig;
import org.dromara.jpom.db.DbExtConfig;
import org.dromara.jpom.oauth2.BaseOauth2Config;
import org.dromara.jpom.oauth2.Oauth2Factory;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.service.user.UserService;
import org.dromara.jpom.system.ExtConfigBean;
import org.dromara.jpom.system.ServerConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 首页模板占位符注入（VL-002）回归测试
 * <p>
 * {@code /oauth2-{provide}} 是 {@code @NotLogin} 入口，provide 与 {@code Jpom-ProxyPath} 请求头
 * 都会被替换进首页 HTML 的 script/属性上下文，必须是白名单值，否则构成未授权反射型 XSS。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class IndexControlOauth2XssTest {

    private IndexControl indexControl;

    @BeforeAll
    public static void setUp() {
        TestI18nSupport.init();
        ExtConfigBean.setPath(System.getProperty("java.io.tmpdir"));
    }

    @BeforeEach
    public void setUpController() {
        WebConfig webConfig = mock(WebConfig.class);
        when(webConfig.getApiTimeout()).thenReturn(30);
        when(webConfig.getName()).thenReturn("Jpom");
        when(webConfig.getTransportEncryption()).thenReturn("false");
        NodeConfig nodeConfig = mock(NodeConfig.class);
        when(nodeConfig.getUploadFileSliceSize()).thenReturn(5);
        when(nodeConfig.getUploadFileConcurrent()).thenReturn(3);
        ServerConfig serverConfig = mock(ServerConfig.class);
        when(serverConfig.getWeb()).thenReturn(webConfig);
        when(serverConfig.getNode()).thenReturn(nodeConfig);
        this.indexControl = new IndexControl(mock(UserService.class), mock(SystemParametersServer.class),
            serverConfig, mock(DbExtConfig.class));
    }

    @Test
    public void testOauth2ProvidePayloadIsNotRendered() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        indexControl.oauth2(response, new MockHttpServletRequest(), "x'+alert(document.domain)+'");

        String html = response.getContentAsString();
        assertFalse(html.contains("alert(document.domain)"), "provide payload 不得进入页面：" + html);
        assertTrue(html.contains("window.oauth2Provide = ''"), "非法 provide 必须回退为空串：" + html);
    }

    @Test
    public void testOauth2ProvideScriptBreakoutIsNotRendered() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        indexControl.oauth2(response, new MockHttpServletRequest(), "a';alert(1);//");

        String html = response.getContentAsString();
        assertFalse(html.contains("alert(1)"), "provide payload 不得进入页面：" + html);
        assertFalse(html.contains("';alert"), "provide 不得跳出 script 字符串：" + html);
        assertTrue(html.contains("window.oauth2Provide = ''"));
    }

    @Test
    public void testEnabledOauth2ProvideStillWorks() throws Exception {
        BaseOauth2Config config = mock(BaseOauth2Config.class);
        when(config.enabled()).thenReturn(true);
        when(config.provide()).thenReturn("gitee");
        when(config.authRequest()).thenReturn(mock(AuthRequest.class));
        Oauth2Factory.put(config);

        MockHttpServletResponse response = new MockHttpServletResponse();
        indexControl.oauth2(response, new MockHttpServletRequest(), "gitee");

        String html = response.getContentAsString();
        assertTrue(html.contains("window.oauth2Provide = 'gitee'"), "已启用平台必须正常输出：" + html);
    }

    @Test
    public void testNotEnabledOauth2ProvideFallsBackToEmpty() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        indexControl.oauth2(response, new MockHttpServletRequest(), "github");

        String html = response.getContentAsString();
        assertTrue(html.contains("window.oauth2Provide = ''"), "未启用平台必须回退为空串：" + html);
    }

    @Test
    public void testProxyPathHeaderPayloadIsNotRendered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ServerConst.PROXY_PATH, "/'+alert(1)+'/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        indexControl.index(response, request);

        String html = response.getContentAsString();
        assertFalse(html.contains("alert(1)"), "Jpom-ProxyPath payload 不得进入页面：" + html);
        assertTrue(html.contains("window.routerBase = ''"), "非法代理路径必须回退为空（按根路径处理）：" + html);
    }

    @Test
    public void testNormalProxyPathIsKept() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ServerConst.PROXY_PATH, "jpom/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        indexControl.index(response, request);

        String html = response.getContentAsString();
        assertTrue(html.contains("window.routerBase = '/jpom'"), "正常二级路径必须保持：<routerBase> 渲染结果不符：" + html);
    }
}
