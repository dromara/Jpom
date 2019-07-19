package cn.keepbx.jpom.service.dblog;

import cn.hutool.db.Entity;
import cn.keepbx.jpom.model.log.BuildHistoryLog;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbBuildHistoryLogService extends BaseDbLogService<BuildHistoryLog> {

    public DbBuildHistoryLogService() {
        super(BuildHistoryLog.TABLE_NAME, BuildHistoryLog.class);
        setKey("id");
    }

    public void delByBuildId(String buildDataId) throws SQLException {
        Entity where = new Entity(getTableName());
        where.set("buildDataId", buildDataId);
        del(where);
    }
}
