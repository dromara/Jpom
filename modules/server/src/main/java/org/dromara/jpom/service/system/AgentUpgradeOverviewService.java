/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.RemoteVersion;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentPackageResolver;
import org.dromara.jpom.common.update.AgentUpgradeOverview;
import org.dromara.jpom.common.update.AgentUpgradePackageStatus;
import org.dromara.jpom.common.version.RemoteVersions;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.AgentFileModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 在线升级页的升级包总览服务
 *
 * <p>职责单一：把「集群里实际存在哪些插件端类型」与「服务端本地已有哪些升级包 / 远端最新版本是什么」
 * 聚合给前端。前端不再靠节点列表（分页、只有当前页）去猜集群是混合还是单一，也不会出现
 * 「展示 Java 升级包、下载动作却固定下 Go 升级容器」的情况。</p>
 *
 * @author jpom
 * @since 2.11.15.4
 */
@Slf4j
@Service
public class AgentUpgradeOverviewService {

    private final MachineNodeServer machineNodeServer;
    private final AgentPackageService agentPackageService;

    public AgentUpgradeOverviewService(MachineNodeServer machineNodeServer,
                                       AgentPackageService agentPackageService) {
        this.machineNodeServer = machineNodeServer;
        this.agentPackageService = agentPackageService;
    }

    /**
     * 统计集群需要的升级包
     *
     * @return 总览（引擎数量 + 每种引擎/平台的包状态）
     */
    public AgentUpgradeOverview overview() {
        return this.overview(null);
    }

    /**
     * 统计集群需要的升级包
     *
     * @param latestVersion 已读取的远端最新版本（调用方已读取时传入，避免重复请求下载中心；为空则本方法自行读取）
     * @return 总览（引擎数量 + 每种引擎/平台的包状态）
     */
    public AgentUpgradeOverview overview(String latestVersion) {
        AgentUpgradeOverview overview = new AgentUpgradeOverview();
        int javaCount = 0;
        int goCount = 0;
        int unknownCount = 0;
        List<MachineNodeModel> nodeList = machineNodeServer.listByBean(new MachineNodeModel());
        if (CollUtil.isNotEmpty(nodeList)) {
            for (MachineNodeModel node : nodeList) {
                String engine = StrUtil.trimToNull(node.getJpomEngine());
                if (engine == null) {
                    unknownCount++;
                } else if (StrUtil.equalsIgnoreCase(engine, AgentEngine.GO.getName())) {
                    goCount++;
                } else {
                    javaCount++;
                }
            }
        }
        overview.setJavaCount(javaCount);
        overview.setGoCount(goCount);
        overview.setUnknownCount(unknownCount);
        if (StrUtil.isBlank(latestVersion)) {
            latestVersion = this.latestVersion();
        }
        overview.setLatestVersion(latestVersion);
        List<AgentUpgradePackageStatus> packages = new ArrayList<>(2);
        // 只要集群里没有 Go 插件端就需要 Java 升级包（含「引擎尚未识别」的老节点：它们默认是 Java 插件端，
        // 保持老集群的原有升级包口径，不能因为 unknown 计数让页面什么包都不提示）
        boolean needJava = javaCount > 0 || goCount == 0;
        if (needJava) {
            packages.add(this.status(AgentEngine.JAVA, null, latestVersion));
        }
        if (goCount > 0) {
            AgentFileModel goPackage = agentPackageService.get(AgentEngine.GO, false);
            String platform = goPackage == null ? null : AgentPackageResolver.normalizePlatform(goPackage.getPlatform());
            packages.add(this.status(AgentEngine.GO, platform, latestVersion));
        }
        overview.setPackages(packages);
        return overview;
    }

    /**
     * 读取远端最新版本（下载中心不可用时返回 null，前端按「未知」展示）
     *
     * @return 版本号
     */
    private String latestVersion() {
        try {
            RemoteVersion remoteVersion = RemoteVersions.infoForUpgrade();
            if (remoteVersion == null) {
                return null;
            }
            return StrUtil.removePrefixIgnoreCase(StrUtil.trimToNull(remoteVersion.getTagName()), "v");
        } catch (Exception e) {
            log.warn("{} {}", I18nMessageUtil.get("i18n.remote_version_unavailable.b7e1"), e.getMessage());
            return null;
        }
    }

    /**
     * 组装单个引擎（平台）的包状态
     *
     * @param engine        引擎
     * @param platform      平台（仅 Go 使用，可为 null）
     * @param latestVersion 远端最新版本（可为 null）
     * @return 包状态
     */
    private AgentUpgradePackageStatus status(AgentEngine engine, String platform, String latestVersion) {
        AgentUpgradePackageStatus status = new AgentUpgradePackageStatus();
        status.setEngine(engine.getName());
        status.setPlatform(platform);
        status.setLatestKnown(latestVersion != null);
        AgentFileModel packageModel = agentPackageService.get(engine, false);
        if (packageModel == null || StrUtil.isBlank(packageModel.getSavePath())) {
            status.setMissing(true);
            status.setUpToDate(false);
            return status;
        }
        String version = AgentPackageResolver.normalizeVersion(packageModel.getVersion());
        status.setVersion(version);
        status.setTimeStamp(packageModel.getTimeStamp());
        status.setFileName(packageModel.getName());
        status.setUpToDate(latestVersion != null && StrUtil.isNotBlank(version)
            && StrUtil.compareVersion(version, latestVersion) >= 0);
        return status;
    }
}
