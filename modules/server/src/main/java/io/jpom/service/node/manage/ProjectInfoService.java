package io.jpom.service.node.manage;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.BaseDynamicService;
import io.jpom.plugin.ClassFeature;
import io.jpom.service.node.NodeService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 项目的简单操作
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Service
public class ProjectInfoService implements BaseDynamicService {


	private final NodeService nodeService;

	public ProjectInfoService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	@Override
	public JSONArray listToArray(String dataId) {
		NodeModel item = nodeService.getByKey(dataId);
		return listAll(item, null);
	}


	public JSONArray listAll(NodeModel nodeModel, HttpServletRequest request) {
		if (nodeModel == null || !nodeModel.isOpenStatus()) {
			return null;
		}
		JSONArray jsonArray;
		if (request != null) {
			jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectInfo, request, JSONArray.class);
		} else {
			jsonArray = NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectInfo, JSONArray.class, "notStatus", "true");
		}
		return filter(jsonArray, ClassFeature.PROJECT);
	}

	public List<String> getAllGroup(NodeModel nodeModel) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectGroup, null, List.class);
	}

	public JSONObject getItem(NodeModel nodeModel, String id) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_GetProjectItem, JSONObject.class, "id", id);
	}

	public JSONObject getLogSize(NodeModel nodeModel, String id, String copyId) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_Log_LogSize, JSONObject.class, "id", id, "copyId", copyId);
	}

	public JSONObject getItem(String nodeId, String id) {
		NodeModel item = nodeService.getByKey(id);
		return NodeForward.requestData(item, NodeUrl.Manage_GetProjectItem, JSONObject.class, "id", id);
	}

	public JSONArray getJdkList(NodeModel nodeModel, HttpServletRequest request) {
		return NodeForward.requestData(nodeModel, NodeUrl.Manage_jdk_list, request, JSONArray.class);
	}
}
