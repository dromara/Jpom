/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.model.BaseModel;

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

    private String createTime;

    private String modifyTime;

    /**
     * 创建人
     */
    private String createUser;

    public boolean global() {
        return StrUtil.equals(this.workspaceId, Const.WORKSPACE_GLOBAL);
    }
}
