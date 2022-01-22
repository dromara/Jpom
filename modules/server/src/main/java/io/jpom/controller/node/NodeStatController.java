package io.jpom.controller.node;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.stat.NodeStatModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.stat.NodeStatService;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2022/1/22
 */
@RestController
@RequestMapping(value = "/node/stat")
@Feature(cls = ClassFeature.NODE_STAT)
public class NodeStatController extends BaseServerController {

	private final NodeStatService nodeStatService;

	public NodeStatController(NodeStatService nodeStatService) {
		this.nodeStatService = nodeStatService;
	}

	@PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String listJson() {
		PageResultDto<NodeStatModel> nodeModelPageResultDto = nodeStatService.listPage(getRequest());
		return JsonMessage.getString(200, "", nodeModelPageResultDto);
	}

	@GetMapping(value = "status_stat.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String statusStat() {
		String workspaceId = nodeStatService.getCheckUserWorkspace(getRequest());
		//
		int heartSecond = ServerExtConfigBean.getInstance().getNodeHeartSecond();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("heartSecond", heartSecond);
		{
			// 节点状态
			String sql = "select `status`,count(1) as cunt from " + nodeStatService.getTableName() + " where workspaceId=? group by `status`";
			List<Entity> list = nodeStatService.query(sql, workspaceId);
			Map<String, Integer> map = CollStreamUtil.toMap(list, entity -> entity.getStr("status"), entity -> entity.getInt("cunt"));
			jsonObject.put("status", map);
		}
		{
			// 启用状态
			String sql = "select `openStatus`,count(1) as cunt from " + nodeService.getTableName() + " where workspaceId=? group by `openStatus`";
			List<Entity> list = nodeStatService.query(sql, workspaceId);
			Map<String, Integer> map = CollStreamUtil.toMap(list, entity -> entity.getStr("openStatus"), entity -> entity.getInt("cunt"));
			jsonObject.put("openStatus", map);
		}
		return JsonMessage.getString(200, "", jsonObject);
	}
}
