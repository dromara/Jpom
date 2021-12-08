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
package io.jpom.service.dblog;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.build.BuildInfoManage;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Objects;

/**
 * 构建 service 新版本，数据从数据库里面加载
 *
 * @author Hotstrip
 * @date 2021-08-10
 **/
@Service
public class BuildInfoService extends BaseWorkspaceService<BuildInfoModel> {

	private final RepositoryService repositoryService;

	public BuildInfoService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

//	/**
//	 * load date group by group name
//	 *
//	 * @return list
//	 */
//	public List<String> listGroup() {
//		String sql = "select `GROUP` from " + getTableName() + " where 1=1 group by `GROUP`";
//		List<Entity> list = super.query(sql);
//		// 筛选字段
//		return list.stream()
//				.filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(Const.GROUP_STR))))
//				.flatMap(entity -> Stream.of(String.valueOf(entity.get(Const.GROUP_STR))))
//				.distinct()
//				.collect(Collectors.toList());
//	}

	/**
	 * start build
	 *
	 * @param buildInfoModel 构建信息
	 * @param userModel      用户信息
	 * @param delay          延迟的时间
	 * @return json
	 */
	public String start(final BuildInfoModel buildInfoModel, final UserModel userModel, Integer delay) {
		// load repository
		RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId(), false);
		Assert.notNull(repositoryModel, "仓库信息不存在");
		BuildInfoManage.create(buildInfoModel, repositoryModel, userModel, delay);
		String msg = (delay == null || delay <= 0) ? "开始构建中" : "延迟" + delay + "秒后开始构建";
		return JsonMessage.getString(200, msg, buildInfoModel.getBuildId());
	}

	/**
	 * check status
	 *
	 * @param status 状态吗
	 * @return 错误消息
	 */
	public String checkStatus(Integer status) {
		if (status == null) {
			return null;
		}
		BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
		Objects.requireNonNull(nowStatus);
		if (BuildStatus.Ing == nowStatus ||
				BuildStatus.PubIng == nowStatus) {
			return "当前还在：" + nowStatus.getDesc();
		}
		return null;
	}

	/**
	 * 判断是否存在 节点关联
	 *
	 * @param nodeId 节点ID
	 * @return true 关联
	 */
	public boolean checkNode(String nodeId) {
		Entity entity = new Entity();
		entity.set("releaseMethod", BuildReleaseMethod.Project.getCode());
		entity.set("releaseMethodDataId", StrUtil.format(" like '{}:%'", nodeId));
		return super.exists(entity);
	}

	/**
	 * 判断是否存在 发布关联
	 *
	 * @param dataId 数据ID
	 * @return true 关联
	 */
	public boolean checkReleaseMethod(String dataId, BuildReleaseMethod releaseMethod) {
		BuildInfoModel buildInfoModel = new BuildInfoModel();
		buildInfoModel.setReleaseMethodDataId(dataId);
		buildInfoModel.setReleaseMethod(releaseMethod.getCode());
		return super.exists(buildInfoModel);
	}
}
