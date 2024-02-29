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

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 带最后修改人字段 数据表实体
 *
 * @author bwcx_jzy
 * @since 2021/8/24
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseUserModifyDbModel extends BaseDbModel {
    /**
     * 修改人
     */
    private String modifyUser;
    /**
     * 创建人
     */
    private String createUser;

    @Override
    public String toString() {
        return super.toString();
    }

    public void setCreateUser(String createUser) {
        if (this.hasCreateUser()) {
            this.createUser = createUser;
        }
    }

    /**
     * 是否开启创建人字段
     *
     * @return true 开启
     */
    protected boolean hasCreateUser() {
        return false;
    }
}
