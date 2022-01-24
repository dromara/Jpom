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
package io.jpom.service.script;

import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.cron.CronUtils;
import io.jpom.cron.ICron;
import io.jpom.model.data.UserModel;
import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.socket.ScriptProcessBuilder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Service
public class ScriptServer extends BaseWorkspaceService<ScriptModel> implements ICron<ScriptModel> {

	@Override
	public List<ScriptModel> queryStartingList() {
		String sql = "select * from " + super.getTableName() + " where autoExecCron is not null and autoExecCron <> ''";
		return super.queryList(sql);
	}

	@Override
	public void insert(ScriptModel scriptModel) {
		super.insert(scriptModel);
		this.checkCron(scriptModel);
	}

	@Override
	public int updateById(ScriptModel info, HttpServletRequest request) {
		int update = super.updateById(info, request);
		if (update > 0) {
			this.checkCron(info);
		}
		return update;
	}

	@Override
	public int delByKey(String keyValue, HttpServletRequest request) {
		int delByKey = super.delByKey(keyValue, request);
		if (delByKey > 0) {
			String taskId = "server_script:" + keyValue;
			CronUtils.remove(taskId);
		}
		return delByKey;
	}

	/**
	 * 检查定时任务 状态
	 *
	 * @param scriptModel 构建信息
	 */
	@Override
	public boolean checkCron(ScriptModel scriptModel) {
		String id = scriptModel.getId();
		String taskId = "server_script:" + id;
		String autoExecCron = scriptModel.getAutoExecCron();
		if (StrUtil.isEmpty(autoExecCron)) {
			CronUtils.remove(taskId);
			return false;
		}
		DefaultSystemLog.getLog().debug("start script cron {} {} {}", id, scriptModel.getName(), autoExecCron);
		CronUtils.upsert(taskId, autoExecCron, new CronTask(id));
		return true;
	}


	private static class CronTask implements Task {

		private final String id;

		public CronTask(String id) {
			this.id = id;
		}

		@Override
		public void execute() {
			try {
				BaseServerController.resetInfo(UserModel.EMPTY);
				ScriptServer nodeScriptServer = SpringUtil.getBean(ScriptServer.class);
				ScriptModel scriptServerItem = nodeScriptServer.getByKey(id);
				if (scriptServerItem == null) {
					return;
				}
				// 创建记录
				ScriptExecuteLogServer execLogServer = SpringUtil.getBean(ScriptExecuteLogServer.class);
				ScriptExecuteLogModel nodeScriptExecLogModel = execLogServer.create(scriptServerItem, 1);
				// 执行
				ScriptProcessBuilder.create(scriptServerItem, nodeScriptExecLogModel.getId(), scriptServerItem.getDefArgs());
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("触发自动执行命令模版异常", e);
			} finally {
				BaseServerController.removeEmpty();
			}
		}
	}
}
