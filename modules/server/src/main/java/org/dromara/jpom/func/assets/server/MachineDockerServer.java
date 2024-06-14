/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.server;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.event.IAsyncLoad;
import cn.keepbx.jpom.plugins.IPlugin;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AssetsConfig;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.func.assets.AssetsExecutorPoolService;
import org.dromara.jpom.func.assets.model.MachineDockerModel;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.cert.service.CertificateInfoService;
import org.dromara.jpom.func.system.model.ClusterInfoModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.docker.DockerInfoModel;
import org.dromara.jpom.model.docker.DockerSwarmInfoMode;
import org.dromara.jpom.plugin.PluginFactory;
import org.dromara.jpom.service.docker.DockerInfoService;
import org.dromara.jpom.service.docker.DockerSwarmInfoService;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author bwcx_jzy
 * @since 2023/3/3
 */
@Service
@Slf4j
public class MachineDockerServer extends BaseDbService<MachineDockerModel> implements ILoadEvent, IAsyncLoad, Task {
    private static final String CRON_ID = "docker-monitor";
    private final MachineSshServer machineSshServer;
    private final DockerInfoService dockerInfoService;
    private final DockerSwarmInfoService dockerSwarmInfoService;
    private final ClusterInfoService clusterInfoService;
    private final AssetsConfig.DockerConfig dockerConfig;
    private final AssetsExecutorPoolService assetsExecutorPoolService;
    @Resource
    @Lazy
    private CertificateInfoService certificateInfoService;


    public MachineDockerServer(MachineSshServer machineSshServer,
                               DockerInfoService dockerInfoService,
                               DockerSwarmInfoService dockerSwarmInfoService,
                               ClusterInfoService clusterInfoService,
                               AssetsConfig assetsConfig,
                               AssetsExecutorPoolService assetsExecutorPoolService) {
        this.machineSshServer = machineSshServer;
        this.dockerInfoService = dockerInfoService;
        this.dockerSwarmInfoService = dockerSwarmInfoService;
        this.clusterInfoService = clusterInfoService;
        this.dockerConfig = assetsConfig.getDocker();
        this.assetsExecutorPoolService = assetsExecutorPoolService;
    }

    @Override
    protected void fillInsert(MachineDockerModel machineDockerModel) {
        super.fillInsert(machineDockerModel);
        machineDockerModel.setGroupName(StrUtil.emptyToDefault(machineDockerModel.getGroupName(), Const.DEFAULT_GROUP_NAME.get()));
        machineDockerModel.setStatus(ObjectUtil.defaultIfNull(machineDockerModel.getStatus(), 0));
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        long count = this.count();
        if (count != 0) {
            log.debug(I18nMessageUtil.get("i18n.docker_data_repair_not_needed.0fb9"), count);
            return;
        }
        List<DockerInfoModel> list = dockerInfoService.list(false);
        if (CollUtil.isEmpty(list)) {
            log.debug(I18nMessageUtil.get("i18n.no_docker_info_no_need_to_fix_machine_data.f45e"));
            return;
        }
        Map<String, List<DockerInfoModel>> map = CollStreamUtil.groupByKey(list, DockerInfoModel::getHost);
        List<MachineDockerModel> models = new ArrayList<>(map.size());
        for (Map.Entry<String, List<DockerInfoModel>> entry : map.entrySet()) {
            List<DockerInfoModel> value = entry.getValue();
            // 排序，最近更新过优先
            value.sort((o1, o2) -> CompareUtil.compare(o2.getModifyTimeMillis(), o1.getModifyTimeMillis()));
            DockerInfoModel first = CollUtil.getFirst(value);
            if (value.size() > 1) {
                log.warn(I18nMessageUtil.get("i18n.multiple_docker_addresses_found.0f82"), entry.getKey(), first.getName());
            }
            models.add(this.dockerInfoToMachineDocker(first));
        }
        this.insert(models);
        log.info(I18nMessageUtil.get("i18n.machines_docker_data_fixed.af8a"), models.size());
        // 更新 docker 的机器id
        for (MachineDockerModel value : models) {
            Entity entity = Entity.create();
            entity.set("machineDockerId", value.getId());
            {
                //
                Entity where = Entity.create();
                where.set("host", value.getHost());
                int update = dockerInfoService.update(entity, where);
                Assert.state(update > 0, I18nMessageUtil.get("i18n.update_docker_machine_id_failed.063d") + value.getName());
            }
        }
    }


