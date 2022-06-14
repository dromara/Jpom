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
package io.jpom.service.outgiving;

import io.jpom.model.outgiving.LogReadModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2022/5/15
 */
@Service
public class LogReadServer extends BaseWorkspaceService<LogReadModel> {


    public void checkNodeProject(String nodeId, String projectId, HttpServletRequest request) {
        // 检查节点分发
        List<LogReadModel> outGivingModels = super.listByWorkspace(request);
        if (outGivingModels != null) {
            boolean match = outGivingModels.stream().anyMatch(outGivingModel -> outGivingModel.checkContains(nodeId, projectId));
            Assert.state(!match, "当前项目存在日志阅读，不能直接删除");
        }
    }

    public boolean checkNode(String nodeId, HttpServletRequest request) {
        List<LogReadModel> list = super.listByWorkspace(request);
        if (list == null || list.isEmpty()) {
            return false;
        }
        for (LogReadModel outGivingModel : list) {
            List<LogReadModel.Item> items = outGivingModel.nodeProjectList();
            if (items != null) {
                for (LogReadModel.Item item : items) {
                    if (item.getNodeId().equals(nodeId)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
