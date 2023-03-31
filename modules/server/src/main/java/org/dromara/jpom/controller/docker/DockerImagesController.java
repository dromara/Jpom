/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.controller.docker;

import org.dromara.jpom.controller.docker.base.BaseDockerImagesController;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/7
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker/images")
public class DockerImagesController extends BaseDockerImagesController {

    private final DockerInfoService dockerInfoService;
    private final MachineDockerServer machineDockerServer;

    public DockerImagesController(DockerInfoService dockerInfoService,
                                  ServerConfig serverConfig,
                                  MachineDockerServer machineDockerServer) {
        super(serverConfig);
        this.dockerInfoService = dockerInfoService;
        this.machineDockerServer = machineDockerServer;
    }

    @Override
    protected Map<String, Object> toDockerParameter(String id) {
        DockerInfoModel dockerInfoModel = dockerInfoService.getByKey(id);
        Assert.notNull(dockerInfoModel, "没有对应 docker");
        MachineDockerModel machineDockerModel = machineDockerServer.getByKey(dockerInfoModel.getMachineDockerId());
        Assert.notNull(machineDockerModel, "没有对应的 docker 资产");
        return machineDockerServer.toParameter(machineDockerModel);
    }
}
