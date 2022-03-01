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
package io.jpom.controller.node;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.stat.NodeStatModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
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
//		{
//			// 启用状态
//			String sql = "select `openStatus`,count(1) as cunt from " + nodeService.getTableName() + " where workspaceId=? group by `openStatus`";
//			List<Entity> list = nodeStatService.query(sql, workspaceId);
//			Map<String, Integer> map = CollStreamUtil.toMap(list, entity -> entity.getStr("openStatus"), entity -> entity.getInt("cunt"));
//			jsonObject.put("openStatus", map);
//		}
		return JsonMessage.getString(200, "", jsonObject);
	}
}
