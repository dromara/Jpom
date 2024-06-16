/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;
import org.dromara.jpom.script.CommandParam;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@TableName(value = "SERVER_SCRIPT_INFO",
    nameKey = "i18n.script_template.1f77")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptModel extends BaseWorkspaceModel {
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

    private String context;
    /**
     * 节点ID
     */
    private String nodeIds;
    /**
     * 触发器 token
     */
    private String triggerToken;

    public void setDefArgs(String defArgs) {
        this.defArgs = CommandParam.convertToParam(defArgs);
    }

    public File scriptPath() {
        return scriptPath(getId());
    }

    public static File scriptPath(String id) {
        if (StrUtil.isEmpty(id)) {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.id_is_empty.3bbf"));
        }
        File path = JpomApplication.getInstance().getScriptPath();
        return FileUtil.file(path, id);
    }

    public File logFile(String executeId) {
        //File path = this.scriptPath();
        //return FileUtil.file(path, "log", executeId + ".log");
        return logFile(getId(), executeId);
    }

    public static File logFile(String id, String executeId) {
        File path = scriptPath(id);
        return FileUtil.file(path, "log", executeId + ".log");
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
