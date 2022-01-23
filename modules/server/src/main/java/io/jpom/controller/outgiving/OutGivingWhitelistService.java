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
package io.jpom.controller.outgiving;

import io.jpom.model.data.ServerWhitelist;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.SystemParametersServer;
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
		String id = ServerWhitelist.workspaceId(workspaceId);
		ServerWhitelist serverWhitelist = systemParametersServer.getConfig(id, ServerWhitelist.class);
		if (serverWhitelist == null) {
			// 兼容旧数据
			serverWhitelist = systemParametersServer.getConfigDefNewInstance(ServerWhitelist.ID, ServerWhitelist.class);
		}
		return serverWhitelist;
	}
}
