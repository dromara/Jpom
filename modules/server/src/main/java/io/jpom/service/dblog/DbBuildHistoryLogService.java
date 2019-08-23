package io.jpom.service.dblog;

import cn.hutool.db.Entity;
import io.jpom.model.data.BuildModel;
import io.jpom.model.log.BuildHistoryLog;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * 构建历史db
 *
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

    /**
     * 更新状态
     *
     * @param logId  记录id
     * @param status 状态
     */
    public void updateLog(String logId, BuildModel.Status status) {
        if (logId == null) {
            return;
        }

        Entity entity = new Entity();
        entity.set("status", status.getCode());
        if (status != BuildModel.Status.PubIng) {
            // 结束
            entity.set("endTime", System.currentTimeMillis());
        }
        //
        Entity where = new Entity();
        where.set("id", logId);
        update(entity, where);
    }
}
