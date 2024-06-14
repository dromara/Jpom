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
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.event.IAsyncLoad;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.exception.AgentAuthorizeException;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.func.assets.AssetsExecutorPoolService;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.model.MachineNodeStatLogModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.system.db.InitDb;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@Service
@Slf4j
public class MachineNodeServer extends BaseDbService<MachineNodeModel> implements ILoadEvent, IAsyncLoad, Runnable {

    private final NodeService nodeService;
    private final NodeConfig nodeConfig;
    private final MachineNodeStatLogServer machineNodeStatLogServer;
    private final ClusterInfoService clusterInfoService;
    private final AssetsExecutorPoolService assetsExecutorPoolService;

    private static final String TASK_ID = "system_monitor_node";

    public MachineNodeServer(NodeService nodeService,
                             ServerConfig serverConfig,
                             MachineNodeStatLogServer machineNodeStatLogServer,
                             ClusterInfoService clusterInfoService,
                             AssetsExecutorPoolService assetsExecutorPoolService) {
        this.nodeService = nodeService;
        this.nodeConfig = serverConfig.getNode();
        this.machineNodeStatLogServer = machineNodeStatLogServer;
        this.clusterInfoService = clusterInfoService;
        this.assetsExecutorPoolService = assetsExecutorPoolService;
    }

    @Override
    protected void fillSelectResult(MachineNodeModel data) {
        Optional.ofNullable(data).ifPresent(machineNodeModel -> machineNodeModel.setJpomPassword(null));
    }

    @Override
    protected void fillInsert(MachineNodeModel machineNodeModel) {
        super.fillInsert(machineNodeModel);
        machineNodeModel.setGroupName(StrUtil.emptyToDefault(machineNodeModel.getGroupName(), Const.DEFAULT_GROUP_NAME.get()));
        //
        machineNodeModel.setTransportMode(0);
    }

