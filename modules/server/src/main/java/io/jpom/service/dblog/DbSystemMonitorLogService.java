package io.jpom.service.dblog;

import cn.hutool.db.Entity;
import cn.hutool.db.PageResult;
import io.jpom.model.log.SystemMonitorLog;
import org.springframework.stereotype.Service;

/**
 * @author Arno
 * @date 2019/9/13
 */
@Service
public class DbSystemMonitorLogService extends BaseDbLogService<SystemMonitorLog> {

    public DbSystemMonitorLogService() {
        super(SystemMonitorLog.TABLE_NAME, SystemMonitorLog.class);
        setKey("id");
    }

    public PageResult<SystemMonitorLog> getMonitorData(long startTime, long endTime) {
        Entity entity = new Entity(SystemMonitorLog.TABLE_NAME);
        entity.set(" MONITORTIME", ">= " + startTime);
        entity.set("MONITORTIME", "<= " + endTime);
        return listPage(entity, null);
    }
}
