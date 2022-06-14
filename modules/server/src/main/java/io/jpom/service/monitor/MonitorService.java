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
package io.jpom.service.monitor;

import io.jpom.cron.CronUtils;
import io.jpom.cron.ICron;
import io.jpom.model.data.MonitorModel;
import io.jpom.monitor.MonitorItem;
import io.jpom.service.h2db.BaseWorkspaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 监控管理Service
 *
 * @author Arno
 */
@Service
@Slf4j
public class MonitorService extends BaseWorkspaceService<MonitorModel> implements ICron<MonitorModel> {

	@Override
	public void insert(MonitorModel monitorModel) {
		super.insert(monitorModel);
		this.checkCron(monitorModel);
	}

	@Override
	public int delByKey(String keyValue, HttpServletRequest request) {
		int i = super.delByKey(keyValue, request);
		if (i > 0) {
			String taskId = "monitor:" + keyValue;
			CronUtils.remove(taskId);
		}
		return i;
	}

	@Override
	public int updateById(MonitorModel info, HttpServletRequest request) {
		int update = super.updateById(info, request);
		if (update > 0) {
			this.checkCron(info);
		}
		return update;
	}

	@Override
	public List<MonitorModel> queryStartingList() {
		// 关闭监听
		MonitorModel monitorModel = new MonitorModel();
		monitorModel.setStatus(true);
		return super.listByBean(monitorModel);
	}

	/**
	 * 检查定时任务 状态
	 *
	 * @param monitorModel 监控信息
	 */
	@Override
	public boolean checkCron(MonitorModel monitorModel) {
		String id = monitorModel.getId();
		String taskId = "monitor:" + id;
		String autoExecCron = monitorModel.getExecCron();
		if (!monitorModel.status()) {
			CronUtils.remove(taskId);
			return false;
		}
		log.debug("start monitor cron {} {} {}", id, monitorModel.getName(), autoExecCron);
		CronUtils.upsert(taskId, autoExecCron, new MonitorItem(id));
		return true;
	}

	/**
	 * 设置报警状态
	 *
	 * @param id    监控id
	 * @param alarm 状态
	 */
	public void setAlarm(String id, boolean alarm) {
		MonitorModel monitorModel = new MonitorModel();
		monitorModel.setId(id);
		monitorModel.setAlarm(alarm);
		super.update(monitorModel);
	}

	/**
	 * 判断是否存在对应节点数据
	 *
	 * @param nodeId 节点id
	 * @return true 存在
	 */
	public boolean checkNode(String nodeId) {
		List<MonitorModel> list = list();
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (MonitorModel monitorModel : list) {
			List<MonitorModel.NodeProject> projects = monitorModel.projects();
			if (projects != null) {
				for (MonitorModel.NodeProject project : projects) {
					if (nodeId.equals(project.getNode())) {
						return true;
					}
				}
			}
		}
		return false;
	}


	/*public boolean checkProject(String nodeId, String projectId) {
		List<MonitorModel> list = list();
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (MonitorModel monitorModel : list) {
			List<MonitorModel.NodeProject> projects = monitorModel.projects();
			if (projects != null) {
				for (MonitorModel.NodeProject project : projects) {
					if (project.getNode().equals(nodeId)) {
						List<String> projects1 = project.getProjects();
						if (projects1 != null && projects1.contains(projectId)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}*/
}
