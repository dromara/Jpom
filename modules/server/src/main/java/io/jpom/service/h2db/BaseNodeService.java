package io.jpom.service.h2db;

import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.BaseNodeModel;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.service.node.NodeService;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
public abstract class BaseNodeService<T extends BaseNodeModel> extends BaseWorkspaceService<T> {

	public PageResultDto<T> listPageNode(HttpServletRequest request) {
		// 验证工作空间权限
		Map<String, String> paramMap = ServletUtil.getParamMap(request);
		String workspaceId = this.getCheckUserWorkspace(request);
		paramMap.put("workspaceId", workspaceId);
		// 验证节点
		String nodeId = paramMap.get(BaseServerController.NODE_ID);
		Assert.notNull(nodeId, "没有选择节点ID");
		NodeService nodeService = SpringUtil.getBean(NodeService.class);
		NodeModel nodeModel = nodeService.getByKey(nodeId);
		Assert.notNull(nodeModel, "不存在对应的节点");
		paramMap.put("nodeId", nodeId);
		return super.listPage(paramMap);
	}
}
