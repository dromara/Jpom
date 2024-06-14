/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.model.docker.DockerSwarmInfoMode;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2023/3/4
 */
@RestController
@RequestMapping(value = "/system/assets/docker")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE_DOCKER)
@SystemPermission
@Slf4j
public class MachineDockerSwarmController extends BaseServerController {

    private final MachineDockerServer machineDockerServer;
    private final DockerSwarmInfoService dockerSwarmInfoService;

    public MachineDockerSwarmController(MachineDockerServer machineDockerServer,
                                        DockerSwarmInfoService dockerSwarmInfoService) {
        this.machineDockerServer = machineDockerServer;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
    }

    @PostMapping(value = "init", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> init(@ValidatorItem String id) throws Exception {
        MachineDockerModel dockerInfoModel1 = machineDockerServer.getByKey(id, false);
        Assert.notNull(dockerInfoModel1, I18nMessageUtil.get("i18n.docker_does_not_exist.bb41"));
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel1);
        JSONObject data = (JSONObject) plugin.execute("tryInitializeSwarm", parameter);

        //
        IPlugin pluginCheck = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        JSONObject info = pluginCheck.execute("info", parameter, JSONObject.class);
        machineDockerServer.updateSwarmInfo(id, data, info);
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.cluster_created_successfully.6bf3"));
    }

