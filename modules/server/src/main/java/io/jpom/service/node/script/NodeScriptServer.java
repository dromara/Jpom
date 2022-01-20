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
 * @date 2019/8/16
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
