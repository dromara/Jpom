/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.hutool.crypto.SecureUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.model.data.NodeModel;
import org.springframework.util.Assert;

/**
 * 节点 数据
 *
 * @author bwcx_jzy
 * @since 2021/12/05
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseNodeModel extends BaseWorkspaceModel {

    /**
     * 节点Id
     *
     * @see NodeModel
     */
    private String nodeId;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 工作空间名称
     */
    private String workspaceName;

    @Override
    public String toString() {
        return super.toString();
    }

    public String fullId() {
        String workspaceId = this.getWorkspaceId();

        String nodeId = this.getNodeId();

        String dataId = this.dataId();

        return BaseNodeModel.fullId(workspaceId, nodeId, dataId);
    }

    public static String fullId(String workspaceId, String nodeId, String dataId) {

        Assert.hasText(workspaceId, "workspaceId");

        Assert.hasText(workspaceId, "nodeId");

        Assert.hasText(workspaceId, "dataId");
        return SecureUtil.sha1(workspaceId + nodeId + dataId);
    }

    /**
     * 获取数据ID
     *
     * @return 数据ID
     */
    public abstract String dataId();

    /**
     * 设置数据ID
     *
     * @param id 数据ID
     */
    public abstract void dataId(String id);
}
