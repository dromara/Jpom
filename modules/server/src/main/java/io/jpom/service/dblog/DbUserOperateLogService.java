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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.monitor.NotifyUtil;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.monitor.MonitorUserOptService;
import io.jpom.service.user.UserService;
import io.jpom.system.db.DbConfig;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbUserOperateLogService extends BaseWorkspaceService<UserOperateLogV1> {

	private final MonitorUserOptService monitorUserOptService;
	private final UserService userService;
	private final BuildInfoService buildService;

	public DbUserOperateLogService(MonitorUserOptService monitorUserOptService,
								   UserService userService,
								   BuildInfoService buildService) {

		this.monitorUserOptService = monitorUserOptService;
		this.userService = userService;
		this.buildService = buildService;
	}

	@Override
	public void insert(UserOperateLogV1 userOperateLogV1) {
		super.insert(userOperateLogV1);
		DbConfig.autoClear(getTableName(), "optTime");
		DbConfig.autoClear(getTableName(), "createTimeMillis");
		ThreadUtil.execute(() -> {
			UserOperateLogV1.OptType optType = BaseEnum.getEnum(UserOperateLogV1.OptType.class, userOperateLogV1.getOptType());
			if (optType == null) {
				return;
			}
			UserModel optUserItem = userService.getByKey(userOperateLogV1.getUserId());
			if (optUserItem == null) {
				return;
			}
			String otherMsg = "";
			if (optType == UserOperateLogV1.OptType.StartBuild || optType == UserOperateLogV1.OptType.EditBuild) {
				BuildInfoModel item = buildService.getByKey(userOperateLogV1.getDataId());
				if (item != null) {
					otherMsg = StrUtil.format("操作的构建名称：{}\n", item.getName());
				}
			}
			List<MonitorUserOptModel> monitorUserOptModels = monitorUserOptService.listByType(optType, userOperateLogV1.getUserId());
			if (CollUtil.isEmpty(monitorUserOptModels)) {
				return;
			}
			for (MonitorUserOptModel monitorUserOptModel : monitorUserOptModels) {
				List<String> notifyUser = monitorUserOptModel.getNotifyUser();
				if (CollUtil.isEmpty(notifyUser)) {
					continue;
				}
				for (String userId : notifyUser) {
					UserModel item = userService.getByKey(userId);
					if (item == null) {
						continue;
					}
					//
					String context = StrUtil.format("操作用户：{}\n操作状态：{}\n操作类型：{}\n操作节点：{}\n 操作数据id: {}\n操作IP: {}\n{}",
							optUserItem.getName(), userOperateLogV1.getOptStatusMsg(), userOperateLogV1.getOptTypeMsg(),
							userOperateLogV1.getNodeId(), userOperateLogV1.getDataId(), userOperateLogV1.getIp(), otherMsg);
					// 邮箱
					String email = item.getEmail();
					if (StrUtil.isNotEmpty(email)) {
						MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
						ThreadUtil.execute(() -> NotifyUtil.send(notify1, "用户操作报警", context));

					}
					// dingding
					String dingDing = item.getDingDing();
					if (StrUtil.isNotEmpty(dingDing)) {
						MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
						ThreadUtil.execute(() -> NotifyUtil.send(notify1, "用户操作报警", context));
					}
					// 企业微信
					String workWx = item.getWorkWx();
					if (StrUtil.isNotEmpty(workWx)) {
						MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
						ThreadUtil.execute(() -> NotifyUtil.send(notify1, "用户操作报警", context));
					}
				}
			}
		});
	}
}
