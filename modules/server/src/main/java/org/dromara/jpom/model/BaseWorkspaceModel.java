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
