/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.model.data.AgentWhitelist;
import org.dromara.jpom.model.data.NodeModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 授权
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Service
public class WhitelistDirectoryService {

    public AgentWhitelist getData(NodeModel model) {
        return NodeForward.requestData(model, NodeUrl.WhitelistDirectory_data, null, AgentWhitelist.class);
    }

    public AgentWhitelist getData(MachineNodeModel machineNodeModel) {
        return NodeForward.requestData(machineNodeModel, NodeUrl.WhitelistDirectory_data, null, AgentWhitelist.class);
    }

    /**
     * 获取项目路径授权
     *
     * @param model 实体
     * @return project
     */
    public List<String> getProjectDirectory(NodeModel model) {
        AgentWhitelist agentWhitelist = getData(model);
        if (agentWhitelist == null) {
            return null;
        }
        return agentWhitelist.getProject();
    }

}
