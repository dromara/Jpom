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
import cn.hutool.core.util.IdUtil;
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
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.GitUtil;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;

/**
 * @author Hotstrip
 * Repository controller
 */
@RestController
@Feature(cls = ClassFeature.BUILD_REPOSITORY)
public class RepositoryController extends BaseServerController {

	@Resource
	private RepositoryService repositoryService;

	@Resource
	private BuildInfoService buildInfoService;

	/**
	 * load repository list
	 *
	 * @return json
	 */
	@PostMapping(value = "/build/repository/list")
	@Feature(method = MethodFeature.LOG)
	public Object loadRepositoryList() {
		PageResultDto<RepositoryModel> pageResult = repositoryService.listPage(getRequest());
		return JsonMessage.getString(200, "获取成功", pageResult);
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

		if (null == repositoryModelReq.getId()) {
			// insert data
			repositoryModelReq.setId(IdUtil.fastSimpleUUID());
			repositoryService.insert(repositoryModelReq);
		} else {
			// update data
			if (StrUtil.isEmpty(repositoryModelReq.getRsaPrv())) {
				repositoryModelReq.setRsaPrv(null);
			}
			if (StrUtil.isEmpty(repositoryModelReq.getPassword())) {
				repositoryModelReq.setPassword(null);
			}
			repositoryService.updateById(repositoryModelReq);
		}
		// 检查 rsa 私钥
		if (!checkAndUpdateSshKey(repositoryModelReq)) {
			return JsonMessage.toJson(500, "rsa 私钥文件不存在或者有误");
		}
		if (repositoryModelReq.getRepoType() == RepositoryModel.RepoType.Git.getCode()) {
			RepositoryModel repositoryModel = repositoryService.getByKey(repositoryModelReq.getId());
			// 验证 git 仓库信息
			try {
				Tuple tuple = GitUtil.getBranchAndTagList(repositoryModel);
			} catch (JpomRuntimeException jpomRuntimeException) {
				throw jpomRuntimeException;
			} catch (Exception e) {
				DefaultSystemLog.getLog().warn("获取仓库分支失败", e);
				return JsonMessage.toJson(500, "无法连接此仓库，" + e.getMessage());
			}
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
			entity.set("strike", 0);
		}
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
				File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModelReq.getId() + Const.ID_RSA);
				/**
				 * if rsa key is start with "file:"
				 * copy this file
				 */
				if (StrUtil.startWith(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX)) {
					String rsaPath = StrUtil.removePrefix(repositoryModelReq.getRsaPrv(), URLUtil.FILE_URL_PREFIX);
					if (!FileUtil.file(rsaPath).exists()) {
						DefaultSystemLog.getLog().warn("there is no rsa file... {}", rsaPath);
						return false;
					}
				} else {
					//  or else put into file
					FileUtil.writeUtf8String(repositoryModelReq.getRsaPrv(), rsaFile);
				}
			}
		}
		return true;
	}

	/**
	 * delete
	 *
	 * @param id        仓库ID
	 * @param isRealDel 是否真删
	 * @return json
	 */
	@PostMapping(value = "/build/repository/delete")
	@Feature(method = MethodFeature.DEL)
	public Object delRepository(String id, Boolean isRealDel) {
		// 判断仓库是否被关联
		Entity entity = Entity.create();
		entity.set("repositoryId", id);
		boolean exists = buildInfoService.exists(entity);
		Assert.state(!exists, "当前仓库被构建关联，不能直接删除");
		UserModel user = getUser();
		if (user.isSystemUser() && isRealDel) {
			repositoryService.delByKey(id);
		} else {
			Entity updateEntity = Entity.create();
			updateEntity.set("strike", 1);
			Entity whereEntity = Entity.create();
			whereEntity.set("id", id);
			repositoryService.update(updateEntity, whereEntity);
		}
		return JsonMessage.getString(200, "删除成功");
	}


	/**
	 * 恢复仓库
	 *
	 * @param id 仓库ID
	 * @return json
	 */
	@PostMapping(value = "/build/repository/recovery")
	@Feature(method = MethodFeature.EXECUTE)
	public Object recoveryRepository(String id) {
		RepositoryModel repositoryModelReq = repositoryService.getByKey(id);
		// 判断仓库是否重复
		Entity entity = Entity.create();
		if (repositoryModelReq.getId() != null) {
			Validator.validateGeneral(repositoryModelReq.getId(), "错误的ID");
			entity.set("id", "<> " + repositoryModelReq.getId());
			entity.set("strike", 0);
		}
		entity.set("gitUrl", repositoryModelReq.getGitUrl());
		Assert.state(!repositoryService.exists(entity), "已经存在对应的仓库信息啦!");
		// 判断仓库是否被关联
		Entity whereEntity = Entity.create();
		whereEntity.set("id", id);
		Entity updateEntity = Entity.create();
		updateEntity.set("strike", 0);
		repositoryService.update(updateEntity, whereEntity);
		return JsonMessage.getString(200, "恢复成功");
	}
}
