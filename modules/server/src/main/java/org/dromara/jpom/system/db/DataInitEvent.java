/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system.db;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.spring.SpringUtil;
import cn.keepbx.jpom.event.ICacheTask;
import cn.keepbx.jpom.model.BaseIdModel;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.h2db.BaseNodeService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据库初始化完成后
 *
 * @author bwcx_jzy
 * @since 2023/2/18
 */
@Configuration
@Slf4j
public class DataInitEvent implements ILoadEvent, ICacheTask {

    private final WorkspaceService workspaceService;
    private final Map<String, List<String>> errorWorkspaceTable = new HashMap<>();

    public DataInitEvent(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    public Map<String, List<String>> getErrorWorkspaceTable() {
        return errorWorkspaceTable;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 分组
        Map<String, BaseDbService> groupServiceMap = SpringUtil.getApplicationContext().getBeansOfType(BaseDbService.class);
        for (BaseDbService<?> value : groupServiceMap.values()) {
            if (value.isCanGroup()) {
                value.repairGroupFiled();
            }
        }
        // 状态恢复的数据
        Map<String, IStatusRecover> statusRecoverMap = SpringUtil.getApplicationContext().getBeansOfType(IStatusRecover.class);
        statusRecoverMap.forEach((name, iCron) -> {
            int count = iCron.statusRecover();
            if (count > 0) {
                log.info(I18nMessageUtil.get("i18n.recover_abnormal_data.9adf"), name, count);
            }
        });
        //  同步项目
        Map<String, BaseNodeService> beansOfType = SpringUtil.getApplicationContext().getBeansOfType(BaseNodeService.class);
        for (BaseNodeService<?> value : beansOfType.values()) {
            value.syncAllNode();
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE + 2;
    }


    private void checkErrorWorkspace() {
        errorWorkspaceTable.clear();
        // 判断是否存在关联数据
        Set<String> workspaceIds = this.allowWorkspaceIds();
        Set<Class<?>> classes = BaseWorkspaceModel.allTableClass();
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            int workspaceBind = tableName.workspaceBind();
            if (workspaceBind == 3) {
                // 父级不存在自动删除
                Class<?> parents = tableName.parents();
                Assert.state(parents != Void.class, I18nMessageUtil.get("i18n.table_info_configuration_error_message.6452") + aClass);
                //
                TableName tableName1 = parents.getAnnotation(TableName.class);
                Assert.notNull(tableName1, I18nMessageUtil.get("i18n.parent_table_info_config_error.2f52") + aClass);
            }
            String workspaceIdField = DialectUtil.wrapField("workspaceId");
            String sql =StrUtil.format("select {},count(1) as allCount from {} group by {}",
                workspaceIdField,tableName.value(),workspaceIdField);
            List<Entity> query = workspaceService.query(sql);
            for (Entity entity : query) {
                String workspaceId = (String) entity.get("workspaceId");
                long allCount = (long) entity.get("allCount");
                if (workspaceIds.contains(workspaceId)) {
                    continue;
                }
                String format = StrUtil.format(I18nMessageUtil.get("i18n.table_error_workspace_data.9021"), I18nMessageUtil.get(tableName.nameKey()), tableName.value(), allCount, workspaceId);
                log.error(format);
                List<String> stringList = errorWorkspaceTable.computeIfAbsent(tableName.value(), s -> new ArrayList<>());
                stringList.add(format);
            }
        }
    }

    public Set<String> allowWorkspaceIds() {
        // 判断是否存在关联数据
        List<WorkspaceModel> list = workspaceService.list();
        Set<String> workspaceIds = Optional.ofNullable(list)
            .map(workspaceModels -> workspaceModels.stream()
                .map(BaseIdModel::getId)
                .collect(Collectors.toSet()))
            .orElse(new HashSet<>());
        // 添加默认的全局工作空间 id
        workspaceIds.add(ServerConst.WORKSPACE_GLOBAL);
        return workspaceIds;
    }

    public void clearErrorWorkspace(String tableName) {
        Assert.state(errorWorkspaceTable.containsKey(tableName), I18nMessageUtil.get("i18n.no_error_data_in_table.3092"));
        Set<String> workspaceIds = this.allowWorkspaceIds();
        String sql = "select workspaceId,count(1) as allCount from " + tableName + " group by workspaceId";
        List<Entity> query = workspaceService.query(sql);
        for (Entity entity : query) {
            String workspaceId = (String) entity.get("workspaceId");
            if (workspaceIds.contains(workspaceId)) {
                continue;
            }
            String deleteSql = "delete from " + tableName + " where workspaceId=?";
            int execute = workspaceService.execute(deleteSql, workspaceId);
            log.info(I18nMessageUtil.get("i18n.delete_table_data.c813"), tableName, execute, workspaceId);
        }
        this.checkErrorWorkspace();
    }

    @Override
    public void refreshCache() {
        try {
            checkErrorWorkspace();
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.query_workspace_error.6a0d"), e);
        }
    }
}
