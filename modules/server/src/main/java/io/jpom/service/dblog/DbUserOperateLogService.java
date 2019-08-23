package io.jpom.service.dblog;

import io.jpom.model.log.UserOperateLogV1;
import io.jpom.system.db.DbConfig;
import org.springframework.stereotype.Service;

/**
 * 操作日志
 *
 * @author bwcx_jzy
 * @date 2019/7/20
 */
@Service
public class DbUserOperateLogService extends BaseDbLogService<UserOperateLogV1> {

    public DbUserOperateLogService() {
        super(UserOperateLogV1.TABLE_NAME, UserOperateLogV1.class);
        setKey("reqId");
    }

    @Override
    public void insert(UserOperateLogV1 userOperateLogV1) {
        super.insert(userOperateLogV1);
        DbConfig.autoClear(getTableName(), "optTime");
    }
}
