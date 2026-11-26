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

import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentUpgradeOverview;
import org.dromara.jpom.common.update.AgentUpgradePackageStatus;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.AgentFileModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 在线升级页升级包总览的回归：混合部署必须同时给出 Java 与 Go 两类包的状态。
 *
 * <p>背景：升级页此前用「当前页节点列表里有没有 Go 节点」决定唯一一份包的下载类型，混合集群
 * （Java + Go 插件端）因此只能升级 Go，Java 升级包永远拿不到新版本。这里锁定服务端的聚合口径：
 * 按资产表全量统计引擎、按引擎各自给出包状态与「是否已最新」。</p>
 *
 * @author jpom
 * @since 2.11.15.4
 */
public class AgentUpgradeOverviewServiceTest {

    private static MachineNodeModel node(String engine) {
        MachineNodeModel model = new MachineNodeModel();
        model.setJpomEngine(engine);
        return model;
    }

    private static AgentFileModel agentPackage(String version, String platform, String timeStamp) {
        AgentFileModel model = new AgentFileModel();
        model.setVersion(version);
        model.setPlatform(platform);
        model.setTimeStamp(timeStamp);
        model.setName("agent-" + version + ".jar");
        model.setSavePath("/tmp/" + version + ".jar");
        return model;
    }

    private static AgentUpgradeOverview overview(List<MachineNodeModel> nodes,
                                                 AgentFileModel javaPackage,
                                                 AgentFileModel goPackage,
                                                 String latestVersion) {
        MachineNodeServer machineNodeServer = Mockito.mock(MachineNodeServer.class);
        AgentPackageService agentPackageService = Mockito.mock(AgentPackageService.class);
        Mockito.when(machineNodeServer.listByBean(Mockito.any(MachineNodeModel.class))).thenReturn(nodes);
        Mockito.when(agentPackageService.get(AgentEngine.JAVA, false)).thenReturn(javaPackage);
        Mockito.when(agentPackageService.get(AgentEngine.GO, false)).thenReturn(goPackage);
        return new AgentUpgradeOverviewService(machineNodeServer, agentPackageService).overview(latestVersion);
    }

    private static AgentUpgradePackageStatus findPackage(AgentUpgradeOverview overview, String engine) {
        return overview.getPackages().stream()
            .filter(item -> engine.equals(item.getEngine()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 混合集群：Java 包落后、Go 包最新，两类都要出现且状态各自独立
     */
    @Test
    public void mixedClusterReportsBothPackageTypes() {
        AgentUpgradeOverview overview = overview(
            Arrays.asList(node("java"), node("java"), node("go"), node(null)),
            agentPackage("2.11.15.1", null, "2026-11-25 12:44:03"),
            agentPackage("2.11.15.3", "linux/amd64", "2026-11-26 11:46:45"),
            "2.11.15.3");
        Assertions.assertEquals(2, overview.getJavaCount());
        Assertions.assertEquals(1, overview.getGoCount());
        Assertions.assertEquals(1, overview.getUnknownCount());
        Assertions.assertEquals(2, overview.getPackages().size());
        AgentUpgradePackageStatus javaPackage = findPackage(overview, "java");
        Assertions.assertNotNull(javaPackage);
        Assertions.assertFalse(javaPackage.isMissing());
        Assertions.assertTrue(javaPackage.isLatestKnown());
        Assertions.assertFalse(javaPackage.isUpToDate());
        Assertions.assertEquals("2.11.15.1", javaPackage.getVersion());
        AgentUpgradePackageStatus goPackage = findPackage(overview, "go");
        Assertions.assertNotNull(goPackage);
        Assertions.assertTrue(goPackage.isUpToDate());
        Assertions.assertEquals("linux/amd64", goPackage.getPlatform());
    }

    /**
     * 纯 Java 集群：不出现 Go 包（与改造前的单包展示一致）
     */
    @Test
    public void pureJavaClusterOnlyReportsJavaPackage() {
        AgentUpgradeOverview overview = overview(
            Arrays.asList(node("java"), node("java")),
            agentPackage("2.11.15.3", null, "2026-11-26 11:46:45"),
            null,
            "2.11.15.3");
        Assertions.assertEquals(0, overview.getGoCount());
        Assertions.assertEquals(1, overview.getPackages().size());
        Assertions.assertTrue(findPackage(overview, "java").isUpToDate());
    }

    /**
     * 纯 Go 集群：只出现 Go 包（Java 包为空，避免页面误报需要 Java 升级包）
     */
    @Test
    public void pureGoClusterOnlyReportsGoPackage() {
        AgentUpgradeOverview overview = overview(
            Collections.singletonList(node("go")),
            null,
            agentPackage("2.11.15.3", "linux/amd64", "2026-11-26 11:46:45"),
            "2.11.15.3");
        Assertions.assertEquals(1, overview.getGoCount());
        Assertions.assertEquals(1, overview.getPackages().size());
        Assertions.assertEquals("go", overview.getPackages().get(0).getEngine());
    }

    /**
     * 包缺失：missing 必须为 true（前端据此置灰并提示先下载/上传）
     */
    @Test
    public void missingPackageIsMarked() {
        AgentUpgradeOverview overview = overview(
            Arrays.asList(node("java"), node("go")),
            null,
            null,
            "2.11.15.3");
        AgentUpgradePackageStatus javaPackage = findPackage(overview, "java");
        Assertions.assertTrue(javaPackage.isMissing());
        Assertions.assertNull(javaPackage.getVersion());
        Assertions.assertTrue(javaPackage.isLatestKnown());
        Assertions.assertFalse(javaPackage.isUpToDate());
        AgentUpgradePackageStatus goPackage = findPackage(overview, "go");
        Assertions.assertTrue(goPackage.isMissing());
        Assertions.assertNull(goPackage.getPlatform());
    }

    /**
     * 旧集群还没有识别出引擎（unknown）：沿用 Java 口径，不能返回空包列表
     */
    @Test
    public void unknownEnginesFallbackToJavaPackage() {
        AgentUpgradeOverview overview = overview(
            Collections.singletonList(node(null)),
            agentPackage("2.11.15.3", null, "2026-11-26 11:46:45"),
            null,
            "2.11.15.3");
        Assertions.assertEquals(1, overview.getUnknownCount());
        Assertions.assertEquals(1, overview.getPackages().size());
        Assertions.assertEquals("java", overview.getPackages().get(0).getEngine());
    }
}
