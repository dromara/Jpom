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
package io.jpom.func.assets.server;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.common.*;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.cron.IAsyncLoad;
import io.jpom.func.assets.model.MachineNodeModel;
import io.jpom.func.assets.model.MachineNodeStatLogModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.user.UserModel;
import io.jpom.service.h2db.BaseDbService;
import io.jpom.service.node.NodeService;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import io.jpom.system.ServerConfig;
import io.jpom.system.db.InitDb;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@Service
@Slf4j
public class MachineNodeServer extends BaseDbService<MachineNodeModel> implements ILoadEvent, IAsyncLoad, Runnable {

    private final NodeService nodeService;
    private final ServerConfig.NodeConfig nodeConfig;
    private final MachineNodeStatLogServer machineNodeStatLogServer;

    public MachineNodeServer(NodeService nodeService,
                             ServerConfig serverConfig,
                             MachineNodeStatLogServer machineNodeStatLogServer) {
        this.nodeService = nodeService;
        this.nodeConfig = serverConfig.getNode();
        this.machineNodeStatLogServer = machineNodeStatLogServer;
    }

    @Override
    protected void fillSelectResult(MachineNodeModel data) {
        Optional.ofNullable(data).ifPresent(machineNodeModel -> machineNodeModel.setJpomPassword(null));
    }

    @Override
    protected void fillInsert(MachineNodeModel machineNodeModel) {
        super.fillInsert(machineNodeModel);
        machineNodeModel.setGroupName(StrUtil.emptyToDefault(machineNodeModel.getGroupName(), Const.DEFAULT_GROUP_NAME));
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
            log.debug("节点机器表已经存在 {} 条数据，不需要修复机器数据", count);
            return;
        }
        List<NodeModel> list = nodeService.list(false);
        if (CollUtil.isEmpty(list)) {
            log.debug("没有任何节点信息,不需要修复机器数据");
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
                log.warn("节点地址 {} 存在多个数据，将自动合并使用 {} 节点的配置信息", entry.getKey(), first.getName());
            }
            machineNodeModels.add(this.nodeInfoToMachineNode(first));
        }
        this.insert(machineNodeModels);
        log.info("成功修复 {} 条机器节点数据", machineNodeModels.size());
        // 更新节点的机器id
        for (MachineNodeModel value : machineNodeModels) {
            Entity entity = Entity.create();
            entity.set("machineId", value.getId());
            Entity where = Entity.create();
            where.set("url", value.getJpomUrl());
            int update = nodeService.update(entity, where);
            Assert.state(update > 0, "更新节点表机器 id 失败：" + value.getName());
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
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "Jpom Node Monitor"));
        scheduler.scheduleAtFixedRate(this, 0, heartSecond, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setTransportMode(0);
        List<MachineNodeModel> machineNodeModels = this.listByBean(machineNodeModel);
        this.checkList(machineNodeModels);
    }


    private void checkList(List<MachineNodeModel> machineNodeModels) {
        if (CollUtil.isEmpty(machineNodeModels)) {
            return;
        }
        machineNodeModels.forEach(machineNodeModel -> {
            // 超时时间统一，避免长时间无响应
            machineNodeModel.setJpomTimeout(10);
            //
            ThreadUtil.execute(() -> {
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
                } catch (AuthorizeException agentException) {
                    this.updateStatus(machineNodeModel, 2, agentException.getMessage());
                } catch (AgentException e) {
                    this.updateStatus(machineNodeModel, 0, e.getMessage());
                } catch (Exception e) {
                    this.updateStatus(machineNodeModel, 0, e.getMessage());
                    log.error("获取节点监控信息失败", e);
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
        machineNodeModel.setStatus(1);
        machineNodeModel.setStatusMsg("ok");
        int networkDelay = data.getIntValue("networkDelay");
        int systemSleep = data.getIntValue("systemSleep");
        // 减去系统固定休眠时间
        networkDelay = networkDelay - systemSleep;
        machineNodeModel.setNetworkDelay(networkDelay);
        // jpom 相关信息
        Optional.ofNullable(data.getJSONObject("jpomInfo")).ifPresent(jsonObject -> {
            JSONObject jpomManifest = jsonObject.getJSONObject("jpomManifest");
            Optional.ofNullable(jpomManifest)
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
            machineNodeModel.setOsLoadAverage(CollUtil.join(jsonObject.getList("osLoadAverage", Double.class), StrUtil.COMMA));
            machineNodeModel.setOsFileStoreTotal(jsonObject.getLong("osFileStoreTotal"));
        });
        this.updateById(machineNodeModel);
        machineNodeStatLogServer.insert(machineNodeStatLogModel);
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
        Assert.hasText(machineNodeModel.getName(), "请填写机器名称");
        Assert.hasText(machineNodeModel.getJpomUrl(), "请填写 节点地址");
        Assert.hasText(machineNodeModel.getJpomUsername(), "请填写节点密码");
        Assert.hasText(machineNodeModel.getJpomProtocol(), "请选择协议");
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
        return update;
    }

    public boolean existsByUrl(String jpomUrl, String id) {
        Assert.hasText(jpomUrl, "节点地址不能为空");
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
        Assert.state(!exists, "对应的节点已经存在啦");
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
            Assert.notNull(jpomManifest, "节点连接失败，请检查节点是否在线");
        } catch (Exception e) {
            log.error("节点连接失败，请检查节点是否在线", e);
            throw new IllegalStateException("节点返回信息异常,请检查节点地址是否配置正确或者代理配置是否正确");
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
            Assert.isTrue(CollUtil.size(split) == 2, "HTTP代理地址格式不正确");
            String host = split.get(0);
            int port = Convert.toInt(split.get(1), 0);
            Assert.isTrue(StrUtil.isNotEmpty(host) && NetUtil.isValidPort(port), "HTTP代理地址格式不正确");
            //
            try {
                NetUtil.netCat(host, port, StrUtil.EMPTY.getBytes());
            } catch (Exception e) {
                log.warn("HTTP代理地址不可用:" + httpProxy, e);
                throw new IllegalArgumentException("HTTP代理地址不可用:" + e.getMessage());
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