    private MachineDockerModel dockerInfoToMachineDocker(DockerInfoModel dockerInfoModel) {
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setName(dockerInfoModel.getName());
        machineDockerModel.setHost(dockerInfoModel.getHost());
        machineDockerModel.setTlsVerify(dockerInfoModel.getTlsVerify());
        machineDockerModel.setHeartbeatTimeout(dockerInfoModel.getHeartbeatTimeout());
        //
        machineDockerModel.setSwarmNodeId(dockerInfoModel.getSwarmNodeId());
        machineDockerModel.setSwarmId(dockerInfoModel.getSwarmId());
        //
        machineDockerModel.setRegistryEmail(dockerInfoModel.getRegistryEmail());
        machineDockerModel.setRegistryUrl(dockerInfoModel.getRegistryUrl());
        machineDockerModel.setRegistryUsername(dockerInfoModel.getRegistryUsername());
        machineDockerModel.setRegistryPassword(dockerInfoModel.getRegistryPassword());
        return machineDockerModel;
    }

    @Override
    public void startLoad() {
        String monitorCron = dockerConfig.getMonitorCron();
        String cron = Opt.ofBlankAble(monitorCron).orElse("0 0/1 * * * ?");
        CronUtils.add(CRON_ID, cron, () -> MachineDockerServer.this);
    }

    @Override
    public void execute() {
        Entity entity = new Entity();
        if (clusterInfoService.isMultiServer()) {
            // 查询对应分组的数据
            ClusterInfoModel current = clusterInfoService.getCurrent();
            String linkGroup = current.getLinkGroup();
            List<String> linkGroups = StrUtil.splitTrim(linkGroup, StrUtil.COMMA);
            if (CollUtil.isEmpty(linkGroups)) {
                log.warn(I18nMessageUtil.get("i18n.cluster_not_bound_to_group_for_docker_monitoring.3926"));
                return;
            }
            entity.set("groupName", linkGroups);
        }
        List<MachineDockerModel> list = this.listByEntity(entity, false);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        this.checkList(list);
    }

    private void checkList(List<MachineDockerModel> monitorModels) {
        monitorModels.forEach(monitorModel -> assetsExecutorPoolService.execute(() -> this.updateMonitor(monitorModel)));
    }

    /**
     * 监控 容器
     *
     * @param dockerInfoModel docker
     */
    public boolean updateMonitor(MachineDockerModel dockerInfoModel) {
        try {
            DockerInfoModel model = new DockerInfoModel();
            model.setMachineDockerId(dockerInfoModel.getId());
            IPlugin pluginCheck = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            Map<String, Object> parameter = this.toParameter(dockerInfoModel);
            //
            JSONObject info = pluginCheck.execute("info", parameter, JSONObject.class);
            //
            MachineDockerModel update = new MachineDockerModel();
            update.setId(dockerInfoModel.getId());
            update.setStatus(1);
            update.setLastHeartbeatTime(SystemClock.now());
            //
            update.setDockerVersion(info.getString("serverVersion"));
            JSONObject swarm = info.getJSONObject("swarm");
            //
            IPlugin plugin = PluginFactory.getPlugin(DockerSwarmInfoService.DOCKER_PLUGIN_NAME);
            JSONObject swarmData = null;
            try {
                if (dockerInfoModel.isControlAvailable()) {
                    swarmData = plugin.execute("inSpectSwarm", this.toParameter(dockerInfoModel), JSONObject.class);
                } else {
                    // 找到管理节点
                    MachineDockerModel managerDocker = this.getMachineDockerBySwarmId(dockerInfoModel.getSwarmId());
                    swarmData = plugin.execute("inSpectSwarm", this.toParameter(managerDocker), JSONObject.class);
                }
            } catch (Exception e) {
                log.debug(I18nMessageUtil.get("i18n.get_docker_cluster_failure_with_placeholder.06cb"), dockerInfoModel.getName(), e.getMessage());
            }
            Optional.ofNullable(swarmData).ifPresent(jsonObject -> {
                String swarmId = jsonObject.getString("id");
                update.setSwarmCreatedAt(DateUtil.parse(jsonObject.getString("createdAt")).getTime());
                update.setSwarmUpdatedAt(DateUtil.parse(jsonObject.getString("updatedAt")).getTime());
                update.setSwarmId(swarmId);
            });
            Optional.ofNullable(swarm).ifPresent(jsonObject -> {
                String nodeId = jsonObject.getString("nodeID");
                String nodeAddr = jsonObject.getString("nodeAddr");
                boolean controlAvailable = jsonObject.getBooleanValue("controlAvailable");
                update.setSwarmControlAvailable(controlAvailable);
                update.setSwarmNodeAddr(nodeAddr);
                update.setSwarmNodeId(nodeId);
            });
            if (StrUtil.isEmpty(update.getSwarmNodeId())) {
                // 集群退出
                update.restSwarm();
            }
            update.setFailureMsg(StrUtil.EMPTY);
            update.setCertExist(this.checkCertPath(dockerInfoModel));
            super.updateById(update);
            //
            return true;
        } catch (Exception e) {
            String message = e.getMessage();
            if (ExceptionUtil.isCausedBy(e, NoSuchFileException.class)) {
                log.error(I18nMessageUtil.get("i18n.monitor_docker_exception_detail.e334"), dockerInfoModel.getName(), message);
            } else if (StrUtil.containsIgnoreCase(message, "Connection timed out")) {
                log.error(I18nMessageUtil.get("i18n.monitor_docker_timeout.b03b"), dockerInfoModel.getName(), message);
            } else {
                log.error(I18nMessageUtil.get("i18n.monitor_docker_exception.e326"), dockerInfoModel.getName(), e);
            }
            this.updateStatus(dockerInfoModel.getId(), 0, message);
            return false;
        }
    }

