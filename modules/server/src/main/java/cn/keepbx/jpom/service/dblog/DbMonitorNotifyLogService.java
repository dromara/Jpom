package cn.keepbx.jpom.service.dblog;

import cn.hutool.db.Entity;
import cn.keepbx.jpom.model.log.MonitorNotifyLog;
import cn.keepbx.jpom.system.db.DbConfig;
import org.springframework.stereotype.Service;

/**
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbMonitorNotifyLogService extends BaseDbLogService<MonitorNotifyLog> {

    public DbMonitorNotifyLogService() {
        super(MonitorNotifyLog.TABLE_NAME, MonitorNotifyLog.class);
        setKey("logId");
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
     * @param logId  通知id
     * @param status 状态
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
