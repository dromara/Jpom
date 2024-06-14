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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/1/26
 */
@RestController
@Feature(cls = ClassFeature.DOCKER)
@RequestMapping(value = "/docker")
@Slf4j
public class DockerInfoController extends BaseServerController {

    private final DockerInfoService dockerInfoService;
    private final MachineDockerServer machineDockerServer;

    public DockerInfoController(DockerInfoService dockerInfoService,
                                MachineDockerServer machineDockerServer) {
        this.dockerInfoService = dockerInfoService;
        this.machineDockerServer = machineDockerServer;
    }

    /**
     * @return json
     */
    @GetMapping(value = "api-versions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<JSONObject>> apiVersions() throws Exception {
        IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        List<JSONObject> data = (List<JSONObject>) plugin.execute("apiVersions");
        return JsonMessage.success("", data);
    }

    /**
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<PageResultDto<DockerInfoModel>> list(HttpServletRequest request) {
        // load list with page
        PageResultDto<DockerInfoModel> resultDto = dockerInfoService.listPage(request);
        resultDto.each(dockerInfoModel -> {
            MachineDockerModel machineDockerModel = machineDockerServer.getByKey(dockerInfoModel.getMachineDockerId());
            if (machineDockerModel != null) {
                machineDockerModel.setRegistryPassword(null);
            }
            dockerInfoModel.setMachineDocker(machineDockerModel);
        });
        return JsonMessage.success("", resultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> edit(@ValidatorItem String id, @ValidatorItem String name, String tags, HttpServletRequest request) throws Exception {
        DockerInfoModel dockerInfoModel = new DockerInfoModel();
        dockerInfoModel.setId(id);
        dockerInfoModel.setName(name);
        Assert.state(!StrUtil.contains(tags, StrUtil.COLON), I18nMessageUtil.get("i18n.tag_cannot_contain_colon.f9ae"));
        List<String> tagList = StrUtil.splitTrim(tags, StrUtil.COMMA);
        String newTags = CollUtil.join(tagList, StrUtil.COLON, StrUtil.COLON, StrUtil.COLON);
        dockerInfoModel.setTags(newTags);
        dockerInfoService.updateById(dockerInfoModel, request);
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }


    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<Object> del(@ValidatorItem String id, HttpServletRequest request) throws Exception {
        dockerInfoService.delByKey(id, request);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }


    /**
     * 同步到指定工作空间
     *
     * @param ids           节点ID
     * @param toWorkspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public IJsonMessage<String> syncToWorkspace(@ValidatorItem String ids, @ValidatorItem String toWorkspaceId, HttpServletRequest request) {
        String nowWorkspaceId = dockerInfoService.getCheckUserWorkspace(request);
        //
        dockerInfoService.checkUserWorkspace(toWorkspaceId);
        dockerInfoService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return JsonMessage.success(I18nMessageUtil.get("i18n.operation_succeeded.3313"));
    }

    /**
     * 查询所有的 tag
     *
     * @return msg
     */
    @GetMapping(value = "all-tag", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public IJsonMessage<List<String>> allTag(HttpServletRequest request) {
        String workspaceId = dockerInfoService.getCheckUserWorkspace(request);
        //
        List<String> strings = dockerInfoService.allTag(workspaceId);
        return JsonMessage.success("", strings);
    }
}
