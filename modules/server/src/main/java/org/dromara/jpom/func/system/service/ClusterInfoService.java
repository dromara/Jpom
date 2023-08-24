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
package org.dromara.jpom.func.system.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.event.IAsyncLoad;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.func.assets.server.MachineDockerServer;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.func.assets.server.MachineSshServer;
import org.dromara.jpom.func.system.model.ClusterInfoModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/8/19
 */
@Service
@Slf4j
public class ClusterInfoService extends BaseDbService<ClusterInfoModel> implements IAsyncLoad, Runnable {


    private final ServerConfig.ClusterConfig clusterConfig;
    private static final String TASK_ID = "system_monitor_cluster";

    private final WorkspaceService workspaceService;

    public ClusterInfoService(ServerConfig serverConfig,
                              WorkspaceService workspaceService) {
        this.clusterConfig = serverConfig.getCluster();
        this.workspaceService = workspaceService;
    }

    /**
     * 获取当前集群
     *
     * @return 集群信息
     */
    public ClusterInfoModel getCurrent() {
        ClusterInfoModel clusterInfoModel = this.getByKey(JpomManifest.getInstance().getInstallId());
        Assert.notNull(clusterInfoModel, "当前集群不存在");
        return clusterInfoModel;
    }

    @Override
    public void startLoad() {
        // 启动心跳检测
        int heartSecond = clusterConfig.getHeartSecond();
        ScheduledExecutorService scheduler = JpomApplication.getScheduledExecutorService();
        scheduler.scheduleWithFixedDelay(this, 0, heartSecond, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        int heartSecond = clusterConfig.getHeartSecond();
        try {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, StrUtil.format("{} 秒执行一次", heartSecond));
            taskStat.onStart();
            //
            JpomManifest jpomManifest = JpomManifest.getInstance();
            String installId = jpomManifest.getInstallId();
            ClusterInfoModel byKey = this.getByKey(installId);
            if (byKey == null) {
                // 初始安装
                this.insert(this.createDefault(installId));
                // 自动绑定默认数据
                this.bindDefault(installId);
                return;
            }
            // 更新数据
            ClusterInfoModel clusterInfoModel = new ClusterInfoModel();
            clusterInfoModel.setId(byKey.getId());
            if (!StrUtil.equals(byKey.getClusterId(), clusterConfig.getId())) {
                log.warn("集群ID 发生变化：{} -> {}", byKey.getClusterId(), clusterConfig.getId());
                clusterInfoModel.setClusterId(clusterConfig.getId());
            }
            clusterInfoModel.setLocalHostName(NetUtil.getLocalHostName());
            clusterInfoModel.setLastHeartbeat(SystemClock.now());
            clusterInfoModel.setJpomVersion(jpomManifest.getVersion());
            this.updateById(clusterInfoModel);
            // 检查是否重复
            Entity entity = Entity.create();
            entity.set("clusterId", clusterConfig.getId());
            entity.set("id", StrUtil.format(" <> {}", installId));
            List<ClusterInfoModel> clusterInfoModels = this.listByEntity(entity);
            if (CollUtil.isNotEmpty(clusterInfoModels)) {
                for (ClusterInfoModel infoModel : clusterInfoModels) {
                    log.error("{} 集群ID冲突：{} {}", clusterConfig.getId(), infoModel.getId(), infoModel.getName());
                }
            }
            // 通知任务结束
            taskStat.onSucceeded();
        } catch (Throwable throwable) {
            CronUtils.TaskStat taskStat = CronUtils.getTaskStat(TASK_ID, StrUtil.format("{} 秒执行一次", heartSecond));
            taskStat.onFailed(TASK_ID, throwable);
        }
    }

    /**
     * 自动帮忙集群相关的默认数据
     *
     * @param installId 安装Id
     */
    private void bindDefault(String installId) {
        long count = this.count();
        if (count != 1) {
            log.debug("系统中存在多个集群,不需要自动绑定数据");
            return;
        }
        // 所以工作空间自动绑定集群Id
        String sql = "update " + workspaceService.getTableName() + " set clusterInfoId=?";
        workspaceService.execute(sql, installId);
        // 获取所有的资产分组
        List<String> list = this.listLinkGroups();
        String join = CollUtil.join(list, StrUtil.COMMA);
        //
        ClusterInfoModel clusterInfoModel = new ClusterInfoModel();
        clusterInfoModel.setId(installId);
        clusterInfoModel.setLinkGroup(join);
        this.updateById(clusterInfoModel);
    }

    /**
     * 查询集群可以管理的分组名
     *
     * @return list
     */
    public List<String> listLinkGroups() {
        MachineDockerServer machineDockerServer = SpringUtil.getBean(MachineDockerServer.class);
        MachineNodeServer machineNodeServer = SpringUtil.getBean(MachineNodeServer.class);
        MachineSshServer machineSshServer = SpringUtil.getBean(MachineSshServer.class);
        List<String> nodeGroup = machineNodeServer.listGroupName();
        List<String> sshGroup = machineSshServer.listGroupName();
        List<String> dockerGroup = machineDockerServer.listGroupName();
        //
        List<String> all = new ArrayList<>();
        CollUtil.addAll(all, nodeGroup);
        CollUtil.addAll(all, sshGroup);
        CollUtil.addAll(all, dockerGroup);
        //
        all = all.stream()
            .distinct()
            .collect(Collectors.toList());
        return all;
    }

    /**
     * 创建默认的集群数据
     *
     * @param installId 系统安装 id
     * @return 默认数据
     */
    private ClusterInfoModel createDefault(String installId) {
        ClusterInfoModel clusterInfoModel = new ClusterInfoModel();
        clusterInfoModel.setId(installId);
        clusterInfoModel.setName("默认集群");
        clusterInfoModel.setCreateUser(UserModel.SYSTEM_ADMIN);
        clusterInfoModel.setClusterId(clusterConfig.getId());
        clusterInfoModel.setLastHeartbeat(SystemClock.now());
        return clusterInfoModel;
    }

    /**
     * 判断集群是否在线
     *
     * @param clusterInfoModel 集群信息
     * @return true 在线
     */
    public boolean online(ClusterInfoModel clusterInfoModel) {
        if (clusterInfoModel == null) {
            return false;
        }
        Long lastHeartbeat = clusterInfoModel.getLastHeartbeat();
        if (lastHeartbeat == null) {
            return false;
        }
        long millis = TimeUnit.SECONDS.toMillis(clusterConfig.getHeartSecond());
        return lastHeartbeat > SystemClock.now() - millis;
    }
}