    /**
     * 同步数据，兼容低版本数据
     *
     * @param applicationContext 应用上下文
     * @throws Exception 异常
     */
    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        long count = this.count();
        if (count != 0) {
            log.debug(I18nMessageUtil.get("i18n.node_machine_table_exists_no_need_to_fix.2625"), count);
            return;
        }
        List<NodeModel> list = nodeService.list(false);
        if (CollUtil.isEmpty(list)) {
            log.debug(I18nMessageUtil.get("i18n.no_node_info_no_need_to_fix_machine_data.562e"));
            return;
        }
        // delete from MACHINE_NODE_INFO;
        // drop table MACHINE_NODE_INFO;
        Map<String, List<NodeModel>> nodeUrlMap = CollStreamUtil.groupByKey(list, NodeModel::getUrl);
        List<MachineNodeModel> machineNodeModels = new ArrayList<>(nodeUrlMap.size());
        for (Map.Entry<String, List<NodeModel>> entry : nodeUrlMap.entrySet()) {
            List<NodeModel> value = entry.getValue();
            // 排序，最近更新过优先
            value.sort((o1, o2) -> CompareUtil.compare(o2.getModifyTimeMillis(), o1.getModifyTimeMillis()));
            NodeModel first = CollUtil.getFirst(value);
            if (value.size() > 1) {
                log.warn(I18nMessageUtil.get("i18n.multiple_node_data_exists_merge_config.043f"), entry.getKey(), first.getName());
            }
            machineNodeModels.add(this.nodeInfoToMachineNode(first));
        }
        this.insert(machineNodeModels);
        log.info(I18nMessageUtil.get("i18n.machines_node_data_fixed.7744"), machineNodeModels.size());
        // 更新节点的机器id
        for (MachineNodeModel value : machineNodeModels) {
            Entity entity = Entity.create();
            entity.set("machineId", value.getId());
            Entity where = Entity.create();
            where.set("url", value.getJpomUrl());
            int update = nodeService.update(entity, where);
            Assert.state(update > 0, I18nMessageUtil.get("i18n.update_node_machine_id_failed.51d9") + value.getName());
        }
    }

    /**
     * 保证在数据库启动成功之后
     *
     * @return 想要比数据库晚加载
     * @see InitDb#getOrder()
     */
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 1;
    }

    /**
     * 节点对象转机器对象
     *
     * @param nodeModel 节点
     * @return 机器对象
     */
    private MachineNodeModel nodeInfoToMachineNode(NodeModel nodeModel) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setName(nodeModel.getName());
        machineNodeModel.setGroupName(nodeModel.getGroup());
        machineNodeModel.setTransportMode(0);
        machineNodeModel.setStatus(0);
        machineNodeModel.setJpomTimeout(nodeModel.getTimeOut());
        machineNodeModel.setJpomUrl(nodeModel.getUrl());
        machineNodeModel.setJpomUsername(nodeModel.getLoginName());
        machineNodeModel.setJpomPassword(nodeModel.getLoginPwd());
        machineNodeModel.setJpomProtocol(nodeModel.getProtocol());
        machineNodeModel.setJpomHttpProxy(nodeModel.getHttpProxy());
        machineNodeModel.setJpomHttpProxyType(nodeModel.getHttpProxyType());
        machineNodeModel.setModifyUser(nodeModel.getModifyUser());
        machineNodeModel.setCreateTimeMillis(nodeModel.getCreateTimeMillis());
        machineNodeModel.setModifyTimeMillis(nodeModel.getModifyTimeMillis());
        return machineNodeModel;
    }

    @Override
    public void startLoad() {
        // 启动心跳检测
        int heartSecond = nodeConfig.getHeartSecond();
        ScheduledExecutorService scheduler = JpomApplication.getScheduledExecutorService();
        scheduler.scheduleWithFixedDelay(this, 0, heartSecond, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        Entity entity = new Entity();
        if (clusterInfoService.isMultiServer()) {
            String linkGroup = clusterInfoService.getCurrent().getLinkGroup();
            List<String> linkGroups = StrUtil.splitTrim(linkGroup, StrUtil.COMMA);
            if (CollUtil.isEmpty(linkGroups)) {
                log.warn(I18nMessageUtil.get("i18n.cluster_not_bound_to_group_for_node_monitoring.1586"));
                return;
            }
            entity.set("groupName", linkGroups);
        }
        entity.set("transportMode", 0);
        int heartSecond = nodeConfig.getHeartSecond();
        try {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, StrUtil.format(I18nMessageUtil.get("i18n.execution_frequency.d014"), heartSecond));
            taskStat.onStart();
            //MachineNodeModel machineNodeModel = new MachineNodeModel();
            //machineNodeModel.setTransportMode(0);
            List<MachineNodeModel> machineNodeModels = this.listByEntity(entity);
            this.checkList(machineNodeModels);
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, StrUtil.format(I18nMessageUtil.get("i18n.execution_frequency.d014"), heartSecond));
            taskStat.onFailed(TASK_ID, throwable);
        }
    }


    private void checkList(List<MachineNodeModel> machineNodeModels) {
        if (CollUtil.isEmpty(machineNodeModels)) {
            return;
        }
        machineNodeModels.forEach(machineNodeModel -> {
            // 超时时间统一，避免长时间无响应
            machineNodeModel.setJpomTimeout(30);
            //
            assetsExecutorPoolService.execute(() -> {
                try {
                    BaseServerController.resetInfo(UserModel.EMPTY);
                    long timeMillis = SystemClock.now();
                    JsonMessage<JSONObject> message = NodeForward.request(machineNodeModel, NodeUrl.GetStatInfo, new JSONObject());
                    int networkTime = (int) (System.currentTimeMillis() - timeMillis);
                    JSONObject jsonObject;
                    if (message.success()) {
                        jsonObject = message.getData(JSONObject.class);
                    } else {
                        // 状态码错
                        this.updateStatus(machineNodeModel, 3, message.toString());
                        return;
                    }
                    jsonObject.put("networkDelay", networkTime);
                    this.saveStatInfo(machineNodeModel, jsonObject);
                } catch (AgentAuthorizeException agentException) {
                    this.updateStatus(machineNodeModel, 2, agentException.getMessage());
                } catch (AgentException e) {
                    this.updateStatus(machineNodeModel, 0, e.getMessage());
                } catch (Exception e) {
                    this.updateStatus(machineNodeModel, 0, e.getMessage());
                    log.error(I18nMessageUtil.get("i18n.get_node_monitoring_info_failure.595a"), e);
                } finally {
                    BaseServerController.removeEmpty();
                }
            });
        });
    }

    /**
     * 更新统计信息
     *
     * @param machineNode 机器数据
     * @param data        统计数据
     */
    private void saveStatInfo(MachineNodeModel machineNode, JSONObject data) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setId(machineNode.getId());
        String oshiError = data.getString("oshiError");
        if (StrUtil.isEmpty(oshiError)) {
            machineNodeModel.setStatus(1);
            machineNodeModel.setStatusMsg("ok");
        } else {
            machineNodeModel.setStatus(4);
            machineNodeModel.setStatusMsg(oshiError);
        }
        int networkDelay = data.getIntValue("networkDelay");
        int systemSleep = data.getIntValue("systemSleep");
        // 减去系统固定休眠时间
        networkDelay = networkDelay - systemSleep;
        machineNodeModel.setNetworkDelay(networkDelay);
        // jpom 相关信息
        JSONObject jpomInfo = data.getJSONObject("jpomInfo");
        Optional.ofNullable(jpomInfo).ifPresent(jsonObject -> {
            Optional.ofNullable(jsonObject.getJSONObject("jpomManifest"))
                .ifPresent(jsonObject1 -> {
                    machineNodeModel.setJpomVersion(jsonObject1.getString("version"));
                    machineNodeModel.setJpomBuildTime(jsonObject1.getString("timeStamp"));
                    machineNodeModel.setOsName(jsonObject1.getString("osName"));
                    machineNodeModel.setJpomUptime(jsonObject1.getLong("upTime"));
                    machineNodeModel.setInstallId(jsonObject1.getString("installId"));
                });
            machineNodeModel.setJpomProjectCount(jsonObject.getIntValue("projectCount"));
            machineNodeModel.setJpomScriptCount(jsonObject.getIntValue("scriptCount"));
            //
            machineNodeModel.setJvmFreeMemory(jsonObject.getLongValue("freeMemory"));
            machineNodeModel.setJvmTotalMemory(jsonObject.getLongValue("totalMemory"));
            machineNodeModel.setJavaVersion(jsonObject.getString("javaVersion"));
        });
        // 基础状态信息
        MachineNodeStatLogModel machineNodeStatLogModel = new MachineNodeStatLogModel();
        machineNodeStatLogModel.setMachineId(machineNodeModel.getId());
        machineNodeStatLogModel.setNetworkDelay(networkDelay);
        //
        JSONObject extendInfo = new JSONObject();
        Optional.ofNullable(data.getJSONObject("simpleStatus")).ifPresent(jsonObject -> {
            machineNodeModel.setOsOccupyMemory(ObjectUtil.defaultIfNull(jsonObject.getDouble("memory"), -1D));
            machineNodeModel.setOsOccupyDisk(ObjectUtil.defaultIfNull(jsonObject.getDouble("disk"), -1D));
            machineNodeModel.setOsOccupyCpu(ObjectUtil.defaultIfNull(jsonObject.getDouble("cpu"), -1D));
            //
            machineNodeStatLogModel.setOccupyCpu(machineNodeModel.getOsOccupyCpu());
            machineNodeStatLogModel.setOccupyMemory(machineNodeModel.getOsOccupyMemory());
            machineNodeStatLogModel.setOccupyDisk(machineNodeModel.getOsOccupyDisk());
            machineNodeStatLogModel.setOccupySwapMemory(jsonObject.getDouble("swapMemory"));
            machineNodeStatLogModel.setOccupyVirtualMemory(jsonObject.getDouble("virtualMemory"));
            machineNodeStatLogModel.setNetTxBytes(jsonObject.getLong("netTxBytes"));
            machineNodeStatLogModel.setNetRxBytes(jsonObject.getLong("netRxBytes"));
            machineNodeStatLogModel.setMonitorTime(jsonObject.getLongValue("time"));
            //
            extendInfo.put("monitorIfsNames", jsonObject.getString("monitorIfsNames"));
        });
        // 系统信息
        Optional.ofNullable(data.getJSONObject("systemInfo")).ifPresent(jsonObject -> {
            machineNodeModel.setOsSystemUptime(jsonObject.getLong("systemUptime"));
            machineNodeModel.setOsVersion(jsonObject.getString("osVersion"));
            machineNodeModel.setHostName(jsonObject.getString("hostName"));
            machineNodeModel.setOsHardwareVersion(jsonObject.getString("hardwareVersion"));
            machineNodeModel.setHostIpv4s(CollUtil.join(jsonObject.getList("hostIpv4s", String.class), StrUtil.COMMA));
            machineNodeModel.setOsCpuIdentifierName(jsonObject.getString("osCpuIdentifierName"));
            machineNodeModel.setOsCpuCores(jsonObject.getInteger("osCpuCores"));
            machineNodeModel.setOsMoneyTotal(jsonObject.getLong("osMoneyTotal"));
            machineNodeModel.setOsSwapTotal(jsonObject.getLong("osSwapTotal"));
            machineNodeModel.setOsVirtualMax(jsonObject.getLong("osVirtualMax"));
            List<Double> osLoadAverage = jsonObject.getList("osLoadAverage", Double.class);
            if (osLoadAverage != null) {
                // 保留两位小数
                osLoadAverage = osLoadAverage.stream()
                    .map(aDouble -> NumberUtil.div(aDouble, (Double) 1D, 2))
                    .collect(Collectors.toList());
            }
            machineNodeModel.setOsLoadAverage(CollUtil.join(osLoadAverage, StrUtil.COMMA));
            machineNodeModel.setOsFileStoreTotal(jsonObject.getLong("osFileStoreTotal"));
        });
        machineNodeModel.setExtendInfo(extendInfo.toString());
        this.updateById(machineNodeModel);
        if (machineNodeStatLogModel.getMonitorTime() != null) {
            machineNodeStatLogServer.insert(machineNodeStatLogModel);
        }
        //
        Optional.ofNullable(jpomInfo).ifPresent(jsonObject -> {
            JSONObject workspaceStat = jsonObject.getJSONObject("workspaceStat");
            if (workspaceStat == null) {
                return;
            }
            for (Map.Entry<String, Object> entry : workspaceStat.entrySet()) {
                String key = entry.getKey();
                JSONObject value = (JSONObject) entry.getValue();
                int projectCount = value.getIntValue("projectCount", 0);
                int scriptCount = value.getIntValue("scriptCount", 0);
                Entity entity = Entity.create();
                entity.set("jpomProjectCount", projectCount);
                entity.set("jpomScriptCount", scriptCount);
                Entity where = Entity.create();
                where.set("machineId", machineNodeModel.getId());
                where.set("workspaceId", key);
                nodeService.update(entity, where);
            }
        });
    }

    /**
     * 更新机器状态
     *
     * @param machineNode 机器信息
     * @param status      状态
     * @param msg         状态消息
     */
    private void updateStatus(MachineNodeModel machineNode, int status, String msg) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setId(machineNode.getId());
        machineNodeModel.setStatus(status);
        machineNodeModel.setStatusMsg(msg);
        // 将信息置空，避免影响排序
        machineNodeModel.setNetworkDelay(-9999_999);
        machineNodeModel.setOsOccupyCpu(-99D);
        machineNodeModel.setOsOccupyMemory(-99D);
        machineNodeModel.setOsOccupyDisk(-99D);
        this.updateById(machineNodeModel);
    }

    private MachineNodeModel resolveMachineData(HttpServletRequest request) {
        // 创建对象
        MachineNodeModel machineNodeModel = ServletUtil.toBean(request, MachineNodeModel.class, true);
        Assert.hasText(machineNodeModel.getName(), I18nMessageUtil.get("i18n.machine_name_required.e8cf"));
        Assert.hasText(machineNodeModel.getJpomUrl(), I18nMessageUtil.get("i18n.please_fill_in_node_address.e77e"));
        Assert.hasText(machineNodeModel.getJpomUsername(), I18nMessageUtil.get("i18n.node_account_required.2d90"));

        Assert.hasText(machineNodeModel.getJpomProtocol(), I18nMessageUtil.get("i18n.protocol_required.b4f8"));
        //
        MachineNodeModel update = new MachineNodeModel();
        update.setId(machineNodeModel.getId());
        update.setGroupName(machineNodeModel.getGroupName());
        update.setName(machineNodeModel.getName());
        update.setJpomHttpProxy(machineNodeModel.getJpomHttpProxy());
        update.setJpomHttpProxyType(machineNodeModel.getJpomHttpProxyType());
        update.setJpomUrl(machineNodeModel.getJpomUrl());
        update.setJpomProtocol(machineNodeModel.getJpomProtocol());
        update.setJpomUsername(machineNodeModel.getJpomUsername());
        update.setJpomPassword(machineNodeModel.getJpomPassword());
        update.setJpomTimeout(machineNodeModel.getJpomTimeout());
        update.setTemplateNode(machineNodeModel.getTemplateNode());
        update.setTransportEncryption(machineNodeModel.getTransportEncryption());
        return update;
    }

    public boolean existsByUrl(String jpomUrl, String id) {
        Assert.hasText(jpomUrl, I18nMessageUtil.get("i18n.node_address_required.71f1"));
        //
        Entity entity = Entity.create();
        entity.set("jpomUrl", jpomUrl);
        if (StrUtil.isNotEmpty(id)) {
            entity.set("id", StrUtil.format(" <> {}", id));
        }
        return this.exists(entity);
    }

    public MachineNodeModel getByUrl(String jpomUrl) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setJpomUrl(jpomUrl);
        List<MachineNodeModel> machineNodeModels = this.listByBean(machineNodeModel);
        return CollUtil.getFirst(machineNodeModels);
    }

    public void update(HttpServletRequest request) {
        MachineNodeModel machineNodeModel = this.resolveMachineData(request);
        boolean exists = this.existsByUrl(machineNodeModel.getJpomUrl(), machineNodeModel.getId());
        Assert.state(!exists, I18nMessageUtil.get("i18n.node_already_exists.28ea"));
        this.testNode(machineNodeModel);
        // 更新状态
        machineNodeModel.setStatus(1);
        //
        this.testHttpProxy(machineNodeModel.getJpomHttpProxy());
        //
        if (StrUtil.isNotEmpty(machineNodeModel.getId())) {
            this.updateById(machineNodeModel);
        } else {
            this.insert(machineNodeModel);
        }
    }

    /**
     * 测试节点是否可以访问
     *
     * @param nodeModel 节点信息
     */
    public void testNode(MachineNodeModel nodeModel) {
        //
        int timeout = ObjectUtil.defaultIfNull(nodeModel.getJpomTimeout(), 0);
        // 检查是否可用默认为5秒，避免太长时间无法连接一直等待
        nodeModel.setJpomTimeout(5);
        //
        JsonMessage<JpomManifest> objectJsonMessage = NodeForward.request(nodeModel, StrUtil.EMPTY, NodeUrl.Info, "nodeId", nodeModel.getId());
        try {
            JpomManifest jpomManifest = objectJsonMessage.getData(JpomManifest.class);
            Assert.notNull(jpomManifest, I18nMessageUtil.get("i18n.node_connection_failure.896d"));
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.node_connection_failure.896d"), e);
            throw new IllegalStateException(I18nMessageUtil.get("i18n.node_return_info_exception.0961"));
        }
        //
        nodeModel.setJpomTimeout(timeout);
    }

    /**
     * 探测 http proxy 是否可用
     *
     * @param httpProxy http proxy
     */
    public void testHttpProxy(String httpProxy) {
        if (StrUtil.isNotEmpty(httpProxy)) {
            List<String> split = StrUtil.splitTrim(httpProxy, StrUtil.COLON);
            Assert.isTrue(CollUtil.size(split) == 2, I18nMessageUtil.get("i18n.invalid_http_proxy_address.1da1"));
            String host = split.get(0);
            int port = Convert.toInt(split.get(1), 0);
            Assert.isTrue(StrUtil.isNotEmpty(host) && NetUtil.isValidPort(port), I18nMessageUtil.get("i18n.invalid_http_proxy_address.1da1"));
            //
            try {
                NetUtil.netCat(host, port, StrUtil.EMPTY.getBytes());
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.http_proxy_address_unavailable.b3f2") + httpProxy, e);
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.http_proxy_address_unavailable.b3f2") + e.getMessage());
            }
        }
    }

    public void insertAndNode(MachineNodeModel machineNodeModel, String workspaceId) {
        this.insert(machineNodeModel);
        //
        this.insertNode(machineNodeModel, workspaceId);
    }

    /**
     * 根据机器添加 节点
     *
     * @param machineNodeModel 机器信息
     * @param workspaceId      工作空间
     */
    public void insertNode(MachineNodeModel machineNodeModel, String workspaceId) {
        NodeModel nodeModel = this.createModel(machineNodeModel, workspaceId);
        nodeService.insert(nodeModel);
    }

    private NodeModel createModel(MachineNodeModel machineNodeModel, String workspaceId) {
        NodeModel nodeModel = new NodeModel();
        nodeModel.setMachineId(machineNodeModel.getId());
        nodeModel.setWorkspaceId(workspaceId);
        nodeModel.setName(machineNodeModel.getName());
        nodeModel.setOpenStatus(1);
        nodeModel.setGroup(machineNodeModel.getGroupName());
        return nodeModel;
    }
}