    /**
     * 验证 证书文件是否存在
     *
     * @param dockerInfoModel docker
     * @return true 证书文件存在
     */
    private boolean checkCertPath(MachineDockerModel dockerInfoModel) {
        try {
            File filePath = certificateInfoService.getFilePath(dockerInfoModel.getCertInfo());
            if (filePath == null) {
                return false;
            }
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            return (boolean) plugin.execute("certPath", "certPath", filePath.getAbsolutePath());
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.check_docker_cert_exception.8042"), e.getMessage());
            return false;
        }
    }

    /**
     * 验证 证书文件是否存在
     *
     * @param path 路径
     * @return true 证书文件存在
     */
    public boolean checkCertPath(String path) {
        try {
            IPlugin plugin = PluginFactory.getPlugin(DockerInfoService.DOCKER_CHECK_PLUGIN_NAME);
            return (boolean) plugin.execute("certPath", "certPath", path);
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.check_docker_cert_exception.8042"), e.getMessage());
            return false;
        }
    }

    public void updateSwarmInfo(String id, JSONObject swarmData, JSONObject info) {
        //
        JSONObject swarm = info.getJSONObject("swarm");
        Assert.notNull(swarm, I18nMessageUtil.get("i18n.cluster_info_incomplete_for_operation.ad96"));
        String nodeAddr = swarm.getString("nodeAddr");
        String nodeId = swarm.getString("nodeID");
        Assert.hasText(nodeAddr, I18nMessageUtil.get("i18n.node_address_not_found.f955"));
        //
        Date createdAt = swarmData.getDate("createdAt");
        Date updatedAt = swarmData.getDate("updatedAt");
        String swarmId = swarmData.getString("id");
        //
        MachineDockerModel machineDockerModel = new MachineDockerModel();
        machineDockerModel.setSwarmUpdatedAt(createdAt.getTime());
        machineDockerModel.setSwarmUpdatedAt(updatedAt.getTime());
        machineDockerModel.setSwarmId(swarmId);
        machineDockerModel.setSwarmNodeAddr(nodeAddr);
        machineDockerModel.setSwarmNodeId(nodeId);
        boolean controlAvailable = swarm.getBooleanValue("controlAvailable");
        machineDockerModel.setSwarmControlAvailable(controlAvailable);
        machineDockerModel.setId(id);
        this.updateById(machineDockerModel);
    }

    /**
     * 更新 容器状态
     *
     * @param id     ID
     * @param status 状态值
     * @param msg    错误消息
     */
    private void updateStatus(String id, int status, String msg) {
        MachineDockerModel dockerInfoModel = new MachineDockerModel();
        dockerInfoModel.setId(id);
        dockerInfoModel.setStatus(status);
        dockerInfoModel.setFailureMsg(msg);
        super.updateById(dockerInfoModel);
    }

    public Map<String, Object> dockerParameter(DockerInfoModel dockerInfoModel) {
        String machineDockerId = dockerInfoModel.getMachineDockerId();
        MachineDockerModel machineDockerModel = this.getByKey(machineDockerId, false);
        Assert.notNull(machineDockerModel, I18nMessageUtil.get("i18n.no_docker_info_found.6d38"));
        Integer status = machineDockerModel.getStatus();
        Assert.state(status != null && status == 1, StrUtil.format(I18nMessageUtil.get("i18n.current_docker_offline.a509"), machineDockerModel.getName()));
        return this.toParameter(machineDockerModel);
    }

