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

import cn.hutool.db.Entity;
import io.jpom.model.log.MonitorNotifyLog;
import io.jpom.service.h2db.BaseDbCommonService;
import io.jpom.system.db.DbConfig;
import org.springframework.stereotype.Service;

/**
 * 监控消息
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbMonitorNotifyLogService extends BaseDbCommonService<MonitorNotifyLog> {

	public DbMonitorNotifyLogService() {
		super(MonitorNotifyLog.TABLE_NAME, "logId", MonitorNotifyLog.class);
	}

	@Override
	public void insert(MonitorNotifyLog monitorNotifyLog) {
		super.insert(monitorNotifyLog);
		//
		DbConfig.autoClear(getTableName(), "createTime");
	}


	/**
	 * 修改执行结果
	 *
	 * @param logId    通知id
	 * @param status   状态
	 * @param errorMsg 错误消息
	 */
	public void updateStatus(String logId, boolean status, String errorMsg) {
		Entity entity = new Entity();
		entity.set("notifyStatus", status);
		if (errorMsg != null) {
			entity.set("notifyError", errorMsg);
		}
		//
		Entity where = new Entity();
		where.set("logId", logId);
		super.update(entity, where);
	}
}
