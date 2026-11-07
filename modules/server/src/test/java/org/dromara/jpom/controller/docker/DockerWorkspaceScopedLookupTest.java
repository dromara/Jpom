/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.docker;

import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.system.ServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.dromara.jpom.TestI18nSupport;

/**
 * 校验 Docker 运行时控制器（容器/镜像/卷/网络/集群）使用工作空间绑定的查询（issue #119）
 * <p>
 * 跨工作空间的 docker/集群 id 在工作空间绑定查询下返回 null，入口会以 Assert 失败拒绝，
 * 而不是像修复前那样通过无工作空间条件的 getByKey(id) 拿到别的空间的资源。
 *
 * @author jpom
 */
public class DockerWorkspaceScopedLookupTest {

    private MockHttpServletRequest request;
    private DockerInfoService dockerInfoService;
    private MachineDockerServer machineDockerServer;

    @BeforeEach
    public void setUp() {
        TestI18nSupport.init();
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        dockerInfoService = mock(DockerInfoService.class);
        machineDockerServer = mock(MachineDockerServer.class);
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testContainerControllerRejectsForeignDockerId() {
        when(dockerInfoService.getByKey("foreign-docker", request)).thenReturn(null);
        DockerContainerController controller = new DockerContainerController(dockerInfoService, machineDockerServer, mock(ServerConfig.class));
        assertThrows(IllegalArgumentException.class, () -> controller.toDockerParameter("foreign-docker"));
        verify(dockerInfoService).getByKey("foreign-docker", request);
        verify(dockerInfoService, never()).getByKey("foreign-docker");
    }

    @Test
    public void testImagesControllerRejectsForeignDockerId() {
        when(dockerInfoService.getByKey("foreign-docker", request)).thenReturn(null);
        DockerImagesController controller = new DockerImagesController(dockerInfoService, mock(ServerConfig.class), machineDockerServer);
        assertThrows(IllegalArgumentException.class, () -> controller.toDockerParameter("foreign-docker"));
        verify(dockerInfoService).getByKey("foreign-docker", request);
        verify(dockerInfoService, never()).getByKey("foreign-docker");
    }

    @Test
    public void testVolumeControllerRejectsForeignDockerId() {
        when(dockerInfoService.getByKey("foreign-docker", request)).thenReturn(null);
        DockerVolumeController controller = new DockerVolumeController(dockerInfoService, machineDockerServer);
        assertThrows(IllegalArgumentException.class, () -> controller.toDockerParameter("foreign-docker"));
        verify(dockerInfoService).getByKey("foreign-docker", request);
        verify(dockerInfoService, never()).getByKey("foreign-docker");
    }

    @Test
    public void testNetworkControllerRejectsForeignDockerId() {
        when(dockerInfoService.getByKey("foreign-docker", request)).thenReturn(null);
        DockerNetworkController controller = new DockerNetworkController(dockerInfoService, machineDockerServer);
        assertThrows(IllegalArgumentException.class, () -> controller.toDockerParameter("foreign-docker"));
        verify(dockerInfoService).getByKey("foreign-docker", request);
        verify(dockerInfoService, never()).getByKey("foreign-docker");
    }

    /**
     * 当前工作空间的 docker 仍然可以正常解析，确认修复不破坏正常流程
     */
    @Test
    public void testContainerControllerAcceptsOwnDockerId() {
        DockerInfoModel dockerInfoModel = new DockerInfoModel();
        dockerInfoModel.setMachineDockerId("machine-1");
        when(dockerInfoService.getByKey("own-docker", request)).thenReturn(dockerInfoModel);
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        when(machineDockerServer.getByKey("machine-1")).thenReturn(machineDockerModel);
        Map<String, Object> parameter = Collections.singletonMap("host", "127.0.0.1");
        when(machineDockerServer.toParameter(machineDockerModel)).thenReturn(parameter);
        DockerContainerController controller = new DockerContainerController(dockerInfoService, machineDockerServer, mock(ServerConfig.class));
        assertSame(parameter, controller.toDockerParameter("own-docker"));
    }

    @Test
    public void testSwarmInfoControllerRejectsForeignSwarmId() {
        DockerSwarmInfoService dockerSwarmInfoService = mock(DockerSwarmInfoService.class);
        when(dockerSwarmInfoService.getByKey("foreign-swarm", request)).thenReturn(null);
        DockerSwarmInfoController controller = new DockerSwarmInfoController(dockerSwarmInfoService, machineDockerServer, dockerInfoService);
        assertThrows(IllegalArgumentException.class, () -> controller.toDockerParameter("foreign-swarm"));
        verify(dockerSwarmInfoService).getByKey("foreign-swarm", request);
        verify(dockerSwarmInfoService, never()).getByKey("foreign-swarm");
    }

    @Test
    public void testSwarmServiceControllerRejectsForeignSwarmId() {
        DockerSwarmInfoService dockerSwarmInfoService = mock(DockerSwarmInfoService.class);
        when(dockerSwarmInfoService.getByKey("foreign-swarm", request)).thenReturn(null);
        DockerSwarmServiceController controller = new DockerSwarmServiceController(mock(ServerConfig.class), machineDockerServer, dockerSwarmInfoService);
        assertThrows(IllegalArgumentException.class, () -> controller.toDockerParameter("foreign-swarm"));
        verify(dockerSwarmInfoService).getByKey("foreign-swarm", request);
        verify(dockerSwarmInfoService, never()).getByKey("foreign-swarm");
    }
}
