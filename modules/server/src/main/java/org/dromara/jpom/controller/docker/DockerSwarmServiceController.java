/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.docker;

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.controller.docker.base.BaseDockerSwarmServiceController;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/14
 */
@RestController
@Feature(cls = ClassFeature.DOCKER_SWARM)
@RequestMapping(value = "/docker/swarm-service")
@Slf4j
public class DockerSwarmServiceController extends BaseDockerSwarmServiceController {

    private final MachineDockerServer machineDockerServer;

    public DockerSwarmServiceController(ServerConfig serverConfig,
                                        MachineDockerServer machineDockerServer) {
        super(serverConfig);
        this.machineDockerServer = machineDockerServer;
    }


    @Override
    protected Map<String, Object> toDockerParameter(String id) {
        return machineDockerServer.dockerParameter(id);
    }
}
