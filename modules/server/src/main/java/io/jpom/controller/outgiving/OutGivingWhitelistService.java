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
