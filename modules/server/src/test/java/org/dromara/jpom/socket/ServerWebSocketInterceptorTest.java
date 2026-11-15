/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.interceptor.IpInterceptor;
import org.dromara.jpom.common.interceptor.LoginInterceptor;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.user.UserBindWorkspaceService;
import org.dromara.jpom.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocket 握手鉴权回归（2.11.13.1-beta 安全修复）：
 * 跨站 Origin / Origin: null / 无会话 / 禁用账号 都必须在握手阶段被拒绝。
 * <p>
 * 拒绝信号：{@code beforeHandshake} 返回 true 但 attributes 写入 {@code permissionMsg}，
 * 后续处理器据此拒绝建立连接。成功路径需要 node/type 等业务参数，不属于本测试的拒绝用例范围。
 *
 * @author jpom
 */
public class ServerWebSocketInterceptorTest {

    private static final String SAME_ORIGIN = "http://127.0.0.1:2122";
    private static final String HOST = "127.0.0.1:2122";

    private ServerWebSocketInterceptor interceptor;
    private UserService userService;
    private IpInterceptor ipInterceptor;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        userService = mock(UserService.class);
        ipInterceptor = mock(IpInterceptor.class);
        // IP 白名单放行，聚焦鉴权分支
        when(ipInterceptor.checkAccess(any())).thenReturn(true);
        interceptor = new ServerWebSocketInterceptor(userService,
            mock(NodeService.class),
            mock(UserBindWorkspaceService.class),
            mock(MachineNodeServer.class),
            ipInterceptor);
    }

    private MockHttpServletRequest request(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/socket");
        request.addHeader("Host", HOST);
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    private Map<String, Object> handshake(MockHttpServletRequest request) throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        interceptor.beforeHandshake(new ServletServerHttpRequest(request),
            mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);
        return attributes;
    }

    private UserModel enabledUser() {
        UserModel userModel = new UserModel("u1");
        userModel.setStatus(1);
        return userModel;
    }

    /**
     * 跨站 Origin：即使带着合法登录会话也必须拒绝（防跨站 WebSocket 劫持）。
     */
    @Test
    public void testRejectCrossSiteOrigin() throws Exception {
        MockHttpServletRequest request = this.request("https://evil.example.com");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.SESSION_NAME, enabledUser());
        request.setSession(session);

        Map<String, Object> attributes = this.handshake(request);
        assertNotNull(attributes.get("permissionMsg"), "跨站 Origin 必须拒绝握手");
        // Origin 校验先于认证，不应进入用户查询
        verify(userService, never()).getByKey(anyString());
    }

    /**
     * 浏览器在 sandbox/opaque 场景会发送字面量 {@code Origin: null}，必须拒绝。
     */
    @Test
    public void testRejectNullOriginHeader() throws Exception {
        MockHttpServletRequest request = this.request("null");
        Map<String, Object> attributes = this.handshake(request);
        assertNotNull(attributes.get("permissionMsg"), "Origin: null 必须拒绝握手");
    }

    /**
     * 无会话、无 Authorization/JPOM-USER-TOKEN 请求头：拒绝。
     */
    @Test
    public void testRejectWithoutSession() throws Exception {
        MockHttpServletRequest request = this.request(SAME_ORIGIN);
        Map<String, Object> attributes = this.handshake(request);
        assertNotNull(attributes.get("permissionMsg"), "无任何认证信息必须拒绝握手");
        verify(userService, never()).getByKey(anyString());
    }

    /**
     * 会话用户被禁用（status=0）：拒绝。
     */
    @Test
    public void testRejectDisabledAccount() throws Exception {
        UserModel disabled = new UserModel("u1");
        disabled.setStatus(0);
        when(userService.getByKey("u1")).thenReturn(disabled);

        MockHttpServletRequest request = this.request(SAME_ORIGIN);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.SESSION_NAME, enabledUser());
        request.setSession(session);

        Map<String, Object> attributes = this.handshake(request);
        assertNotNull(attributes.get("permissionMsg"), "禁用账号必须拒绝握手");
        assertEquals(ServerConst.ACCOUNT_LOCKED_TIP.get(), attributes.get("permissionMsg"));
    }

    /**
     * 缺省 Origin（非浏览器客户端不带该头）不在 Origin 层拒绝，但仍必须通过会话/请求头认证；
     * 无认证时同样拒绝——与 {@code Origin: null} 的拒绝语义区分开。
     */
    @Test
    public void testMissingOriginStillRequiresAuth() throws Exception {
        MockHttpServletRequest request = this.request(null);
        Map<String, Object> attributes = this.handshake(request);
        assertNotNull(attributes.get("permissionMsg"), "缺省 Origin 且无认证信息必须拒绝握手");
        assertNull(attributes.get("userInfo"));
    }
}
