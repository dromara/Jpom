/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.node;

import cn.hutool.core.annotation.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseNodeModel;

/**
 * @author bwcx_jzy
 * @since 2021/12/12
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SCRIPT_EXECUTE_LOG",
    nameKey = "i18n.node_script_template_execution_record.704a", parents = NodeScriptCacheModel.class)
@Data
public class NodeScriptExecuteLogCacheModel extends BaseNodeModel {

    /**
     *
     */
    @PropIgnore
    private String name;
    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 脚本名称
     */
    private String scriptName;
    /**
     * 触发类型 {0，手动，1 自动触发}
     */
    private Integer triggerExecType;

    @Override
    public String fullId() {
        throw new IllegalStateException("NO implements");
    }

    public void setName(String name) {
        this.name = name;
        this.scriptName = name;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
        this.name = scriptName;
    }

    @Override
    public String dataId() {
        return getScriptId();
    }

    @Override
    public void dataId(String id) {
        setScriptId(id);
    }
}