    /**
     * 加入集群
     *
     * @param id         docker ID
     * @param managerId  管理节点
     * @param remoteAddr 集群ID
     * @param role       加入角色
     * @return json
     * @throws Exception 异常
     */
    @PostMapping(value = "join", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<Object> join(@ValidatorItem String managerId,
                                     @ValidatorItem String id,
                                     @ValidatorItem String remoteAddr,
                                     @ValidatorItem String role) throws Exception {
        MachineDockerModel managerSwarmDocker = machineDockerServer.getByKey(managerId, false);
        Assert.notNull(managerSwarmDocker, I18nMessageUtil.get("i18n.docker_does_not_exist.bb41"));
        MachineDockerModel joinSwarmDocker = machineDockerServer.getByKey(id, false);
        Assert.notNull(joinSwarmDocker, I18nMessageUtil.get("i18n.docker_does_not_exist_with_code.689b"));
        JSONObject managerSwarmInfo;
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        IPlugin checkPlugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
        {
            // 获取集群信息
            managerSwarmInfo = (JSONObject) plugin.execute("inSpectSwarm", machineDockerServer.toParameter(managerSwarmDocker));
            Assert.notNull(managerSwarmInfo, I18nMessageUtil.get("i18n.cluster_info_incomplete.84a1"));
        }
        {
            // 检查节点存在的信息
            JSONObject info = checkPlugin.execute("info", machineDockerServer.toParameter(joinSwarmDocker), JSONObject.class);
            Assert.notNull(info, I18nMessageUtil.get("i18n.get_docker_cluster_info_failure.c80d"));
            JSONObject swarm = info.getJSONObject("swarm");
            Assert.notNull(swarm, I18nMessageUtil.get("i18n.get_docker_cluster_info_failure_with_code.fa77"));
            JSONArray remoteManagers = swarm.getJSONArray("remoteManagers");
            if (ArrayUtil.isNotEmpty(remoteManagers)) {
                Optional<Object> optional = remoteManagers.stream().filter(o -> {
                    JSONObject jsonObject = (JSONObject) o;
                    String addr = jsonObject.getString("addr");
                    List<String> strings = StrUtil.splitTrim(addr, StrUtil.COLON);
                    return StrUtil.equals(CollUtil.getFirst(strings), remoteAddr);
                }).findFirst();
                Assert.state(optional.isPresent(), I18nMessageUtil.get("i18n.current_docker_already_in_other_cluster.e629"));
                // 绑定数据
                machineDockerServer.updateSwarmInfo(joinSwarmDocker.getId(), managerSwarmInfo, info);
                return JsonMessage.success(I18nMessageUtil.get("i18n.cluster_binding_success.eb7e"));
            }
        }
        String roleToken;
        {
            // 准备加入集群
            JSONObject joinTokens = managerSwarmInfo.getJSONObject("joinTokens");
            Assert.notNull(joinTokens, I18nMessageUtil.get("i18n.cluster_info_incomplete_with_code.246b"));
            roleToken = joinTokens.getString(role);
            Assert.hasText(roleToken, StrUtil.format(I18nMessageUtil.get("i18n.cannot_join_cluster_as_role.01d4"), role));
        }
        Map<String, Object> parameter = machineDockerServer.toParameter(joinSwarmDocker);
        parameter.put("token", roleToken);
        parameter.put("remoteAddrs", remoteAddr);
        plugin.execute("joinSwarm", parameter);
        // 绑定数据
        JSONObject info = checkPlugin.execute("info", machineDockerServer.toParameter(joinSwarmDocker), JSONObject.class);
        machineDockerServer.updateSwarmInfo(joinSwarmDocker.getId(), managerSwarmInfo, info);
        return JsonMessage.success(I18nMessageUtil.get("i18n.cluster_created_successfully.6bf3"));
    }

    /**
     * 强制退出集群
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "leave-force", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> leaveForce(@ValidatorItem String id) throws Exception {
        //
        MachineDockerModel dockerInfoModel = machineDockerServer.getByKey(id, false);
        Assert.notNull(dockerInfoModel, I18nMessageUtil.get("i18n.docker_not_exist.7ed8"));
        Assert.hasText(dockerInfoModel.getSwarmId(), I18nMessageUtil.get("i18n.current_docker_has_no_cluster_info.0b52"));
        {
            MachineDockerModel where = new MachineDockerModel();
            where.setSwarmId(dockerInfoModel.getSwarmId());
            long allCount = machineDockerServer.count(where);

            where.setSwarmControlAvailable(true);
            long controlCount = machineDockerServer.count(where);
            if (controlCount == 1 && allCount > 1 && dockerInfoModel.isControlAvailable()) {
                // 如果只有一个管理节点，并且还有多个节点, 并且当前操作的是管理节点
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.multiple_worker_nodes_exist.7110"));
            }
        }
        //
        DockerSwarmInfoMode dockerSwarmInfoMode = new DockerSwarmInfoMode();
        dockerSwarmInfoMode.setSwarmId(dockerInfoModel.getSwarmId());
        long count = dockerSwarmInfoService.count(dockerSwarmInfoMode);
        Assert.state(count <= 0, StrUtil.format(I18nMessageUtil.get("i18n.current_docker_cluster_still_associated_with_workspaces.b301"), count));
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        Map<String, Object> parameter = machineDockerServer.toParameter(dockerInfoModel);
        parameter.put("force", true);
        plugin.execute("leaveSwarm", parameter, JSONObject.class);
        //
        MachineDockerModel update = new MachineDockerModel();
        update.setId(dockerInfoModel.getId());
        update.restSwarm();
        machineDockerServer.updateById(update);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.force_unbind_succeeded.5bfd"));
    }

    /**
     * 退出集群
     *
     * @param id 集群ID
     * @return json
     */
    @GetMapping(value = "leave-node", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> leave(@ValidatorItem String id, @ValidatorItem String nodeId) throws Exception {
        MachineDockerModel dockerModel = machineDockerServer.getByKey(id, false);
        Assert.notNull(dockerModel, I18nMessageUtil.get("i18n.no_corresponding_docker.733e"));
        // 找到对应 docker 集群 node id 关联的数据
        MachineDockerModel find = new MachineDockerModel();
        find.setSwarmId(dockerModel.getSwarmId());
        find.setSwarmNodeId(nodeId);
        List<MachineDockerModel> machineDockerModels1 = machineDockerServer.listByBean(find, false);
        Assert.notEmpty(machineDockerModels1, I18nMessageUtil.get("i18n.cluster_node_not_in_system.0645"));
        MachineDockerModel first = CollUtil.getFirst(machineDockerModels1);
        // 找到管理节点
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setSwarmId(dockerModel.getSwarmId());
        machineDockerModel.setSwarmControlAvailable(true);
        List<MachineDockerModel> machineDockerModels = machineDockerServer.listByBean(machineDockerModel, false);
        Assert.notEmpty(machineDockerModels, I18nMessageUtil.get("i18n.manager_node_not_found.df04"));
        Optional<MachineDockerModel> dockerModelOptional = machineDockerModels.stream()
            .filter(machineDockerModel1 -> machineDockerModel1.getStatus() != null && machineDockerModel1.getStatus() == 1)
            .findFirst();
        Assert.state(dockerModelOptional.isPresent(), I18nMessageUtil.get("i18n.no_online_manager_node_found.05d7"));
        //
        IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
        //
        {
            //节点强制退出
            Map<String, Object> parameter = machineDockerServer.toParameter(first);
            parameter.put("force", true);
            plugin.execute("leaveSwarm", parameter, JSONObject.class);
        }
        {
            // 集群删除节点
            Map<String, Object> map = machineDockerServer.toParameter(dockerModelOptional.get());
            map.put("nodeId", nodeId);
            plugin.execute("removeSwarmNode", map);
        }
        //
        MachineDockerModel update = new MachineDockerModel();
        update.setId(first.getId());
        update.restSwarm();
        machineDockerServer.updateById(update);
        return new JsonMessage<>(200, I18nMessageUtil.get("i18n.exclusion_success.7d46"));
    }
}
