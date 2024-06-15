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

import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.TableName;
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
            String sql = "update " + tableName.value() + " set workspaceId=? where (workspaceId is null or workspaceId='' or workspaceId='null')";
            int execute = this.execute(sql, Const.WORKSPACE_DEFAULT_ID);
            if (execute > 0) {
                log.info(I18nMessageUtil.get("i18n.fix_null_workspace_data.4d0b"), tableName.value(), execute);
            }
            total += execute;
        }
        return total;
    }
}
