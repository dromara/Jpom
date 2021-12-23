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
package io.jpom.service.dblog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.build.BuildInfoManage;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.service.ICron;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.util.CronUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * 构建 service 新版本，数据从数据库里面加载
 *
 * @author Hotstrip
 * @date 2021-08-10
 **/
@Service
public class BuildInfoService extends BaseWorkspaceService<BuildInfoModel> implements ICron {

	private final RepositoryService repositoryService;

	public BuildInfoService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Override
	public void insert(BuildInfoModel buildInfoModel) {
		super.insert(buildInfoModel);
		this.checkCron(buildInfoModel);
	}

	@Override
	public int update(BuildInfoModel buildInfoModel) {
		int update = super.update(buildInfoModel);
		if (update > 0) {
			this.checkCron(buildInfoModel);
		}
		return update;
	}

	/**
	 * 开启定时构建任务
	 */
	@Override
	public int startCron() {
		String sql = "select * from " + super.getTableName() + " where autoBuildCron is not null and autoBuildCron <> ''";
		List<BuildInfoModel> buildInfoModels = super.queryList(sql);
		if (buildInfoModels == null) {
			return 0;
		}
		for (BuildInfoModel buildInfoModel : buildInfoModels) {
			this.checkCron(buildInfoModel);
		}
		// 恢复异常数据
		String updateSql = "update " + super.getTableName() + " set status=? where status=? or status=?";
		int execute = super.execute(updateSql, BuildStatus.No.getCode(), BuildStatus.Ing.getCode(), BuildStatus.PubIng.getCode());
		if (execute > 0) {
			DefaultSystemLog.getLog().info("build Recover bad data {}", execute);
		}
		return CollUtil.size(buildInfoModels);
	}

	/**
	 * 检查定时任务 状态
	 *
	 * @param buildInfoModel 构建信息
	 */
	private void checkCron(BuildInfoModel buildInfoModel) {
		String id = buildInfoModel.getId();
		String taskId = "build:" + id;
		String autoBuildCron = buildInfoModel.getAutoBuildCron();
		if (StrUtil.isEmpty(autoBuildCron)) {
			CronUtils.remove(taskId);
			return;
		}
		DefaultSystemLog.getLog().debug("start build cron {} {} {}", id, buildInfoModel.getName(), autoBuildCron);
		CronUtils.upsert(taskId, autoBuildCron, new CronTask(id));
	}

	private class CronTask implements Task {

		private final String buildId;

		public CronTask(String buildId) {
			this.buildId = buildId;
		}

		@Override
		public void execute() {
			try {
				BuildInfoService.this.start(this.buildId, null, null, 2);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("触发自动构建异常", e);
			}
		}
	}

	/**
	 * start build
	 *
	 * @param buildInfoId      构建Id
	 * @param userModel        用户信息
	 * @param delay            延迟的时间
	 * @param triggerBuildType 触发构建类型
	 * @return json
	 */
	public JsonMessage<Integer> start(String buildInfoId, UserModel userModel, Integer delay, int triggerBuildType) {
		synchronized (buildInfoId.intern()) {
			BuildInfoModel buildInfoModel = super.getByKey(buildInfoId);
			String e = this.checkStatus(buildInfoModel.getStatus());
			Assert.isNull(e, () -> e);
			// set buildId field
			int buildId = ObjectUtil.defaultIfNull(buildInfoModel.getBuildId(), 0);
			{
				BuildInfoModel buildInfoModel1 = new BuildInfoModel();
				buildInfoModel1.setBuildId(buildId + 1);
				buildInfoModel1.setId(buildInfoId);
				buildInfoModel.setBuildId(buildInfoModel1.getBuildId());
				super.update(buildInfoModel1);
			}
			// load repository
			RepositoryModel repositoryModel = repositoryService.getByKey(buildInfoModel.getRepositoryId(), false);
			Assert.notNull(repositoryModel, "仓库信息不存在");
			BuildInfoManage.create(buildInfoModel, repositoryModel, userModel, delay, triggerBuildType);
			String msg = (delay == null || delay <= 0) ? "开始构建中" : "延迟" + delay + "秒后开始构建";
			return new JsonMessage<>(200, msg, buildInfoModel.getBuildId());
		}
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
