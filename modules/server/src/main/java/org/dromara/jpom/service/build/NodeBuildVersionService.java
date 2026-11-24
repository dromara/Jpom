/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.build;

import cn.hutool.core.collection.CollUtil;
import org.dromara.jpom.model.data.NodeBuildVersionModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 构建节点版本
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
@Service
public class NodeBuildVersionService extends BaseWorkspaceService<NodeBuildVersionModel> {

    /**
     * 查询构建在节点的版本记录
     *
     * @param buildDataId   构建配置ID
     * @param buildNumberId 构建编号
     * @return list
     */
    public List<NodeBuildVersionModel> listByBuild(String buildDataId, Integer buildNumberId) {
        NodeBuildVersionModel where = new NodeBuildVersionModel();
        where.setBuildDataId(buildDataId);
        where.setBuildNumberId(buildNumberId);
        return this.listByBean(where);
    }

    /**
     * 查询构建在指定节点的版本记录
     *
     * @param buildDataId   构建配置ID
     * @param buildNumberId 构建编号
     * @param nodeId        节点ID
     * @param projectId     项目ID
     * @return model
     */
    public NodeBuildVersionModel getByProject(String buildDataId, Integer buildNumberId, String nodeId, String projectId) {
        NodeBuildVersionModel where = new NodeBuildVersionModel();
        where.setBuildDataId(buildDataId);
        where.setBuildNumberId(buildNumberId);
        where.setNodeId(nodeId);
        where.setProjectId(projectId);
        List<NodeBuildVersionModel> list = this.listByBean(where);
        return CollUtil.getFirst(list);
    }

    /**
     * 记录版本
     *
     * @param model 版本信息
     */
    public void saveVersion(NodeBuildVersionModel model) {
        NodeBuildVersionModel old = this.getByProject(model.getBuildDataId(), model.getBuildNumberId(), model.getNodeId(), model.getProjectId());
        if (old == null) {
            this.insert(model);
        } else {
            model.setId(old.getId());
            this.updateById(model);
        }
    }

    /**
     * 标记当前运行版本
     *
     * @param nodeId     节点ID
     * @param projectId  项目ID
     * @param versionId  当前版本
     */
    public void markCurrent(String nodeId, String projectId, String versionId) {
        NodeBuildVersionModel where = new NodeBuildVersionModel();
        where.setNodeId(nodeId);
        where.setProjectId(projectId);
        where.setStatus(2);
        List<NodeBuildVersionModel> list = this.listByBean(where);
        for (NodeBuildVersionModel item : list) {
            if (versionId != null && versionId.equals(item.getVersionId())) {
                continue;
            }
            NodeBuildVersionModel update = new NodeBuildVersionModel();
            update.setId(item.getId());
            update.setStatus(3);
            this.updateById(update);
        }
    }
}
