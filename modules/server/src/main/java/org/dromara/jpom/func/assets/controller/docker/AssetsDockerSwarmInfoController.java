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

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.controller.docker.base.BaseDockerSwarmInfoController;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
@RequestMapping(value = "/system/assets/docker/swarm")
@Slf4j
public class AssetsDockerSwarmInfoController extends BaseDockerSwarmInfoController {

    private final MachineDockerServer machineDockerServer;

    public AssetsDockerSwarmInfoController(MachineDockerServer machineDockerServer) {
        this.machineDockerServer = machineDockerServer;
    }

    @Override
    protected Map<String, Object> toDockerParameter(String id) {
        MachineDockerModel machineDockerModel = machineDockerServer.getByKey(id, false);
        Assert.notNull(machineDockerModel, I18nMessageUtil.get("i18n.no_corresponding_docker_info.c47a"));
        if (machineDockerModel.isControlAvailable()) {
            // 管理节点
            return machineDockerServer.toParameter(machineDockerModel);
        }
        // 非管理节点
        MachineDockerModel bySwarmId = machineDockerServer.getMachineDockerBySwarmId(machineDockerModel.getSwarmId());
        return machineDockerServer.toParameter(bySwarmId);
    }

    /**
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<MachineDockerModel>> listAll() {
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setSwarmControlAvailable(true);
        // load list with all
        List<MachineDockerModel> swarmInfoModes = machineDockerServer.listByBean(machineDockerModel);
        return JsonMessage.success("", swarmInfoModes);
    }
}
