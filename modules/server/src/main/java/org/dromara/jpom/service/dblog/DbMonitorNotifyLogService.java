/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.dblog;

import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.model.log.MonitorNotifyLog;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * 监控消息
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@Service
public class DbMonitorNotifyLogService extends BaseWorkspaceService<MonitorNotifyLog> {


    @Override
    public int insert(MonitorNotifyLog monitorNotifyLog) {
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            return super.insert(monitorNotifyLog);
            //
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"createTime", "createTimeMillis"};
    }

    /**
     * 修改执行结果
     *
     * @param logId    通知id
     * @param status   状态
     * @param errorMsg 错误消息
     */
    public void updateStatus(String logId, boolean status, String errorMsg) {
        MonitorNotifyLog monitorNotifyLog = new MonitorNotifyLog();
        monitorNotifyLog.setId(logId);
        monitorNotifyLog.setNotifyStatus(status);
        monitorNotifyLog.setNotifyError(errorMsg);
        super.updateById(monitorNotifyLog);
//		Entity entity = new Entity();
//		entity.set("notifyStatus", status);
//		if (errorMsg != null) {
//			entity.set("notifyError", errorMsg);
//		}
//		//
//		Entity where = new Entity();
//		where.set("logId", logId);
//		super.update(entity, where);
    }
}
