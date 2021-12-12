package io.jpom.service.node.script;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ScriptModel;
import io.jpom.service.h2db.BaseNodeService;
import io.jpom.service.node.NodeService;
import io.jpom.service.system.WorkspaceService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @date 2019/8/16
 */
@Service
public class ScriptServer extends BaseNodeService<ScriptModel> {


	public ScriptServer(NodeService nodeService,
						WorkspaceService workspaceService) {
		super(nodeService, workspaceService, "脚本模版");
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
