/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * 工作空间
 *
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "WORKSPACE",
    nameKey = "i18n.workspace_label.98d6")
@Data
@NoArgsConstructor
public class WorkspaceModel extends BaseUserModifyDbModel {

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;
    /**
     * 分组
     */
    private String group;
    /**
     * 集群信息Id
     *
     * @see org.dromara.jpom.func.system.model.ClusterInfoModel
     */
    private String clusterInfoId;

    public WorkspaceModel(String id) {
        this.setId(id);
    }
}
