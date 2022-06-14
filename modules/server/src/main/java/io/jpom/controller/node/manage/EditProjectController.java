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
package io.jpom.controller.node.manage;

import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.NodeDataPermission;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.system.WhitelistDirectoryService;
import io.jpom.system.ConfigBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 项目管理
 *
 * @author jiangzeyin
 * @since 2018/9/29
 */
@RestController
@RequestMapping(value = "/node/manage/")
@Feature(cls = ClassFeature.PROJECT)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class EditProjectController extends BaseServerController {

	private final ProjectInfoCacheService projectInfoCacheService;
	private final WhitelistDirectoryService whitelistDirectoryService;

	public EditProjectController(ProjectInfoCacheService projectInfoCacheService,
								 WhitelistDirectoryService whitelistDirectoryService) {
		this.projectInfoCacheService = projectInfoCacheService;
		this.whitelistDirectoryService = whitelistDirectoryService;
	}

	@RequestMapping(value = "getProjectData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getProjectData(@ValidatorItem String id) {
		JSONObject projectInfo = projectInfoCacheService.getItem(getNode(), id);
		return JsonMessage.getString(200, "", projectInfo);
	}

	/**
	 * get project access list
	 * 获取项目的白名单
	 *
	 * @return json
	 * @author Hotstrip
	 */
	@RequestMapping(value = "project-access-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String projectAccessList() {
		List<String> jsonArray = whitelistDirectoryService.getProjectDirectory(getNode());
		return JsonMessage.getString(200, "success", jsonArray);
	}

	/**
	 * 保存项目
	 *
	 * @param id id
	 * @return json
	 */
	@RequestMapping(value = "saveProject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String saveProject(String id) {
		// 防止和Jpom冲突
		if (StrUtil.isNotEmpty(ConfigBean.getInstance().applicationTag) && ConfigBean.getInstance().applicationTag.equalsIgnoreCase(id)) {
			return JsonMessage.getString(401, "当前项目id已经被Jpom占用");
		}
		NodeModel node = getNode();
		JsonMessage<Object> request = NodeForward.request(node, getRequest(), NodeUrl.Manage_SaveProject);
		if (request.getCode() == HttpStatus.OK.value()) {
			projectInfoCacheService.syncNode(node, id);
		}
		return request.toString();
	}


	/**
	 * 验证lib 暂时用情况
	 *
	 * @return json
	 */
	@RequestMapping(value = "judge_lib.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String saveProject() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_Jude_Lib).toString();
	}
}
