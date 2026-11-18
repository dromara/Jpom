/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.configuration.MonitorConfig;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.model.data.NodeScriptModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.service.script.NodeScriptServer;
import org.dromara.jpom.util.JvmUtil;
import org.dromara.jpom.util.OshiUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件端节点状态采集。
 * <p>
 * 从 {@code IndexController#getDirectTop} 抽出，供两个入口复用：
 * <ul>
 *     <li>服务端拉取模式：{@code POST /get-stat-info}</li>
 *     <li>服务端推送模式：反向隧道 {@code PUSH} 帧的上报内容</li>
 * </ul>
 * 两个入口必须产出同一份结构，否则推送模式与拉取模式的节点详情会不一致。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
@Service
public class AgentStatInfoService {

    private final ProjectInfoService projectInfoService;
    private final NodeScriptServer nodeScriptServer;
    private final AgentConfig agentConfig;

    public AgentStatInfoService(ProjectInfoService projectInfoService,
                                NodeScriptServer nodeScriptServer,
                                AgentConfig agentConfig) {
        this.projectInfoService = projectInfoService;
        this.nodeScriptServer = nodeScriptServer;
        this.agentConfig = agentConfig;
    }

    /**
     * 采集本机状态（与 {@code /get-stat-info} 的 data 完全一致）
     *
     * @return 状态 JSON
     */
    public JSONObject statInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            Optional<MonitorConfig> monitorConfig = Optional.ofNullable(agentConfig).map(AgentConfig::getMonitor);
            JSONObject topInfo = OshiUtils.getSimpleInfo(monitorConfig.orElse(null));
            jsonObject.put("simpleStatus", topInfo);
            // 系统固定休眠时间
            jsonObject.put("systemSleep", OshiUtils.NET_STAT_SLEEP + OshiUtils.CPU_STAT_SLEEP);
            JSONObject systemInfo = OshiUtils.getSystemInfo();
            jsonObject.put("systemInfo", systemInfo);
        } catch (Throwable e) {
            log.error(I18nMessageUtil.get("i18n.oshi_system_monitoring_exception.5c1c"), e);
            jsonObject.put("oshiError", e.getMessage());
        }
        jsonObject.put("jpomInfo", this.jpomInfo());
        return jsonObject;
    }

    /**
     * 插件端自身信息（项目/脚本统计、JVM、清单）
     *
     * @return 信息 JSON
     */
    public JSONObject jpomInfo() {
        List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
        List<NodeScriptModel> list = nodeScriptServer.list();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("javaVirtualCount", JvmUtil.getJavaVirtualCount());
        JpomManifest instance = JpomManifest.getInstance();
        jsonObject.put("jpomManifest", instance);
        jsonObject.put("javaVersion", SystemUtil.getJavaRuntimeInfo().getVersion());
        //  获取JVM中内存总大小
        jsonObject.put("totalMemory", SystemUtil.getTotalMemory());
        jsonObject.put("freeMemory", SystemUtil.getFreeMemory());
        Map<String, JSONObject> workspaceMap = new HashMap<>(4);
        for (NodeProjectInfoModel model : nodeProjectInfoModels) {
            JSONObject workspace = workspaceMap.computeIfAbsent(model.getWorkspaceId(), id -> {
                JSONObject item = new JSONObject();
                item.put("projectCount", 0);
                item.put("scriptCount", 0);
                return item;
            });
            workspace.merge("projectCount", 1, (v1, v2) -> Integer.sum((Integer) v1, (Integer) v2));
        }
        jsonObject.put("projectCount", CollUtil.size(nodeProjectInfoModels));
        for (NodeScriptModel model : list) {
            JSONObject workspace = workspaceMap.computeIfAbsent(model.getWorkspaceId(), id -> {
                JSONObject item = new JSONObject();
                item.put("projectCount", 0);
                item.put("scriptCount", 0);
                return item;
            });
            workspace.merge("scriptCount", 1, (v1, v2) -> Integer.sum((Integer) v1, (Integer) v2));
        }
        jsonObject.put("scriptCount", CollUtil.size(list));
        jsonObject.put("workspaceStat", workspaceMap);
        return jsonObject;
    }
}
