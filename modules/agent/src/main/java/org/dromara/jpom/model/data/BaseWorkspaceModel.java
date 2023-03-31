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
package org.dromara.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件端 工作空间相关的数据
 *
 * @author bwcx_jzy
 * @since 2021/12/12
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseWorkspaceModel extends BaseModel {

    /**
     * 数据关联的工作空间
     */
    private String workspaceId;
    /**
     * 数据跟随的节点 ID
     */
    private String nodeId;
    /**
     * 最后修改人
     */
    private String modifyUser;

    private String modifyTime;

    /**
     * 创建人
     */
    private String createUser;

    public String getModifyUser() {
        if (StrUtil.isEmpty(modifyUser)) {
            return StrUtil.DASHED;
        }
        return modifyUser;
    }

    public boolean global() {
        return StrUtil.equals(this.workspaceId, Const.WORKSPACE_GLOBAL);
    }

    public String getWorkspaceId() {
        return StrUtil.emptyToDefault(workspaceId, Const.WORKSPACE_DEFAULT_ID);
    }
}
