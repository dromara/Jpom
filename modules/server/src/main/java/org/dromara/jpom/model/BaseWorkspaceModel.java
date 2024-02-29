/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.data.WorkspaceModel;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作空间 数据
 *
 * @author bwcx_jzy
 * @since 2021/12/04
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseWorkspaceModel extends BaseUserModifyDbModel {

    /**
     * 工作空间ID
     *
     * @see WorkspaceModel
     * @see Const#WORKSPACE_ID_REQ_HEADER
     */
    private String workspaceId;

    @Override
    public String toString() {
        return super.toString();
    }

    public boolean global() {
        return StrUtil.equals(this.workspaceId, ServerConst.WORKSPACE_GLOBAL);
    }

    /**
     * 所有实现过的 class
     *
     * @return set
     */
    public static Set<Class<?>> allClass() {
        return ClassUtil.scanPackageBySuper("org.dromara.jpom", BaseWorkspaceModel.class);
    }

    /**
     * 所有实现过的 class
     *
     * @return set
     */
    public static Set<Class<?>> allTableClass() {
        Set<Class<?>> classes1 = allClass();
        return classes1.stream()
            .filter(aClass -> aClass.isAnnotationPresent(TableName.class))
            .collect(Collectors.toSet());
    }

}
