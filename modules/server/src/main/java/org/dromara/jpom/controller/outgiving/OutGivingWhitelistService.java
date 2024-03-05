/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.outgiving;

import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author bwcx_jzy
 * @since 2022/1/23
 */
@Service
public class OutGivingWhitelistService {

    private final SystemParametersServer systemParametersServer;
    private final NodeService nodeService;

    public OutGivingWhitelistService(SystemParametersServer systemParametersServer,
                                     NodeService nodeService) {
        this.systemParametersServer = systemParametersServer;
        this.nodeService = nodeService;
    }


    public ServerWhitelist getServerWhitelistData(HttpServletRequest request) {
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        return this.getServerWhitelistData(workspaceId);
    }

    public ServerWhitelist getServerWhitelistData(String workspaceId) {
        String id = ServerWhitelist.workspaceId(workspaceId);
        ServerWhitelist serverWhitelist = systemParametersServer.getConfigDefNewInstance(id, ServerWhitelist.class);
        if (serverWhitelist == null) {
            // 兼容旧数据
            serverWhitelist = systemParametersServer.getConfigDefNewInstance(ServerWhitelist.ID, ServerWhitelist.class);
        }
        return serverWhitelist;
    }
}
