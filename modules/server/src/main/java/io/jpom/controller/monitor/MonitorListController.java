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
package io.jpom.controller.monitor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.MonitorModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbMonitorNotifyLogService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;

/**
 * 监控列表
 *
 * @author bwcx_jzy
 * @date 2019/6/15
 */
@RestController
@RequestMapping(value = "/monitor")
@Feature(cls = ClassFeature.MONITOR)
public class MonitorListController extends BaseServerController {

	private final MonitorService monitorService;
	private final DbMonitorNotifyLogService dbMonitorNotifyLogService;
	private final UserService userService;
	private final ProjectInfoCacheService projectInfoCacheService;

	public MonitorListController(MonitorService monitorService,
								 DbMonitorNotifyLogService dbMonitorNotifyLogService,
								 UserService userService,
								 ProjectInfoCacheService projectInfoCacheService) {
		this.monitorService = monitorService;
		this.dbMonitorNotifyLogService = dbMonitorNotifyLogService;
		this.userService = userService;
		this.projectInfoCacheService = projectInfoCacheService;
	}

	/**
	 * 展示监控列表
	 *
	 * @return json
	 */
	@RequestMapping(value = "getMonitorList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getMonitorList() {
		PageResultDto<MonitorModel> pageResultDto = monitorService.listPage(getRequest());
		return JsonMessage.getString(200, "", pageResultDto);
	}

	/**
	 * 删除列表
	 *
	 * @param id id
	 * @return json
	 */
	@RequestMapping(value = "deleteMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String deleteMonitor(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败") String id) throws SQLException {
		//
		HttpServletRequest request = getRequest();
		int delByKey = monitorService.delByKey(id, request);
		if (delByKey > 0) {
			// 删除日志
			dbMonitorNotifyLogService.delByWorkspace(request, entity -> entity.set("monitorId", id));
		}
		return JsonMessage.getString(200, "删除成功");
	}


	/**
	 * 增加或修改监控
	 *
	 * @param id         id
	 * @param name       name
	 * @param notifyUser user
	 * @return json
	 */
	@RequestMapping(value = "updateMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String updateMonitor(String id,
								@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空") String name,
								@ValidatorItem(msg = "请配置监控周期") String execCron,
								String notifyUser) {

		UserModel user = getUser();
		Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
		try {
			new CronPattern(execCron);
		} catch (Exception e) {
			throw new IllegalArgumentException("定时构建表达式格式不正确");
		}

		String status = getParameter("status");
		String autoRestart = getParameter("autoRestart");

		JSONArray jsonArray = JSONArray.parseArray(notifyUser);
//		List<String> notifyUsers = jsonArray.toJavaList(String.class);
		List<String> notifyUserList = jsonArray.toJavaList(String.class);
		Assert.notEmpty(jsonArray, "请选择报警联系人");
		for (Object o : jsonArray) {
			String userId = (String) o;
			Assert.state(userService.exists(new UserModel(userId)), "没有对应的用户：" + userId);
		}
		String projects = getParameter("projects");
		JSONArray projectsArray = JSONArray.parseArray(projects);
		List<MonitorModel.NodeProject> nodeProjects = projectsArray.toJavaList(MonitorModel.NodeProject.class);
		Assert.notEmpty(nodeProjects, "请至少选择一个节点");
		for (MonitorModel.NodeProject nodeProject : nodeProjects) {
			Assert.notEmpty(nodeProject.getProjects(), "请至少选择一个项目");
			for (String project : nodeProject.getProjects()) {
				boolean exists = projectInfoCacheService.exists(nodeProject.getNode(), project);
				Assert.state(exists, "没有对应的项目：" + project);
			}
		}
		boolean start = "on".equalsIgnoreCase(status);
		MonitorModel monitorModel = monitorService.getByKey(id);
		if (monitorModel == null) {
			monitorModel = new MonitorModel();
		}
		monitorModel.setAutoRestart("on".equalsIgnoreCase(autoRestart));
		monitorModel.setExecCron(execCron);
		monitorModel.projects(nodeProjects);
		monitorModel.setStatus(start);
		monitorModel.notifyUser(notifyUserList);
		monitorModel.setName(name);

		if (StrUtil.isEmpty(id)) {
			//添加监控
			monitorService.insert(monitorModel);
			return JsonMessage.getString(200, "添加成功");
		}
		monitorService.updateById(monitorModel);
		return JsonMessage.getString(200, "修改成功");
	}

//	/**
//	 * 开启或关闭监控
//	 *
//	 * @param id     id
//	 * @param status 状态
//	 * @param type   类型
//	 * @return json
//	 */
//	@RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//	@Feature(method = MethodFeature.EDIT)
//	public String changeStatus(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id不能为空")) String id,
//							   String status, String type) {
//		MonitorModel monitorModel = monitorService.getByKey(id);
//		Assert.notNull(monitorModel, "不存在监控项啦");
//
//		boolean bStatus = Convert.toBool(status, false);
//		if ("status".equalsIgnoreCase(type)) {
//			monitorModel.setStatus(bStatus);
//		} else if ("restart".equalsIgnoreCase(type)) {
//			monitorModel.setAutoRestart(bStatus);
//		} else {
//			return JsonMessage.getString(405, "type不正确");
//		}
//		monitorService.updateById(monitorModel);
//		return JsonMessage.getString(200, "修改成功");
//	}


}
