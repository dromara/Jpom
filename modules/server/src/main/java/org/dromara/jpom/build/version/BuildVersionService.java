/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build.version;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.NodeBuildVersionModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.enums.BuildReleaseMethod;
import org.dromara.jpom.service.build.NodeBuildVersionService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.sshproject.SshProjectVersionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 构建节点版本：发布前后与插件端交互，维护「构建历史 ↔ 节点版本」索引
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
@Slf4j
@Service
public class BuildVersionService {

    /**
     * 版本状态：已下发
     */
    public static final int STATUS_RELEASED = 1;
    /**
     * 版本状态：当前运行
     */
    public static final int STATUS_CURRENT = 2;
    /**
     * 版本状态：已废弃
     */
    public static final int STATUS_DISCARD = 3;

    /**
     * 版本快照默认保留个数
     * <p>
     * 构建配置只打开「版本快照」开关、保留个数留空（默认 0）时使用。
     * 历史上把 0 直接透传给插件端，插件端据此判定为「未启用」，于是发布成功但一条版本记录都不写，
     * 快速回滚入口点进去永远是「没有记录」（2026-11-19 群反馈）。
     */
    public static final int DEFAULT_VERSION_KEEP_COUNT = 3;

    private final NodeBuildVersionService nodeBuildVersionService;
    private final NodeService nodeService;
    private final SshProjectVersionService sshProjectVersionService;

    public BuildVersionService(NodeBuildVersionService nodeBuildVersionService,
                               NodeService nodeService,
                               SshProjectVersionService sshProjectVersionService) {
        this.nodeBuildVersionService = nodeBuildVersionService;
        this.nodeService = nodeService;
        this.sshProjectVersionService = sshProjectVersionService;
    }

    /**
     * 该构建历史是否发布到 SSH 项目（SSH 项目的版本快照在远端目录，与插件端项目是两套落地方式）
     *
     * @param buildHistoryLog 构建历史
     * @return true 是 SSH 项目
     */
    public static boolean isSshProject(BuildHistoryLog buildHistoryLog) {
        return buildHistoryLog != null
            && buildHistoryLog.getReleaseMethod() != null
            && buildHistoryLog.getReleaseMethod() == BuildReleaseMethod.SshProject.getCode();
    }

    /**
     * 构建配置是否开启版本快照
     *
     * @param buildExtraModule 构建信息
     * @return true 开启
     */
    public boolean enabled(BuildExtraModule buildExtraModule) {
        return buildExtraModule != null && Boolean.TRUE.equals(buildExtraModule.getVersionSnapshot());
    }

    /**
     * 归一版本保留个数：0 / 空一律按默认值处理，避免「开关已开但保留个数为 0」时静默不生效
     *
     * @param keepCount 构建配置里的保留个数
     * @return 实际生效的保留个数
     */
    public static int resolveKeepCount(Integer keepCount) {
        return (keepCount == null || keepCount <= 0) ? DEFAULT_VERSION_KEEP_COUNT : keepCount;
    }

    /**
     * 插件端返回体是否真正启用了版本快照
     * <p>
     * 插件端在 {@code jpom.project.version-count <= 0}（或老版本没有该能力）时返回 success + enabled=false，
     * 只看 success 会打印「已登记版本快照」但实际上既没有快照也没有索引记录，必须同时校验 enabled。
     *
     * @param jsonMessage 插件端返回
     * @return true 真正生效
     */
    public static boolean effective(JsonMessage<JSONObject> jsonMessage) {
        if (jsonMessage == null || !jsonMessage.success()) {
            return false;
        }
        JSONObject data = jsonMessage.getData();
        return data != null && Boolean.TRUE.equals(data.getBoolean("enabled"));
    }

    /**
     * 解析节点信息
     *
     * @param nodeId 节点ID
     * @return node
     */
    public NodeModel resolveNode(String nodeId) {
        return nodeService.getByKey(nodeId);
    }

    /**
     * 发布前准备
     *
     * @param nodeModel     节点
     * @param projectId     项目ID
     * @param buildNumberId 构建编号
     * @param keepCount     保留个数
     * @return json
     */
    public JsonMessage<JSONObject> begin(NodeModel nodeModel, String projectId, Integer buildNumberId, Integer keepCount) {
        JSONObject data = new JSONObject();
        data.put("id", projectId);
        data.put("buildNumberId", buildNumberId);
        data.put("keepCount", resolveKeepCount(keepCount));
        return NodeForward.request(nodeModel, NodeUrl.Manage_File_Version_Begin, data);
    }

