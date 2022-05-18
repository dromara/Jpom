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
package io.jpom.service.node.script;

import cn.hutool.db.Entity;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ScriptCacheModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2019/8/16
 */
@Service
public class NodeScriptServer extends BaseNodeService<ScriptCacheModel> {


	public NodeScriptServer(NodeService nodeService,
							WorkspaceService workspaceService) {
		super(nodeService, workspaceService, "脚本模版");
	}

	/**
	 * 查询操作脚本 模版的节点
	 *
	 * @return nodeId list
	 */
	public List<String> hasScriptNode() {
		String sql = "select nodeId from " + super.getTableName() + " group by nodeId ";
		List<Entity> query = super.query(sql);
		if (query == null) {
			return null;
		}
		return query.stream().map(entity -> entity.getStr("nodeId")).collect(Collectors.toList());
	}

	@Override
	public JSONObject getItem(NodeModel nodeModel, String id) {
		return null;
	}

	@Override
	public JSONArray getLitDataArray(NodeModel nodeModel) {
		return NodeForward.requestData(nodeModel, NodeUrl.Script_List, null, JSONArray.class);
	}
}
