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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.script.CommandParam;

/**
 * 脚本模版实体
 *
 * @author bwcx_jzy
 * @since 2021/12/12
 **/
@TableName(value = "SCRIPT_INFO",
    nameKey = "i18n.node_script_template_title.4e74")
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeScriptCacheModel extends BaseNodeModel {
    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 模版名称
     */
    private String name;
    /**
     * 最后执行人员
     */
    private String lastRunUser;
    /**
     * 定时执行
     */
    private String autoExecCron;
    /**
     * 默认参数
     */
    private String defArgs;
    /**
     * 描述
     */
    private String description;
    /**
     * 脚本类型
     */
    private String scriptType;
    /**
     * 触发器 token
     */
    private String triggerToken;

    @Override
    public String dataId() {
        return getScriptId();
    }

    @Override
    public void dataId(String id) {
        setScriptId(id);
    }

    public void setDefArgs(String defArgs) {
        this.defArgs = CommandParam.convertToParam(defArgs);
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
