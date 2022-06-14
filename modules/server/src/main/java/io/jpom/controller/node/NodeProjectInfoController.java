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

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.NodeModel;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.SystemPermission;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 节点管理
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/node")
@Feature(cls = ClassFeature.NODE)
public class NodeProjectInfoController extends BaseServerController {

	private final ProjectInfoCacheService projectInfoCacheService;

	public NodeProjectInfoController(ProjectInfoCacheService projectInfoCacheService) {
		this.projectInfoCacheService = projectInfoCacheService;
	}

	/**
	 * @return json
	 * @author Hotstrip
	 * load node project list
	 * 加载节点项目列表
	 */
	@PostMapping(value = "node_project_list", produces = MediaType.APPLICATION_JSON_VALUE)
	public String nodeProjectList() {
		PageResultDto<ProjectInfoCacheModel> resultDto = projectInfoCacheService.listPageNode(getRequest());
		return JsonMessage.getString(200, "success", resultDto);
	}


	/**
	 * load node project list
	 * 加载节点项目列表
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@PostMapping(value = "project_list", produces = MediaType.APPLICATION_JSON_VALUE)
	public String projectList() {
		PageResultDto<ProjectInfoCacheModel> resultDto = projectInfoCacheService.listPage(getRequest());
		return JsonMessage.getString(200, "success", resultDto);
	}

	/**
	 * load node project list
	 * 加载节点项目列表
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@GetMapping(value = "project_list_all", produces = MediaType.APPLICATION_JSON_VALUE)
	public String projectListAll() {
		List<ProjectInfoCacheModel> projectInfoCacheModels = projectInfoCacheService.listByWorkspace(getRequest());
		return JsonMessage.getString(200, "", projectInfoCacheModels);
	}

	/**
	 * 同步节点项目
	 *
	 * @return json
	 */
	@GetMapping(value = "sync_project", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
	public String syncProject(String nodeId) {
		NodeModel nodeModel = nodeService.getByKey(nodeId);
		Assert.notNull(nodeModel, "对应的节点不存在");
		int count = projectInfoCacheService.delCache(nodeId, getRequest());
		String msg = projectInfoCacheService.syncExecuteNode(nodeModel);
		return JsonMessage.getString(200, "主动清除：" + count + StrUtil.SPACE + msg);
	}

	/**
	 * 删除节点缓存的所有项目
	 *
	 * @return json
	 */
	@GetMapping(value = "clear_all_project", produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission(superUser = true)
	@Feature(cls = ClassFeature.PROJECT, method = MethodFeature.DEL)
	public String clearAll() {
		Entity where = Entity.create();
		where.set("id", " <> id");
		int del = projectInfoCacheService.del(where);
		return JsonMessage.getString(200, "成功删除" + del + "条项目缓存");
	}


}
