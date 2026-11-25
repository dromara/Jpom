/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.system;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@Service
@Slf4j
public class WorkspaceService extends BaseDbService<WorkspaceModel> implements IStatusRecover {

    @Override
    public int statusRecover() {
        WorkspaceModel workspaceModel = super.getByKey(Const.WORKSPACE_DEFAULT_ID);
        if (workspaceModel == null) {
            WorkspaceModel defaultWorkspace = new WorkspaceModel();
            defaultWorkspace.setId(Const.WORKSPACE_DEFAULT_ID);
            defaultWorkspace.setName(Const.DEFAULT_GROUP_NAME.get());
            defaultWorkspace.setDescription(I18nMessageUtil.get("i18n.default_workspace_cannot_delete.18b4"));
            super.insert(defaultWorkspace);
            log.info(I18nMessageUtil.get("i18n.initialize_workspace.bc97"), Const.DEFAULT_GROUP_NAME.get());
        }

        Set<Class<?>> classes = BaseWorkspaceModel.allClass();
        int total = 0;
        for (Class<?> aClass : classes) {
            TableName tableName = aClass.getAnnotation(TableName.class);
            if (tableName == null) {
                continue;
            }

            String workspaceId = DialectUtil.wrapField("workspaceId");
            String value = super.parseRealTableName(tableName);
            String sql = StrUtil.format("update " + value + " set {}=? where ({} is null or {}='' or {}='null')",
                workspaceId,workspaceId,workspaceId,workspaceId);
            int execute;
            try {
                execute = this.execute(sql, Const.WORKSPACE_DEFAULT_ID);
            } catch (Exception e) {
                if (isTableMissing(e)) {
                    // 新模块的表在应用启动早期还没建（建表发生在后置事件里），这里跳过即可：
                    // 表不存在就没有历史数据需要修，不能因此让整个服务起不来（实测新增 PIPELINE_INFO 时启动 crash）
                    log.debug("skip workspace repair for not-created table: {}", value);
                    continue;
                }
                throw e;
            }
            if (execute > 0) {
                log.info(I18nMessageUtil.get("i18n.fix_null_workspace_data.4d0b"), value, execute);
            }
            total += execute;
        }
        return total;
    }

    /**
     * 判断异常是否为「表不存在」
     *
     * @param e 异常
     * @return true 表不存在
     */
    private static boolean isTableMissing(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("not found") || message.contains("does not exist")
                || message.contains("不存在") || message.contains("42S02"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}