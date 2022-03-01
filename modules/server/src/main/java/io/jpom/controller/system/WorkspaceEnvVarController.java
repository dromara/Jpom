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
package io.jpom.controller.system;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.WorkspaceEnvVarModel;
import io.jpom.permission.SystemPermission;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.system.WorkspaceEnvVarService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bwcx_jzy
 * @since 2021/12/10
 */

@RestController
@Feature(cls = ClassFeature.SYSTEM_WORKSPACE)
@RequestMapping(value = "/system/workspace_env/")
@SystemPermission
public class WorkspaceEnvVarController extends BaseServerController {

	private final WorkspaceEnvVarService workspaceEnvVarService;

	public WorkspaceEnvVarController(WorkspaceEnvVarService workspaceEnvVarService) {
		this.workspaceEnvVarService = workspaceEnvVarService;
	}

	/**
	 * 分页列表
	 *
	 * @return json
	 */
	@PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String list() {
		PageResultDto<WorkspaceEnvVarModel> listPage = workspaceEnvVarService.listPage(getRequest());
		return JsonMessage.getString(200, "", listPage);
	}

	/**
	 * 编辑变量
	 *
	 * @param name        变量名称
	 * @param value       值
	 * @param description 描述
	 * @return json
	 */
	@PostMapping(value = "/edit", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String edit(String id, @ValidatorItem String name, @ValidatorItem String value, @ValidatorItem String description) {
		String workspaceId = workspaceEnvVarService.getCheckUserWorkspace(getRequest());
		this.checkInfo(id, name, workspaceId);
		//
		WorkspaceEnvVarModel workspaceModel = new WorkspaceEnvVarModel();
		workspaceModel.setName(name);
		workspaceModel.setValue(value);
		workspaceModel.setDescription(description);
		if (StrUtil.isEmpty(id)) {
			// 创建
			workspaceEnvVarService.insert(workspaceModel);
		} else {
			workspaceModel.setId(id);
			workspaceModel.setWorkspaceId(workspaceId);
			workspaceEnvVarService.update(workspaceModel);
		}
		return JsonMessage.getString(200, "操作成功");
	}

	private void checkInfo(String id, String name, String workspaceId) {
		Validator.validateGeneral(name, 1, 50, "变量名称 1-50 英文字母 、数字和下划线");
		//
		Entity entity = Entity.create();
		entity.set("name", name);
		entity.set("workspaceId", workspaceId);
		if (StrUtil.isNotEmpty(id)) {
			entity.set("id", StrUtil.format(" <> {}", id));
		}
		boolean exists = workspaceEnvVarService.exists(entity);
		Assert.state(!exists, "对应的变量名称已经存在啦");
	}


	/**
	 * 删除变量
	 *
	 * @param id 变量 ID
	 * @return json
	 */
	@GetMapping(value = "/delete", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String delete(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 删除信息
		workspaceEnvVarService.delByKey(id, getRequest());
		return JsonMessage.getString(200, "删除成功");
	}
}
