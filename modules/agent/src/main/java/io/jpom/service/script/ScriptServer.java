/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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

import cn.hutool.core.io.FileUtil;
import io.jpom.common.BaseOperService;
import io.jpom.model.data.ScriptModel;
import io.jpom.system.AgentConfigBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 脚本模板管理
 *
 * @author jiangzeyin
 * @date 2019/4/24
 */
@Service
public class ScriptServer extends BaseOperService<ScriptModel> {

    public ScriptServer() {
        super(AgentConfigBean.SCRIPT);
    }

    @Override
    public List<ScriptModel> list() {
        List<ScriptModel> scriptModels = super.list();
        if (scriptModels == null) {
            return null;
        }
        // 读取文件内容
        scriptModels.forEach(ScriptModel::readFileTime);
        return scriptModels;
    }

    @Override
    public ScriptModel getItem(String id) {
        ScriptModel scriptModel = super.getItem(id);
        if (scriptModel != null) {
            scriptModel.readFileContext();
        }
        return scriptModel;
    }

    @Override
    public void addItem(ScriptModel scriptModel) {
        super.addItem(scriptModel);
        scriptModel.saveFile();
    }

    @Override
    public void updateItem(ScriptModel scriptModel) {
        super.updateItem(scriptModel);
        scriptModel.saveFile();
    }

    @Override
    public void deleteItem(String id) {
        ScriptModel scriptModel = getItem(id);
        if (scriptModel != null) {
            FileUtil.del(scriptModel.getFile(true).getParentFile());
        }
        super.deleteItem(id);
    }
}
