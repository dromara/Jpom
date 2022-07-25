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
package io.jpom.model.node;

import io.jpom.model.BaseNodeModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 脚本模版实体
 *
 * @author bwcx_jzy
 * @since 2021/12/12
 **/
@TableName(value = "SCRIPT_INFO", name = "节点脚本模版")
@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptCacheModel extends BaseNodeModel {
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
}
