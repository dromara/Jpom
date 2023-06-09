/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.service.system;

import cn.hutool.db.Entity;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@Service
@Slf4j
public class WorkspaceService extends BaseDbService<WorkspaceModel> implements IStatusRecover {

    /**
     * load date group by group name
     *
     * @return list
     */
    public List<String> listGroup() {
        String sql = "select `GROUP` from " + getTableName() + "  group by `GROUP`";
        List<Entity> list = super.query(sql);
        // 筛选字段
        return list.stream()
            .flatMap(entity -> {
                Object obj = entity.get(ServerConst.GROUP_STR);
                if (obj == null) {
                    return null;
                }
                return Stream.of(String.valueOf(obj));
            }).filter(Objects::nonNull)
            .distinct().collect(Collectors.toList());
    }

    /**
     * 恢复字段
     */
    private void repairGroupFiled() {
        String sql = "update " + getTableName() + " set `GROUP`=? where `GROUP` is null or `GROUP`=''";
        super.execute(sql, Const.DEFAULT_GROUP_NAME);
    }

    @Override
    public int statusRecover() {
        WorkspaceModel workspaceModel = super.getByKey(Const.WORKSPACE_DEFAULT_ID);
        if (workspaceModel == null) {
            WorkspaceModel defaultWorkspace = new WorkspaceModel();
            defaultWorkspace.setId(Const.WORKSPACE_DEFAULT_ID);
            defaultWorkspace.setName(Const.DEFAULT_GROUP_NAME);
            defaultWorkspace.setDescription("系统默认的工作空间,不能删除");
            super.insert(defaultWorkspace);

            log.info("init created default workspace");
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
                log.info("convertNullWorkspaceId {} {}", tableName.value(), execute);
            }
            total += execute;
        }
        //
        this.repairGroupFiled();
        return total;
    }
}
