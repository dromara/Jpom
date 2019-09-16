package io.jpom.service.dblog;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.PageResult;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.node.NodeService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CronUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DbSystemMonitorLogService extends BaseDbLogService<SystemMonitorLog> {
    @Resource
    private NodeService nodeService;

    public DbSystemMonitorLogService() {
        super(SystemMonitorLog.TABLE_NAME, SystemMonitorLog.class);
        setKey("id");
    }

    /**
     * 请求监控数据
     */
    public void init() {
        List<NodeModel> list = nodeService.list();
        CronUtil.schedule("0 0/30 * * * ?", (Runnable) () -> {
            List<Entity> arrayList = new ArrayList<>();
            for (NodeModel nodeModel : list) {
                try {
                    JsonMessage message = NodeForward.request(nodeModel, null, NodeUrl.GetTop);
                    JSONObject jsonObject = (JSONObject) message.getData();
                    if (jsonObject == null) {
                        continue;
                    }
                    JSONArray series = jsonObject.getJSONArray("series");
                    if (CollUtil.isNotEmpty(series)) {
                        for (int i = 0; i < series.size(); i++) {
                            try {
                                JSONObject object = series.getJSONObject(i);
                                double disk = object.getDoubleValue("disk");
                                if (disk <= 0) {
                                    continue;
                                }
                                long monitorTime = object.getLongValue("monitorTime");
                                SystemMonitorLog log = new SystemMonitorLog();
                                log.setId(IdUtil.fastSimpleUUID());
                                log.setOccupyMemory(object.getDoubleValue("memory"));
                                log.setOccupyDisk(disk);
                                log.setOccupyCpu(object.getDoubleValue("cpu"));
                                log.setMonitorTime(monitorTime);
                                log.setNodeId(nodeModel.getId());
                                Entity entity = new Entity(SystemMonitorLog.TABLE_NAME);
                                entity.parseBean(log);
                                arrayList.add(entity);
                            } catch (Exception e) {
                                DefaultSystemLog.ERROR().error("数据库插入监控数据异常：", e);
                            }
                        }
                    }
                } catch (Exception e) {
                    DefaultSystemLog.ERROR().error("请求节点数据异常：", e);
                }
            }
            insert(arrayList);
        });
        CronUtils.start();
    }

    public PageResult<SystemMonitorLog> getMonitorData(long startTime, long endTime) {
        Entity entity = new Entity(SystemMonitorLog.TABLE_NAME);
        entity.set(" MONITORTIME", ">= " + startTime);
        entity.set("MONITORTIME", "<= " + endTime);
        return listPage(entity, null);
    }

    private void insert(List<Entity> list) {
        Db db = Db.use();
        db.setWrapper((Character) null);
        try {
            db.insert(list);
        } catch (SQLException e) {
            throw new JpomRuntimeException("数据库异常", e);
        }
    }
}
