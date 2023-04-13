package org.dromara.jpom.service.user;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.db.Entity;
import cn.keepbx.jpom.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.model.user.TriggerTokenLogBean;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 清理触发器 token
 * @author Hotstrip
 * @since 2023-04-13
 */
@Service
@Slf4j
public class CleanTriggerTokenService extends BaseDbService<TriggerTokenLogBean> implements ISystemTask {

    @Override
    public void executeTask() {
        log.info("clean trigger token start...");
        long start = SystemClock.now();

        // 调用方法处理逻辑
        cleanTriggerToken();

        log.info("clean trigger token end... cost time: {}" , DateUtil.formatBetween(SystemClock.now() - start, BetweenFormatter.Level.MILLISECOND));
    }

    private void cleanTriggerToken() {
        // 查询数据库中的数据
        List<TriggerTokenLogBean> triggerTokenList = list();
        if (triggerTokenList == null || triggerTokenList.isEmpty()) {
            log.warn("trigger token list is empty, no need to clean");
            return;
        }

        List<String> dataIdList = queryDataIdList();

        // 统计删除条数
        int delCount = 0;

        // 遍历数据，判断是否应该删除
        for (TriggerTokenLogBean triggerToken : triggerTokenList) {
            if (!dataIdList.contains(triggerToken.getDataId())) {
                // 删除数据
                delByKey(triggerToken.getId());
                log.info("delete trigger token: {}", triggerToken);
                delCount++;
            }
        }

        log.info("clean trigger token count: {}", delCount);
    }

    private List<String> queryDataIdList() {
        List<String> dataIdList = new ArrayList<>();
        // 根据触发器类型 type 分组查询触发器数据关联的表
        String sql = String.format("select TYPE from %s group by TYPE", this.getTableName());
        log.info("sql: {}", sql);
        List<TriggerTokenLogBean> triggerTokenTypeList = queryList(sql);
        if (triggerTokenTypeList == null || triggerTokenTypeList.isEmpty()) {
            log.warn("trigger token type list is empty, no need to clean");
            return dataIdList;
        }

        // 遍历数据，分别查询对应的数据库表
        triggerTokenTypeList.forEach(item -> {
            // 构造 sql 查询对应表数据的 ID
            String selectSql = String.format("select ID from %s", item.getType());
            List<Entity> entityList = query(selectSql);
            log.info("select sql: {}, size: {}", selectSql, entityList.size());

            // 遍历数据，获取 ID
            entityList.forEach(entity -> {
                dataIdList.add(entity.getStr("ID"));
            });
        });

        return dataIdList;
    }
}
