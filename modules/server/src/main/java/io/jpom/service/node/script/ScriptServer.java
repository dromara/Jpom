package io.jpom.service.node.script;

import com.alibaba.fastjson.JSONArray;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.service.node.NodeService;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @date 2019/8/16
 */
@Service
public class ScriptServer {


	private final NodeService nodeService;

	public ScriptServer(NodeService nodeService) {
		this.nodeService = nodeService;
	}


	public JSONArray listToArray(NodeModel nodeModel) {
		return NodeForward.requestData(nodeModel, NodeUrl.Script_List, null, JSONArray.class);
	}
}
