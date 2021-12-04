package io.jpom.service.node.script;

import com.alibaba.fastjson.JSONArray;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.service.node.NodeOld1Service;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @date 2019/8/16
 */
@Service
public class ScriptServer implements BaseDynamicService {


	private final NodeOld1Service nodeService;

	public ScriptServer(NodeOld1Service nodeService) {
		this.nodeService = nodeService;
	}

	@Override
	public JSONArray listToArray(String dataId) {
		NodeModel item = nodeService.getItem(dataId);
		if (item == null || !item.isOpenStatus()) {
			return null;
		}
		return listToArray(item);
	}

	public JSONArray listToArray(NodeModel nodeModel) {
		JSONArray jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Script_List, null, JSONArray.class);
		return filter(jsonArray, ClassFeature.SCRIPT);
	}
}
