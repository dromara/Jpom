package cn.keepbx.jpom.service.build;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.model.data.BuildHistoryLog;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

/**
 * 构建service
 *
 * @author bwcx_jzy
 * @date 2019/7/18
 **/
@Service
public class BuildHistoryService {

    public BuildHistoryLog getLog(String logId) throws SQLException {
        Entity where = new Entity(BuildHistoryLog.TABLE_NAME);
        where.set("id", logId);
        Db db = Db.use();
        db.setWrapper((Character) null);
        Entity entity = db.get(where);
        if (entity == null) {
            return null;
        }
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreError(true);
        copyOptions.setIgnoreCase(true);
        return BeanUtil.mapToBean(entity, BuildHistoryLog.class, copyOptions);
    }

    public void del(String logId) throws SQLException {
        Entity where = new Entity(BuildHistoryLog.TABLE_NAME);
        where.set("id", logId);
        Db db = Db.use();
        db.setWrapper((Character) null);
        db.del(where);
    }

    public void delByBuildId(String buildDataId) throws SQLException {
        Entity where = new Entity(BuildHistoryLog.TABLE_NAME);
        where.set("buildDataId", buildDataId);
        Db db = Db.use();
        db.setWrapper((Character) null);
        db.del(where);
    }
}
