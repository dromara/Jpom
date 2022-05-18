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

import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

/**
 * @author Arno
 * @since 2019/9/13
 */
@Service
public class DbSystemMonitorLogService extends BaseDbService<SystemMonitorLog> {

	@Override
	protected String[] clearTimeColumns() {
		return new String[]{"monitorTime"};
	}


	//
//	public PageResultDto<SystemMonitorLog> getMonitorData(long startTime, long endTime) {
//		Entity entity = new Entity(SystemMonitorLog.TABLE_NAME);
//		entity.set(" MONITORTIME", ">= " + startTime);
//		entity.set("MONITORTIME", "<= " + endTime);
//		return listPage(entity, null);
//	}
}