    /**
     * 发布成功后登记版本（同时写入索引表）
     *
     * @param nodeModel     节点
     * @param projectId     项目ID
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置ID
     * @param keepCount     保留个数
     * @param resultDirFile 服务端产物路径
     * @return json
     */
    public JsonMessage<JSONObject> commit(NodeModel nodeModel, String projectId, Integer buildNumberId,
                                          String buildDataId, Integer keepCount, String resultDirFile) {
        JSONObject data = new JSONObject();
        data.put("id", projectId);
        data.put("buildNumberId", buildNumberId);
        data.put("buildDataId", buildDataId);
        data.put("keepCount", resolveKeepCount(keepCount));
        JsonMessage<JSONObject> jsonMessage = NodeForward.request(nodeModel, NodeUrl.Manage_File_Version_Commit, data);
        if (!jsonMessage.success()) {
            return jsonMessage;
        }
        JSONObject version = jsonMessage.getData();
        if (version == null || !Boolean.TRUE.equals(version.getBoolean("enabled"))) {
            // 插件端未开启版本快照
            return jsonMessage;
        }
        NodeBuildVersionModel model = new NodeBuildVersionModel();
        model.setWorkspaceId(nodeModel.getWorkspaceId());
        model.setBuildDataId(buildDataId);
        model.setBuildNumberId(buildNumberId);
        model.setNodeId(nodeModel.getId());
        model.setProjectId(projectId);
        model.setVersionId(version.getString("versionId"));
        model.setVersionDir(version.getString("versionDir"));
        model.setManifest(version.getString("manifest"));
        model.setFileSize(version.getLong("fileSize"));
        model.setFileCount(version.getInteger("fileCount"));
        model.setStatus(STATUS_CURRENT);
        model.setResultDirFile(resultDirFile);
        nodeBuildVersionService.saveVersion(model);
        nodeBuildVersionService.markCurrent(nodeModel.getId(), projectId, version.getString("versionId"));
        return jsonMessage;
    }

    /**
     * 切换节点版本（快速回滚）
     *
     * @param nodeModel     节点
     * @param projectId     项目ID
     * @param versionId     版本ID
     * @param restart       是否重启项目
     * @param keepCount     保留个数
     * @return json
     */
    public JsonMessage<JSONObject> switchVersion(NodeModel nodeModel, String projectId, String versionId, boolean restart, Integer keepCount) {
        JSONObject data = new JSONObject();
        data.put("id", projectId);
        data.put("versionId", versionId);
        data.put("after", restart ? 1 : 0);
        data.put("snapshotCurrent", true);
        data.put("keepCount", keepCount);
        return NodeForward.request(nodeModel, NodeUrl.Manage_File_Version_Switch, data);
    }

    /**
     * 查询节点上的版本列表
     *
     * @param nodeModel 节点
     * @param projectId 项目ID
     * @return json
     */
    public JsonMessage<JSONObject> listVersion(NodeModel nodeModel, String projectId) {
        JSONObject data = new JSONObject();
        data.put("id", projectId);
        return NodeForward.request(nodeModel, NodeUrl.Manage_File_Version_List, data);
    }

    /**
     * 汇总某个构建历史在各节点的版本情况（索引表 + 节点实时校验）
     *
     * @param buildHistoryLog 构建历史
     * @return json
     */
    public JSONObject listByLog(BuildHistoryLog buildHistoryLog) {
        if (isSshProject(buildHistoryLog)) {
            // SSH 项目：版本快照在目标机器上，直接按构建编号过滤远端版本
            return sshProjectVersionService.listByBuildLog(buildHistoryLog);
        }
        Integer buildNumberId = ObjectUtil.defaultIfNull(buildHistoryLog.getFromBuildNumberId(), buildHistoryLog.getBuildNumberId());
        List<NodeBuildVersionModel> list = nodeBuildVersionService.listByBuild(buildHistoryLog.getBuildDataId(), buildNumberId);
        JSONArray array = new JSONArray();
        for (NodeBuildVersionModel model : list) {
            JSONObject item = JSONObject.from(model);
            item.put("buildNumberId", buildNumberId);
            item.put("buildNo", model.getBuildNumberId());
            NodeModel nodeModel = this.resolveNode(model.getNodeId());
            if (nodeModel == null) {
                item.put("exist", false);
                item.put("reachable", false);
                array.add(item);
                continue;
            }
            try {
                JsonMessage<JSONObject> jsonMessage = this.listVersion(nodeModel, model.getProjectId());
                item.put("reachable", jsonMessage.success());
                item.put("exist", this.matchVersion(jsonMessage, model.getVersionId()));
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.node_version_query_error.9a52"), model.getNodeId(), model.getProjectId(), e);
                item.put("reachable", false);
                item.put("exist", false);
            }
            array.add(item);
        }
        JSONObject result = new JSONObject();
        result.put("buildNumberId", buildNumberId);
        result.put("list", array);
        return result;
    }

    /**
     * 节点返回的版本列表中是否存在指定版本
     *
     * @param jsonMessage 节点返回
     * @param versionId   版本ID
     * @return true 存在
     */
    private boolean matchVersion(JsonMessage<JSONObject> jsonMessage, String versionId) {
        if (!jsonMessage.success() || jsonMessage.getData() == null) {
            return false;
        }
        JSONArray list = jsonMessage.getData().getJSONArray("list");
        if (list == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            JSONObject item = list.getJSONObject(i);
            if (item != null && StrUtil.equals(versionId, item.getString("versionId")) && Boolean.TRUE.equals(item.getBoolean("exist"))) {
                return true;
            }
        }
        return false;
    }

}
