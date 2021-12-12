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
import io.jpom.model.data.NodeScriptModel;
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
public class ScriptServer extends BaseOperService<NodeScriptModel> {

    public ScriptServer() {
        super(AgentConfigBean.SCRIPT);
    }

    @Override
    public List<NodeScriptModel> list() {
        List<NodeScriptModel> nodeScriptModels = super.list();
        if (nodeScriptModels == null) {
            return null;
        }
        // 读取文件内容
        nodeScriptModels.forEach(NodeScriptModel::readFileTime);
        return nodeScriptModels;
    }

    @Override
    public NodeScriptModel getItem(String id) {
        NodeScriptModel nodeScriptModel = super.getItem(id);
        if (nodeScriptModel != null) {
            nodeScriptModel.readFileContext();
        }
        return nodeScriptModel;
    }

    @Override
    public void addItem(NodeScriptModel nodeScriptModel) {
        super.addItem(nodeScriptModel);
        nodeScriptModel.saveFile();
    }

    @Override
    public void updateItem(NodeScriptModel nodeScriptModel) {
        super.updateItem(nodeScriptModel);
        nodeScriptModel.saveFile();
    }

    @Override
    public void deleteItem(String id) {
        NodeScriptModel nodeScriptModel = getItem(id);
        if (nodeScriptModel != null) {
            FileUtil.del(nodeScriptModel.getFile(true).getParentFile());
        }
        super.deleteItem(id);
    }
}
