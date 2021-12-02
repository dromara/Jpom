package io.jpom.controller.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@Controller
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeIndexController extends BaseServerController {


	private List<NodeModel> listByGroup(String group) {
		List<NodeModel> nodeModels = nodeService.list();
		//
		if (nodeModels != null && StrUtil.isNotEmpty(group)) {
			// 筛选
			List<NodeModel> filterList = nodeModels.stream().filter(nodeModel -> StrUtil.equals(group, nodeModel.getGroup())).collect(Collectors.toList());
			if (CollUtil.isNotEmpty(filterList)) {
				// 如果传入的分组找到了节点，就返回  否则返回全部
				nodeModels = filterList;
			}
		}
		return nodeModels;
	}


	@RequestMapping(value = "list_data.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	@ResponseBody
	public String listJson(String group) {
		List<NodeModel> nodeModels = this.listByGroup(group);
		return JsonMessage.getString(200, "", nodeModels);
	}


	@RequestMapping(value = "list_group.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	@ResponseBody
	public String listAllGroup() {
		HashSet<String> allGroup = nodeService.getAllGroup();
		return JsonMessage.getString(200, "", allGroup);
	}

	@RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String nodeStatus() {
		long timeMillis = System.currentTimeMillis();
		JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Status, getRequest(), JSONObject.class);
		if (jsonObject == null) {
			return JsonMessage.getString(500, "获取信息失败");
		}
		JSONArray jsonArray = new JSONArray();
		jsonObject.put("timeOut", System.currentTimeMillis() - timeMillis);
		jsonArray.add(jsonObject);
		return JsonMessage.getString(200, "", jsonArray);
	}

	/**
	 * @param status 节点状态获取
	 * @return json
	 * @author Hotstrip
	 * load node project list
	 * 加载节点项目列表
	 */
	@RequestMapping(value = "node_project_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String nodeProjectList(@RequestParam(value = "status", defaultValue = "false") Boolean status) {
		List<NodeModel> nodeModels = null;
		if (status) {
			nodeModels = nodeService.listAndProjectAndStatus();
		} else {
			nodeModels = nodeService.listAndProject();
		}

		return JsonMessage.getString(200, "success", nodeModels);
	}


}
