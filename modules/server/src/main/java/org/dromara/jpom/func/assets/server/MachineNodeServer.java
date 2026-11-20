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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentPackageResolver;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.exception.AgentAuthorizeException;
import org.dromara.jpom.exception.AgentException;
import org.dromara.jpom.func.assets.AssetsExecutorPoolService;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.model.MachineNodeStatLogModel;
import org.dromara.jpom.func.system.service.ClusterInfoService;
import org.dromara.jpom.func.tunnel.TunnelPushService;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.transport.INodeInfo;
import org.dromara.jpom.transport.IUrlItem;
import org.dromara.jpom.transport.TransportServerFactory;
import org.dromara.jpom.system.db.InitDb;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final TunnelPushService tunnelPushService;
    /**
     * 推送模式节点连续未上报状态的次数（达到阈值自动回退拉取，避免插件端回退版本后永久异常）
     */
    private final Map<String, Integer> pushMissCount = new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * 每个节点最近一次已落库的推送：避免同一份推送在多个心跳周期里重复写统计日志
     */
    private final PushSnapshotTracker pushSnapshotTracker = new PushSnapshotTracker();

    private static final String TASK_ID = "system_monitor_node";

    /**
     * 连续多少个心跳周期没有推送即回退拉取
     */
    private static final int PUSH_MISS_LIMIT = 3;

    public MachineNodeServer(NodeService nodeService,
                             ServerConfig serverConfig,
                             MachineNodeStatLogServer machineNodeStatLogServer,
                             ClusterInfoService clusterInfoService,
                             AssetsExecutorPoolService assetsExecutorPoolService,
                             TunnelPushService tunnelPushService) {
        this.nodeService = nodeService;
        this.nodeConfig = serverConfig.getNode();
        this.machineNodeStatLogServer = machineNodeStatLogServer;
        this.clusterInfoService = clusterInfoService;
        this.assetsExecutorPoolService = assetsExecutorPoolService;
        this.tunnelPushService = tunnelPushService;
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
        // 迁移旧数据
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
        int heartSecond = nodeConfig.getHeartSecond();
        // 任务统计放在最前面：即使下面因为配置/数据问题提前结束，也能在任务统计中看到该任务在跑
        CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, StrUtil.format(I18nMessageUtil.get("i18n.execution_frequency.d014"), heartSecond));
        taskStat.onStart();
        try {
            Entity entity = new Entity();
            if (clusterInfoService.isMultiServer()) {
                List<String> linkGroups = clusterInfoService.currentLinkGroups();
                if (CollUtil.isEmpty(linkGroups)) {
                    // 未绑定分组时退化为监控全部分组，不能整轮跳过（否则节点状态会永久停在旧值）
                    log.warn(I18nMessageUtil.get("i18n.cluster_not_bound_group_monitor_all_assets.b885"));
                } else {
                    entity.set("groupName", linkGroups);
                }
            }
            List<MachineNodeModel> machineNodeModels = Optional.ofNullable(this.listByEntity(entity)).orElseGet(ArrayList::new);
            List<MachineNodeModel> pollMachines = new ArrayList<>();
            List<MachineNodeModel> pushMachines = new ArrayList<>();
            for (MachineNodeModel machineNodeModel : machineNodeModels) {
                if (isPushTransportMode(machineNodeModel.getTransportMode())) {
                    pushMachines.add(machineNodeModel);
                } else {
                    // transportMode 为空视为拉取，兼容历史数据、手工迁库
                    pollMachines.add(machineNodeModel);
                }
            }
            // 已收到主动推送的节点切到推送模式；多服务端部署时各实例只持有自己那条隧道的推送数据，
            // 切换会导致其它实例误判节点离线，因此多服务端保持拉取
            if (!this.clusterInfoService.isMultiServer()) {
                pollMachines.removeIf(this::switchToPushIfFresh);
            }
            // 清理已删除节点残留的推送状态，避免 Map 无界增长
            this.prunePushState(machineNodeModels);
            this.checkList(pollMachines);
            this.checkPushList(pushMachines);
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            taskStat.onFailed(TASK_ID, throwable);
        }
    }


    /**
     * 拉取模式下，如果该节点已经通过隧道主动上报了新鲜数据，则切换为推送模式
     * <p>
     * 只有真正收到推送的节点才会切换（老版本插件端不会推送，保持拉取），
     * 切换后由 {@link #checkPushList(List)} 消费推送快照，不再发起拉取请求。
     *
     * @param machineNodeModel 机器节点（transportMode 为空或 0）
     * @return true 已切换（本轮不再拉取）
     */
    private boolean switchToPushIfFresh(MachineNodeModel machineNodeModel) {
        TunnelPushService.Snapshot snapshot = this.tunnelPushService.fresh(machineNodeModel.getId());
        if (snapshot == null) {
            return false;
        }
        MachineNodeModel update = new MachineNodeModel();
        update.setId(machineNodeModel.getId());
        update.setTransportMode(1);
        this.updateById(update);
        log.info("节点[{}]已收到主动推送数据，传输方式切换为推送模式", machineNodeModel.getName());
        return true;
    }

    /**
     * 推送模式的节点状态刷新：消费插件端主动上报的最新快照
     * <p>
     * 推送数据过期（超过 {@code jpom.agent.tunnel.push-max-age-seconds}）按无法连接处理，
     * 与拉取模式请求失败的口径一致，不自动回退拉取（避免同一节点两种数据源来回抖动）。
     *
     * @param machineNodeModels 推送模式的机器列表
     */
    private void checkPushList(List<MachineNodeModel> machineNodeModels) {
        if (CollUtil.isEmpty(machineNodeModels)) {
            return;
        }
        machineNodeModels.forEach(machineNodeModel -> this.assetsExecutorPoolService.execute(() -> {
            try {
                TunnelPushService.Snapshot snapshot = this.tunnelPushService.fresh(machineNodeModel.getId());
                if (snapshot == null) {
                    this.updateStatus(machineNodeModel, 0, I18nMessageUtil.get("i18n.tunnel_push_data_timeout.7c1a"));
                    this.fallbackToPollIfMissed(machineNodeModel);
                    return;
                }
                this.pushMissCount.remove(machineNodeModel.getId());
                if (!this.pushSnapshotTracker.firstConsume(machineNodeModel.getId(), snapshot.getReceivedAt())) {
                    // 同一份推送不重复落库：只刷新“最后检测时间”，避免统计日志里出现同一 monitorTime 的重复行
                    MachineNodeModel touch = new MachineNodeModel();
                    touch.setId(machineNodeModel.getId());
                    this.updateById(touch);
                    return;
                }
                JSONObject jsonObject = JSON.parseObject(snapshot.getBody());
                if (jsonObject == null) {
                    this.updateStatus(machineNodeModel, 0, I18nMessageUtil.get("i18n.tunnel_push_data_invalid.2f5b"));
                    return;
                }
                fillPushNetworkDelay(jsonObject);
                this.saveStatInfo(machineNodeModel, jsonObject);
            } catch (Exception e) {
                this.updateStatus(machineNodeModel, 0, I18nMessageUtil.get("i18n.tunnel_push_data_invalid.2f5b"));
                log.warn("节点[{}]推送数据解析失败", machineNodeModel.getName(), e);
            }
        }));
    }

    /**
     * 推送模式的网络延迟口径：没有“服务端到节点”的请求往返，落库值为 0。
     * <p>
     * {@link #saveStatInfo(MachineNodeModel, JSONObject)} 会减去插件端上报的 {@code systemSleep}
     * （采集时的固定休眠 {@code NET_STAT_SLEEP + CPU_STAT_SLEEP}），这里必须填入同样的休眠时间，
     * 否则推送节点的 networkDelay 会变成负数（页面显示 -2000ms、监控曲线为负值）。
     *
     * @param jsonObject 推送数据（与 {@code /get-stat-info} 同构）
     */
    static void fillPushNetworkDelay(JSONObject jsonObject) {
        jsonObject.put("networkDelay", jsonObject.getIntValue("systemSleep"));
    }

    /**
     * 清理已不在当前监控范围（节点被删除）时残留的推送状态
     *
     * @param machineNodeModels 本轮机器列表
     */
    private void prunePushState(List<MachineNodeModel> machineNodeModels) {
        if (CollUtil.isEmpty(machineNodeModels)) {
            this.pushMissCount.clear();
            this.pushSnapshotTracker.retain(null);
            return;
        }
        Set<String> ids = machineNodeModels.stream().map(MachineNodeModel::getId).collect(Collectors.toSet());
        this.pushMissCount.keySet().retainAll(ids);
        this.pushSnapshotTracker.retain(ids);
    }

    /**
     * 推送连续中断达到阈值后回退为拉取模式
     * <p>
     * 用于插件端被回退到不支持推送的版本、或推送配置被关闭的场景：否则节点会永久停留在
     * “未上报状态”，只能改库恢复。回退后若再次收到推送，{@link #switchToPushIfFresh(MachineNodeModel)}
     * 会重新切回推送模式。
     *
     * @param machineNodeModel 推送模式的机器
     */
    private void fallbackToPollIfMissed(MachineNodeModel machineNodeModel) {
        int missed = this.pushMissCount.merge(machineNodeModel.getId(), 1, Integer::sum);
        if (missed < PUSH_MISS_LIMIT) {
            return;
        }
        MachineNodeModel update = new MachineNodeModel();
        update.setId(machineNodeModel.getId());
        update.setTransportMode(0);
        this.updateById(update);
        this.pushMissCount.remove(machineNodeModel.getId());
        this.pushSnapshotTracker.clear(machineNodeModel.getId());
        log.warn("节点[{}]连续 {} 个周期未收到主动推送，回退为服务端拉取模式", machineNodeModel.getName(), missed);
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
            machineNodeStatLogModel.setCpuTicks(StrUtil.toStringOrNull(jsonObject.getByPath("cpuInfo.ticks")));
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
     * 节点状态：检测超时（超过指定心跳周期没有刷新检测结果）
     */
    public static final int STATUS_CHECK_TIMEOUT = 5;

    /**
     * 折算节点状态：长时间没有刷新检测结果时，不能继续沿用"正常"
     *
     * @param machineNodeModels 机器列表
     */
    public void fillTimeoutStatus(List<MachineNodeModel> machineNodeModels) {
        if (CollUtil.isEmpty(machineNodeModels)) {
            return;
        }
        long timeoutMillis = TimeUnit.SECONDS.toMillis(nodeConfig.getHeartSecond()) * 3;
        long now = SystemClock.now();
        machineNodeModels.forEach(machineNodeModel -> this.fillTimeoutStatus(machineNodeModel, timeoutMillis, now));
    }

    /**
     * 折算单个节点的状态
     *
     * @param machineNodeModel 机器信息
     */
    public void fillTimeoutStatus(MachineNodeModel machineNodeModel) {
        long timeoutMillis = TimeUnit.SECONDS.toMillis(nodeConfig.getHeartSecond()) * 3;
        this.fillTimeoutStatus(machineNodeModel, timeoutMillis, SystemClock.now());
    }

    private void fillTimeoutStatus(MachineNodeModel machineNodeModel, long timeoutMillis, long now) {
        if (machineNodeModel == null) {
            return;
        }
        if (checkStatusTimeout(machineNodeModel.getStatus(), machineNodeModel.getModifyTimeMillis(), timeoutMillis, now)) {
            machineNodeModel.setStatus(STATUS_CHECK_TIMEOUT);
        }
    }

    /**
     * 是否由服务端拉取检测（transportMode 为空视为拉取，兼容历史数据/手工迁移的库）
     *
     * @param transportMode 传输方式
     * @return true 需要检测
     */
    public static boolean isPollTransportMode(Integer transportMode) {
        return transportMode == null || transportMode == 0;
    }

    /**
     * 是否由节点主动推送状态
     *
     * @param transportMode 传输方式
     * @return true 推送模式
     */
    public static boolean isPushTransportMode(Integer transportMode) {
        return transportMode != null && transportMode == 1;
    }

    /**
     * 节点状态是否已经超时（长时间没有刷新检测结果）
     *
     * @param status           当前状态
     * @param modifyTimeMillis 最后刷新时间
     * @param timeoutMillis    超时时间
     * @param now              当前时间
     * @return true 已经超时
     */
    public static boolean checkStatusTimeout(Integer status, Long modifyTimeMillis, long timeoutMillis, long now) {
        if (!ObjectUtil.equal(status, 1)) {
            return false;
        }
        if (modifyTimeMillis == null || modifyTimeMillis <= 0) {
            return false;
        }
        return now - modifyTimeMillis > timeoutMillis;
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
        // 构建节点能力：机器资产层的开关与并发上限（只增字段，老数据为空即不承担构建）
        update.setBuildEnabled(machineNodeModel.getBuildEnabled());
        update.setBuildMaxConcurrency(machineNodeModel.getBuildMaxConcurrency());
        return update;
    }

    /**
     * 查询机器上运行的插件端 /info（不落库，仅用于能力校验）
     *
     * @param machineNodeModel 机器
     * @return /info 的 data
     */
    private JSONObject queryInfo(MachineNodeModel machineNodeModel) {
        INodeInfo nodeInfo = NodeForward.coverNodeInfo(machineNodeModel);
        IUrlItem urlItem = NodeForward.parseUrlItem(nodeInfo, StrUtil.EMPTY, NodeUrl.Info);
        String body = TransportServerFactory.get().execute(nodeInfo, urlItem, null);
        JsonMessage<JSONObject> message = NodeForward.toJsonMessage(body, new TypeReference<JsonMessage<JSONObject>>() {
        });
        return message.getData();
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
        // 开启构建能力前校验引擎与能力：只有 Go 插件端（/info 声明 capabilities 含 build）才能承担构建
        if (machineNodeModel.getBuildEnabled() != null && machineNodeModel.getBuildEnabled() == 1) {
            Assert.state(machineNodeModel.getBuildMaxConcurrency() == null || machineNodeModel.getBuildMaxConcurrency() > 0,
                I18nMessageUtil.get("i18n.build_node_concurrency_invalid.5d18"));
            JSONObject info = this.queryInfo(machineNodeModel);
            AgentEngine agentEngine = AgentPackageResolver.resolveEngine(info);
            Assert.state(agentEngine == AgentEngine.GO, I18nMessageUtil.get("i18n.build_node_not_go_agent.8e4b"));
            Assert.state(AgentPackageResolver.hasCapability(info, AgentPackageResolver.CAPABILITY_BUILD),
                I18nMessageUtil.get("i18n.build_node_unsupported_capability.6b02"));
        }
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