    /**
     * 跟进 docker 列表找到一个可用的 docker 信息
     *
     * @param dockerInfoModels docker 列表
     * @return map
     */
    public Map<String, Object> dockerParameter(List<DockerInfoModel> dockerInfoModels) {
        for (DockerInfoModel dockerInfoModel : dockerInfoModels) {
            String machineDockerId = dockerInfoModel.getMachineDockerId();
            MachineDockerModel machineDockerModel = this.getByKey(machineDockerId, false);
            if (machineDockerModel != null) {
                Integer status = machineDockerModel.getStatus();
                if (status != null && status == 1) {
                    Map<String, Object> parameter = this.toParameter(machineDockerModel);
                    // 更新名称
                    parameter.put("name", dockerInfoModel.getName());
                    return parameter;
                }
            }
        }
        return null;
    }

    /**
     * 通过集群 id 获取 docker 管理参数
     *
     * @param workspaceSwarmId 集群id
     * @return map
     */
    public Map<String, Object> dockerParameter(String workspaceSwarmId) {
        MachineDockerModel first = this.getMachineDocker(workspaceSwarmId);
        Assert.notNull(first, I18nMessageUtil.get("i18n.cluster_manager_node_not_found.1cd0"));
        Integer status = first.getStatus();
        Assert.state(status != null && status == 1, StrUtil.format(I18nMessageUtil.get("i18n.current_docker_cluster_has_no_management_nodes_online.56cd"), first.getName()));
        return toParameter(first);
    }

    private MachineDockerModel getMachineDocker(String workspaceSwarmId) {
        DockerSwarmInfoMode swarmInfoMode = dockerSwarmInfoService.getByKey(workspaceSwarmId);
        Assert.notNull(swarmInfoMode, I18nMessageUtil.get("i18n.no_cluster_info_found.fb40"));
        String modeSwarmId = swarmInfoMode.getSwarmId();
        //
        return this.getMachineDockerBySwarmId(modeSwarmId);
    }

    public MachineDockerModel getMachineDockerBySwarmId(String swarmId) {
        //
        MachineDockerModel dockerInfoModel = this.tryMachineDockerBySwarmId(swarmId);
        Assert.notNull(dockerInfoModel, I18nMessageUtil.get("i18n.no_manager_node_found.5934"));
        return dockerInfoModel;
    }

    public MachineDockerModel tryMachineDockerBySwarmId(String swarmId) {
        if (StrUtil.isEmpty(swarmId)) {
            return null;
        }
        //
        MachineDockerModel dockerInfoModel = new MachineDockerModel();
        dockerInfoModel.setSwarmId(swarmId);
        dockerInfoModel.setSwarmControlAvailable(true);
        List<MachineDockerModel> machineDockerModels = this.listByBean(dockerInfoModel, false);
        if (machineDockerModels == null) {
            return null;
        }
        // 跟进在线情况排序
        machineDockerModels.sort((o1, o2) -> CompareUtil.compare(o2.getStatus(), o1.getStatus()));
        return CollUtil.getFirst(machineDockerModels);
    }

    /**
     * 插件 插件参数 map
     *
     * @return 插件需要使用到到参数
     */
    public Map<String, Object> toParameter(MachineDockerModel machineDockerModel) {
        Map<String, Object> parameter = new HashMap<>(10);
        parameter.put("dockerHost", machineDockerModel.getHost());
        parameter.put("name", machineDockerModel.getName());
        parameter.put("registryUsername", machineDockerModel.getRegistryUsername());
        parameter.put("registryPassword", machineDockerModel.getRegistryPassword());
        parameter.put("registryEmail", machineDockerModel.getRegistryEmail());
        parameter.put("registryUrl", machineDockerModel.getRegistryUrl());
        parameter.put("timeout", machineDockerModel.getHeartbeatTimeout());
        if (machineDockerModel.getTlsVerify()) {
            File filePath = certificateInfoService.getFilePath(machineDockerModel.getCertInfo());
            Assert.notNull(filePath, I18nMessageUtil.get("i18n.docker_certificate_file_missing.ad46"));
            parameter.put("dockerCertPath", filePath.getAbsolutePath());
        }
        if (Boolean.TRUE.equals(machineDockerModel.getEnableSsh()) && StrUtil.isNotEmpty(machineDockerModel.getMachineSshId())) {
            // 添加SSH的操作Session
            MachineSshModel sshModel = machineSshServer.getByKey(machineDockerModel.getMachineSshId());
            Assert.notNull(sshModel, I18nMessageUtil.get("i18n.ssh_info_does_not_exist.5ed0"));
            // 需要关闭之前的连接，避免阻塞
            parameter.put("closeBefore", true);
            parameter.put("session", (Supplier<Session>) () -> machineSshServer.getSessionByModel(sshModel));
        }
        return parameter;
    }
}
