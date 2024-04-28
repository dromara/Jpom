/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.BaseAgentController;
import org.dromara.jpom.model.data.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2022/1/17
 */
public abstract class BaseWorkspaceOptService<T extends BaseWorkspaceModel> extends BaseOperService<T> {

    public BaseWorkspaceOptService(String fileName) {
        super(fileName);
    }

    @Override
    public void addItem(T t) {
        t.setCreateTime(DateUtil.now());
        String userName = BaseAgentController.getNowUserName();
        if (!StrUtil.DASHED.equals(userName)) {
            t.setCreateUser(userName);
            t.setModifyUser(userName);
        }
        super.addItem(t);
    }

    /**
     * 修改信息
     *
     * @param data 信息
     */
    @Override
    public void updateItem(T data) {
        data.setModifyTime(DateUtil.now());
        String userName = BaseAgentController.getNowUserName();
        if (!StrUtil.DASHED.equals(userName)) {
            data.setModifyUser(userName);
        }
        super.updateItem(data);
    }

    @Override
    public void updateById(T updateData, String id) {
        updateData.setModifyTime(DateUtil.now());
        String userName = BaseAgentController.getNowUserName();
        if (!StrUtil.DASHED.equals(userName)) {
            updateData.setModifyUser(userName);
        }
        super.updateById(updateData, id);
    }
}
