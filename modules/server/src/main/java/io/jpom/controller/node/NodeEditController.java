package io.jpom.controller.node;

import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.OutGivingServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeEditController extends BaseServerController {

	@Resource
	private OutGivingServer outGivingServer;
	@Resource
	private MonitorService monitorService;
	@Resource
	private BuildInfoService buildService;


	@PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String listJson() {
		PageResultDto<NodeModel> nodeModelPageResultDto = nodeService.listPage(getRequest());
		return JsonMessage.getString(200, "", nodeModelPageResultDto);
	}

	@RequestMapping(value = "node_status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String nodeStatus() {
		long timeMillis = System.currentTimeMillis();
		JSONObject jsonObject = NodeForward.requestData(getNode(), NodeUrl.Status, getRequest(), JSONObject.class);
		Assert.notNull(jsonObject, "获取信息失败");
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
//		if (status) {
//			nodeModels = nodeService.listAndProjectAndStatus();
//		} else {
//			nodeModels = nodeService.listAndProject();
//		}

		return JsonMessage.getString(200, "success", nodeModels);
	}

	@PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String save() {
		nodeService.update(getRequest());
		return JsonMessage.getString(200, "操作成功");
	}


	/**
	 * 删除节点
	 *
	 * @param id 节点id
	 * @return json
	 */
	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@OptLog(UserOperateLogV1.OptType.DelNode)
	@ResponseBody
	@Feature(method = MethodFeature.DEL)
	public String del(String id) {
		//  判断分发
		if (outGivingServer.checkNode(id)) {
			return JsonMessage.getString(400, "该节点存在分发项目，不能删除");
		}
		// 监控
		if (monitorService.checkNode(id)) {
			return JsonMessage.getString(400, "该节点存在监控项，不能删除");
		}
		if (buildService.checkNode(id)) {
			return JsonMessage.getString(400, "该节点存在构建项，不能删除");
		}
		nodeService.delByKey(id);
		// 删除授权
		//        List<UserModel> list = userService.list();
		//        if (list != null) {
		//            list.forEach(userModel -> {
		//                userService.updateItem(userModel);
		//            });
		//        }
		return JsonMessage.getString(200, "操作成功");
	}
}
