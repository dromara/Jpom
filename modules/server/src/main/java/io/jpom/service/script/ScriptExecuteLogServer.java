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
package io.jpom.service.script;

import io.jpom.model.script.ScriptExecuteLogModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.util.CommandUtil;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@Service
public class ScriptExecuteLogServer extends BaseWorkspaceService<ScriptExecuteLogModel> {

    /**
     * 创建执行记录
     *
     * @param scriptModel 脚本
     * @param type        执行类型
     * @return 对象
     */
    public ScriptExecuteLogModel create(ScriptModel scriptModel, int type) {
        ScriptExecuteLogModel scriptExecuteLogModel = new ScriptExecuteLogModel();
        scriptExecuteLogModel.setScriptId(scriptModel.getId());
        scriptExecuteLogModel.setScriptName(scriptModel.getName());
        scriptExecuteLogModel.setTriggerExecType(type);
        scriptExecuteLogModel.setWorkspaceId(scriptModel.getWorkspaceId());
        super.insert(scriptExecuteLogModel);
        return scriptExecuteLogModel;
    }


    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount, null, scriptExecuteLogModel -> {
            File logFile = ScriptModel.logFile(scriptExecuteLogModel.getScriptId(), scriptExecuteLogModel.getId());
            boolean fastDel = CommandUtil.systemFastDel(logFile);
            return !fastDel;
        });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }
}
