/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.controller.build;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.GitUtil;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

/**
 * @author Hotstrip
 * Repository controller
 */
@RestController
@Feature(cls = ClassFeature.BUILD_REPOSITORY)
public class RepositoryController extends BaseServerController {

	private final RepositoryService repositoryService;
	private final BuildInfoService buildInfoService;

	public RepositoryController(RepositoryService repositoryService,
								BuildInfoService buildInfoService) {
		this.repositoryService = repositoryService;
		this.buildInfoService = buildInfoService;
	}

	/**
	 * load repository list
	 *
	 * @return json
	 */
	@PostMapping(value = "/build/repository/list")
	@Feature(method = MethodFeature.LIST)
	public Object loadRepositoryList() {
		PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(getRequest());
		return JsonMessage.getString(200, "获取成功", pageResult);
	}

	/**
	 * load repository list
	 *
	 * @return json
	 */
	@GetMapping(value = "/build/repository/list_all")
	@Feature(method = MethodFeature.LIST)
	public Object loadRepositoryListAll() {
		List<RepositoryModel> repositoryModels = repositoryService.listByWorkspace(getRequest());
		return JsonMessage.getString(200, "", repositoryModels);
	}

	/**
	 * edit
	 *
	 * @param repositoryModelReq 仓库实体
	 * @return json
	 */
	@PostMapping(value = "/build/repository/edit")
	@Feature(method = MethodFeature.EDIT)
	public Object editRepository(RepositoryModel repositoryModelReq) {
		this.checkInfo(repositoryModelReq);
		// 检查 rsa 私钥
		boolean andUpdateSshKey = this.checkAndUpdateSshKey(repositoryModelReq);
		Assert.state(andUpdateSshKey, "rsa 私钥文件不存在或者有误");

		if (repositoryModelReq.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
			RepositoryModel repositoryModel = repositoryService.getByKey(repositoryModelReq.getId(), false);
			if (repositoryModel != null) {
				repositoryModelReq.setRsaPrv(StrUtil.emptyToDefault(repositoryModelReq.getRsaPrv(), repositoryModel.getRsaPrv()));
				repositoryModelReq.setPassword(StrUtil.emptyToDefault(repositoryModelReq.getPassword(), repositoryModel.getPassword()));
			}
			// 验证 git 仓库信息
			try {
				Tuple tuple = GitUtil.getBranchAndTagList(repositoryModelReq);
			} catch (JpomRuntimeException jpomRuntimeException) {
				throw jpomRuntimeException;
			} catch (Exception e) {
				DefaultSystemLog.getLog().warn("获取仓库分支失败", e);
				return JsonMessage.toJson(500, "无法连接此仓库，" + e.getMessage());
			}
		}
		if (StrUtil.isEmpty(repositoryModelReq.getId())) {
			// insert data
			repositoryService.insert(repositoryModelReq);
		} else {
			// update data
			repositoryModelReq.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
			repositoryService.updateById(repositoryModelReq);
		}

		return JsonMessage.toJson(200, "操作成功");
	}

	/**
	 * edit
	 *
	 * @param id 仓库信息
	 * @return json
	 */
	@PostMapping(value = "/build/repository/rest_hide_field")
	@Feature(method = MethodFeature.EDIT)
	public Object restHideField(@ValidatorItem String id) {
		RepositoryModel repositoryModel = new RepositoryModel();
		repositoryModel.setId(id);
		repositoryModel.setPassword(StrUtil.EMPTY);
		repositoryModel.setRsaPrv(StrUtil.EMPTY);
		repositoryModel.setWorkspaceId(repositoryService.getCheckUserWorkspace(getRequest()));
		repositoryService.updateById(repositoryModel);
		return JsonMessage.toJson(200, "操作成功");
	}

	/**
	 * 检查信息
	 *
	 * @param repositoryModelReq 仓库信息
	 */
	private void checkInfo(RepositoryModel repositoryModelReq) {
		Assert.notNull(repositoryModelReq, "请传人正确的信息");
		Assert.hasText(repositoryModelReq.getName(), "请填写仓库名称");
		Integer repoType = repositoryModelReq.getRepoType();
		Assert.state(repoType != null && (repoType == 1 || repoType == 0), "请选择仓库类型");
		Assert.hasText(repositoryModelReq.getGitUrl(), "请填写仓库地址");
		//
		Integer protocol = repositoryModelReq.getProtocol();
		Assert.state(protocol != null && (protocol == GitProtocolEnum.HTTP.getCode() || protocol == GitProtocolEnum.SSH.getCode()), "请选择拉取代码的协议");
		// 修正字段
		if (protocol == GitProtocolEnum.HTTP.getCode()) {
			//  http
			repositoryModelReq.setRsaPub(StrUtil.EMPTY);
		} else if (protocol == GitProtocolEnum.SSH.getCode()) {
			// ssh
			repositoryModelReq.setPassword(StrUtil.emptyToDefault(repositoryModelReq.getPassword(), StrUtil.EMPTY));
		}
		// 判断仓库是否重复
		Entity entity = Entity.create();
		if (repositoryModelReq.getId() != null) {
			Validator.validateGeneral(repositoryModelReq.getId(), "错误的ID");
			entity.set("id", "<> " + repositoryModelReq.getId());
		}
		String workspaceId = repositoryService.getCheckUserWorkspace(getRequest());
		entity.set("workspaceId", workspaceId);
		entity.set("gitUrl", repositoryModelReq.getGitUrl());
		Assert.state(!repositoryService.exists(entity), "已经存在对应的仓库信息啦");
	}

	/**
	 * check and update ssh key
	 *
	 * @param repositoryModelReq 仓库
	 */
	private boolean checkAndUpdateSshKey(RepositoryModel repositoryModelReq) {
		if (repositoryModelReq.getProtocol() == GitProtocolEnum.SSH.getCode()) {
			// if rsa key is not empty
			if (StrUtil.isNotEmpty(repositoryModelReq.getRsaPrv())) {
				/**
				 * if rsa key is start with "file:"
				 * copy this file
				 */
				if (StrUtil.startWith(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
					String rsaPath = StrUtil.removePrefix(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
					if (!FileUtil.exist(rsaPath)) {
						DefaultSystemLog.getLog().warn("there is no rsa file... {}", rsaPath);
						return false;
					}
				} else {
					//File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModelReq.getId() + Const.ID_RSA);
					//  or else put into file
					//FileUtil.writeUtf8String(repositoryModelReq.getRsaPrv(), rsaFile);
				}
			}
		}
		return true;
	}

	/**
	 * delete
	 *
	 * @param id 仓库ID
	 * @return json
	 */
	@PostMapping(value = "/build/repository/delete")
	@Feature(method = MethodFeature.DEL)
	public Object delRepository(String id) {
		// 判断仓库是否被关联
		Entity entity = Entity.create();
		entity.set("repositoryId", id);
		boolean exists = buildInfoService.exists(entity);
		Assert.state(!exists, "当前仓库被构建关联，不能直接删除");

		repositoryService.delByKey(id, getRequest());
		File rsaFile = BuildUtil.getRepositoryRsaFile(id + Const.ID_RSA);
		FileUtil.del(rsaFile);
		return JsonMessage.getString(200, "删除成功");
	}
}
