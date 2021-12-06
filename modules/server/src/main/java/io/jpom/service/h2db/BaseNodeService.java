/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
