/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseUserModifyDbModel;

/**
 * @author bwcx_jzy1
 * @since 2024/6/1
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SCRIPT_LIBRARY", nameKey = "脚本库信息")
@Data
public class ScriptLibraryModel extends BaseUserModifyDbModel {
    /**
     * 脚本唯一的标记
     */
    private String tag;
    /**
     * 脚本内容
     */
    private String script;
    /**
     * 描述
     */
    private String description;
    /**
     * 版本
     */
    private String version;
    /**
     * 关联的资产机器节点
     */
    private String machineIds;

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
