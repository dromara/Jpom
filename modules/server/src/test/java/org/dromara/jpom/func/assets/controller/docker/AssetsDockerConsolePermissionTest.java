/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller.docker;

import org.dromara.jpom.TestI18nSupport;
import org.dromara.jpom.common.interceptor.LoginInterceptor;
import org.dromara.jpom.common.interceptor.PermissionInterceptor;
import org.dromara.jpom.func.assets.controller.MachineDockerController;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.system.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 资产控制台的 docker 操作必须要求系统管理权限（2026-11-19 外部安全报告，CVSS 8.8）。
 * <p>
 * 修复前六个 {@code /system/assets/docker/*} 控制器只有类级 {@code @Feature}、没有 {@code @SystemPermission}，
 * {@code MethodFeature.LIST} 还不做工作空间校验，导致只有某个工作空间 execute 权限的用户可以：
 * 通过 {@code /system/assets/docker/swarm/list-all} 拿到资产列表（含仓库凭据），
 * 再用 {@code /system/assets/docker/images/create-container} 对从未分配给任何工作空间的资产创建特权容器。
 * <p>
 * 这里用真实的 {@link PermissionInterceptor} 做行为级回归：非系统管理用户访问六个控制器的处理器必须被拒绝；
 * 系统管理用户照常放行。若把类级 {@code @SystemPermission} 去掉，本测试会失败。
 *
 * @author jpom
 */
public class AssetsDockerConsolePermissionTest {

    private PermissionInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ServerConfig serverConfig;
    private MachineDockerServer machineDockerServer;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        interceptor = new PermissionInterceptor();
        // 系统权限检查发生在工作空间权限检查之前，这里只需要保证注入字段不为 null
        org.dromara.jpom.service.user.UserBindWorkspaceService userBindWorkspaceService = mock(org.dromara.jpom.service.user.UserBindWorkspaceService.class);
        org.dromara.jpom.model.user.UserBindWorkspaceModel.PermissionResult success = org.dromara.jpom.model.user.UserBindWorkspaceModel.PermissionResult.builder()
            .state(org.dromara.jpom.model.user.UserBindWorkspaceModel.PermissionResultEnum.SUCCESS)
            .build();
        when(userBindWorkspaceService.checkPermission(any(), anyString())).thenReturn(success);
        ReflectionTestUtils.setField(interceptor, "userBindWorkspaceService", userBindWorkspaceService);
        ReflectionTestUtils.setField(interceptor, "nodeService", mock(org.dromara.jpom.service.node.NodeService.class));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        serverConfig = mock(ServerConfig.class);
        machineDockerServer = mock(MachineDockerServer.class);
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void loginAs(int systemUser) {
        UserModel userModel = new UserModel();
        userModel.setId("user-" + systemUser);
        userModel.setParent("parent");
        userModel.setSystemUser(systemUser);
        request.getSession().setAttribute(LoginInterceptor.SESSION_NAME, userModel);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /**
     * 六个资产控制台 docker 控制器实例
     */
    private List<Object> consoles() {
        return Arrays.asList(
            new AssetsDockerImagesController(serverConfig, machineDockerServer),
            new AssetsDockerContainerController(machineDockerServer, serverConfig),
            new AssetsDockerVolumeController(machineDockerServer),
            new AssetsDockerNetworkController(machineDockerServer),
            new AssetsDockerSwarmServiceController(serverConfig, machineDockerServer),
            new AssetsDockerSwarmInfoController(machineDockerServer)
        );
    }

    private static List<Method> handlers(Object controller) {
        List<Method> methods = new ArrayList<>();
        for (Method method : controller.getClass().getMethods()) {
            boolean mapping = method.isAnnotationPresent(org.springframework.web.bind.annotation.RequestMapping.class)
                || method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)
                || method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class);
            if (mapping) {
                methods.add(method);
            }
        }
        assertFalse(methods.isEmpty(), "未找到 " + controller.getClass().getSimpleName() + " 的处理器方法");
        return methods;
    }

    @Test
    public void testWorkspaceUserIsDeniedOnEveryAssetDockerHandler() throws Exception {
        loginAs(0);
        for (Object controller : consoles()) {
            for (Method method : handlers(controller)) {
                boolean allowed = interceptor.preHandle(request, response, new HandlerMethod(controller, method));
                assertFalse(allowed, "工作空间用户不应能访问资产控制台 docker 操作：" + controller.getClass().getSimpleName() + "#" + method.getName());
            }
        }
    }

    @Test
    public void testSwarmListAllDeniedForWorkspaceUser() throws Exception {
        loginAs(0);
        AssetsDockerSwarmInfoController controller = new AssetsDockerSwarmInfoController(machineDockerServer);
        Method listAll = AssetsDockerSwarmInfoController.class.getMethod("listAll");
        boolean allowed = interceptor.preHandle(request, response, new HandlerMethod(controller, listAll));
        assertFalse(allowed, "/system/assets/docker/swarm/list-all 不应允许工作空间用户访问（曾可直接拿到资产列表与仓库凭据）");
    }

    @Test
    public void testAssetDockerHandlerAllowedForSystemUser() throws Exception {
        loginAs(1);
        AssetsDockerImagesController controller = new AssetsDockerImagesController(serverConfig, machineDockerServer);
        Method list = AssetsDockerImagesController.class.getMethod("list", String.class);
        boolean allowed = interceptor.preHandle(request, response, new HandlerMethod(controller, list));
        assertTrue(allowed, "系统管理用户应能继续使用资产控制台 docker 操作");
    }

    /**
     * 对照：系统管理后台的 docker 资产控制器一直带有该注解，便于确认扫描口径正确
     */
    @Test
    public void testSystemAssetControllerStillAnnotated() {
        assertTrue(MachineDockerController.class.isAnnotationPresent(org.dromara.jpom.permission.SystemPermission.class));
    }
}
