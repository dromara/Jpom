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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.Const;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.monitor.NotifyUtil;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.h2db.BaseDbCommonService;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.monitor.MonitorUserOptService;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceService;
import io.jpom.service.user.UserService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbUserOperateLogService extends BaseWorkspaceService<UserOperateLogV1> {

	private static final Map<ClassFeature, Class<? extends BaseDbCommonService<?>>> CLASS_FEATURE_SERVICE = new HashMap<>();

	static {
		CLASS_FEATURE_SERVICE.put(ClassFeature.NODE, NodeService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.SSH, SshService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.OUTGIVING, SshService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.MONITOR, MonitorService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.OPT_MONITOR, MonitorUserOptService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.PROJECT, ProjectInfoCacheService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.BUILD_REPOSITORY, RepositoryService.class);
		CLASS_FEATURE_SERVICE.put(ClassFeature.BUILD, BuildInfoService.class);
	}

	private final MonitorUserOptService monitorUserOptService;
	private final UserService userService;
	private final WorkspaceService workspaceService;

	public DbUserOperateLogService(MonitorUserOptService monitorUserOptService,
								   UserService userService,
								   WorkspaceService workspaceService) {
		this.monitorUserOptService = monitorUserOptService;
		this.userService = userService;
		this.workspaceService = workspaceService;
	}

	private void checkMonitor(UserOperateLogV1 userOperateLogV1) {
		ClassFeature classFeature = EnumUtil.fromString(ClassFeature.class, userOperateLogV1.getClassFeature(), null);
		MethodFeature methodFeature = EnumUtil.fromString(MethodFeature.class, userOperateLogV1.getMethodFeature(), null);
		UserModel optUserItem = userService.getByKey(userOperateLogV1.getUserId());
		if (classFeature == null || methodFeature == null || optUserItem == null) {
			return;
		}
		String otherMsg = "";
		Class<? extends BaseDbCommonService<?>> aClass = CLASS_FEATURE_SERVICE.get(classFeature);
		if (aClass != null) {
			BaseDbCommonService<?> baseDbCommonService = SpringUtil.getBean(aClass);
			Object data = baseDbCommonService.getByKey(userOperateLogV1.getNodeId());
			if (data != null) {
				Object name = BeanUtil.getProperty(data, "name");
				otherMsg = name == null ? StrUtil.EMPTY : StrUtil.format("操作的数据名称：{}\n", name);
			}
		}
		WorkspaceModel workspaceModel = workspaceService.getByKey(userOperateLogV1.getWorkspaceId());

		String optTypeMsg = StrUtil.format(" 【{}】->【{}】", classFeature.getName(), methodFeature.getName());
		List<MonitorUserOptModel> monitorUserOptModels = monitorUserOptService.listByType(userOperateLogV1.getWorkspaceId(),
				classFeature,
				methodFeature);
		if (CollUtil.isEmpty(monitorUserOptModels)) {
			return;
		}
		for (MonitorUserOptModel monitorUserOptModel : monitorUserOptModels) {
			List<String> notifyUser = monitorUserOptModel.notifyUser();
			if (CollUtil.isEmpty(notifyUser)) {
				continue;
			}
			for (String userId : notifyUser) {
				UserModel item = userService.getByKey(userId);
				if (item == null) {
					continue;
				}
				//
				String context = StrUtil.format("操作用户：{}\n操作状态：{}\n操作类型：{}\n所属工作空间：{}\n操作节点：{}\n 操作数据id: {}\n操作IP: {}\n{}",
						optUserItem.getName(),
						userOperateLogV1.getOptStatusMsg(),
						optTypeMsg,
						workspaceModel.getName(),
						userOperateLogV1.getNodeId(), userOperateLogV1.getDataId(), userOperateLogV1.getIp(), otherMsg);
				// 邮箱
				String email = item.getEmail();
				if (StrUtil.isNotEmpty(email)) {
					MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
					ThreadUtil.execute(() -> {
						try {
							NotifyUtil.send(notify1, "用户操作报警", context);
						} catch (Exception e) {
							DefaultSystemLog.getLog().error("发送报警信息错误", e);
						}
					});

				}
				// dingding
				String dingDing = item.getDingDing();
				if (StrUtil.isNotEmpty(dingDing)) {
					MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
					ThreadUtil.execute(() -> {
						try {
							NotifyUtil.send(notify1, "用户操作报警", context);
						} catch (Exception e) {
							DefaultSystemLog.getLog().error("发送报警信息错误", e);
						}
					});
				}
				// 企业微信
				String workWx = item.getWorkWx();
				if (StrUtil.isNotEmpty(workWx)) {
					MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
					ThreadUtil.execute(() -> {
						try {
							NotifyUtil.send(notify1, "用户操作报警", context);
						} catch (Exception e) {
							DefaultSystemLog.getLog().error("发送报警信息错误", e);
						}
					});
				}
			}
		}
	}

	@Override
	public void insert(UserOperateLogV1 userOperateLogV1) {
		super.insert(userOperateLogV1);
		ThreadUtil.execute(() -> {
			try {
				this.checkMonitor(userOperateLogV1);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("执行操作监控错误", e);
			}
		});
	}

	@Override
	public String getCheckUserWorkspace(HttpServletRequest request) {
		// 忽略检查
		return ServletUtil.getHeader(request, Const.WORKSPACEID_REQ_HEADER, CharsetUtil.CHARSET_UTF_8);
	}

	@Override
	protected void checkUserWorkspace(String workspaceId, UserModel userModel) {
		// 忽略检查
	}

	@Override
	protected String[] clearTimeColumns() {
		return new String[]{"optTime", "createTimeMillis"};
	}
}
