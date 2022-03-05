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

import cn.hutool.core.annotation.PropIgnore;
import io.jpom.model.BaseNodeModel;
import io.jpom.service.h2db.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2021/12/12
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SCRIPT_EXECUTE_LOG", name = "节点脚本模版执行记录")
@Data
public class ScriptExecuteLogCacheModel extends BaseNodeModel {

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
