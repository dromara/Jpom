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
package io.jpom.controller.docker;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import io.jpom.common.JsonMessage;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.controller.docker.base.BaseDockerSwarmInfoController;
import io.jpom.func.assets.model.MachineDockerModel;
import io.jpom.func.assets.server.MachineDockerServer;
import io.jpom.model.docker.DockerInfoModel;
import io.jpom.model.docker.DockerSwarmInfoMode;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.docker.DockerInfoService;
import io.jpom.service.docker.DockerSwarmInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@RestController
@Feature(cls = ClassFeature.DOCKER_SWARM)
@RequestMapping(value = "/docker/swarm")
@Slf4j
public class DockerSwarmInfoController extends BaseDockerSwarmInfoController {

    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final MachineDockerServer machineDockerServer;
    private final DockerInfoService dockerInfoService;

    public DockerSwarmInfoController(DockerSwarmInfoService dockerSwarmInfoService,
                                     MachineDockerServer machineDockerServer,
                                     DockerInfoService dockerInfoService) {
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.machineDockerServer = machineDockerServer;
        this.dockerInfoService = dockerInfoService;
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<DockerSwarmInfoMode>> list(HttpServletRequest request) {
        // load list with page
        PageResultDto<DockerSwarmInfoMode> resultDto = dockerSwarmInfoService.listPage(request);
        resultDto.each(dockerSwarmInfoMode -> {
            String swarmId = dockerSwarmInfoMode.getSwarmId();
            MachineDockerModel machineDocker = machineDockerServer.tryMachineDockerBySwarmId(swarmId);
            dockerSwarmInfoMode.setMachineDocker(machineDocker);
        });
        return JsonMessage.success("", resultDto);
    }

    /**
     * @return json
     */
    @GetMapping(value = "list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<List<DockerSwarmInfoMode>> listAll(HttpServletRequest request) {
        // load list with all
        List<DockerSwarmInfoMode> swarmInfoModes = dockerSwarmInfoService.listByWorkspace(request);
        return JsonMessage.success("", swarmInfoModes);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Object> edit(@ValidatorItem String id,
                                    @ValidatorItem String name,
                                    @ValidatorItem String tag,
                                    HttpServletRequest request) throws Exception {
        String workspaceId = dockerSwarmInfoService.getCheckUserWorkspace(request);
        DockerSwarmInfoMode dockerSwarmInfoMode1 = dockerSwarmInfoService.getByKey(id, request);
        Assert.notNull(dockerSwarmInfoMode1, "对应的集群不存在");
        // 更新集群信息
        DockerSwarmInfoMode dockerSwarmInfoMode = new DockerSwarmInfoMode();
        dockerSwarmInfoMode.setId(id);
        dockerSwarmInfoMode.setName(name);
        dockerSwarmInfoMode.setTag(tag);
        dockerSwarmInfoService.updateById(dockerSwarmInfoMode);
        // 更新集群关联的 docker 工作空间的 tag
        MachineDockerModel dockerModel = new MachineDockerModel();
        dockerModel.setSwarmId(dockerSwarmInfoMode1.getSwarmId());
        List<MachineDockerModel> machineDockerModels = machineDockerServer.listByBean(dockerModel);
        Assert.notEmpty(machineDockerModels, "当前集群未找到 docker 信息");
        for (MachineDockerModel machineDockerModel : machineDockerModels) {
            DockerInfoModel queryWhere = new DockerInfoModel();
            queryWhere.setMachineDockerId(machineDockerModel.getId());
            queryWhere.setWorkspaceId(workspaceId);
            List<DockerInfoModel> dockerInfoModels = dockerInfoService.listByBean(queryWhere);
            for (DockerInfoModel dockerInfoModel : dockerInfoModels) {
                // 处理标签
                Collection<String> allTag = StrUtil.splitTrim(dockerInfoModel.getTags(), StrUtil.COLON);
                allTag = ObjectUtil.defaultIfNull(allTag, new ArrayList<>());
                if (!allTag.contains(tag)) {
                    allTag.add(tag);
                }
                allTag = allTag.stream().filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
                String newTags = CollUtil.join(allTag, StrUtil.COLON, StrUtil.COLON, StrUtil.COLON);
                //
                Entity where = Entity.create().set("id", dockerInfoModel.getId());
                Entity update = Entity.create().set("tags", newTags);
                dockerInfoService.update(update, where);
            }
        }
        //
        return JsonMessage.success("修改成功");
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<Object> del(@ValidatorItem String id, HttpServletRequest request) throws Exception {
        dockerSwarmInfoService.delByKey(id, request);
        return JsonMessage.success("删除成功");
    }


    @Override
    protected Map<String, Object> toDockerParameter(String id) {
        return machineDockerServer.dockerParameter(id);
    }
}
