/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.system;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.model.BaseModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lidaofu
 * @since 2022/3/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WorkspaceEnvVarModel extends BaseModel {

    private Map<String, WorkspaceEnvVarItemModel> varData;

    /**
     * 更新变量
     *
     * @param name                 变量名称
     * @param workspaceEnvVarModel 变量信息
     */
    public void put(String name, WorkspaceEnvVarItemModel workspaceEnvVarModel) {
        if (varData == null) {
            varData = new HashMap<>(2);
        }
        varData.put(name, workspaceEnvVarModel);
    }

    /**
     * 删除 变量
     *
     * @param name 名称
     */
    public void remove(String name) {
        if (varData == null) {
            return;
        }
        varData.remove(name);
    }

    /**
     * @author lidaofu
     * @since 2022/3/8
     */
    @Data
    public static class WorkspaceEnvVarItemModel {

        private String name;

        private String value;

        private String description;

        /**
         * 隐私变量{1，隐私变量，0 非隐私变量（明文回显）}
         */
        private Integer privacy;
    }
}
