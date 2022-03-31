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
package io.jpom.service.system;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.collection.CollUtil;
import io.jpom.model.data.WorkspaceEnvVarModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.util.StringUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2021/12/10
 */
@Service
public class WorkspaceEnvVarService extends BaseWorkspaceService<WorkspaceEnvVarModel> {

    public Map<String, String> getEnv(String workspaceId) {
        WorkspaceEnvVarModel workspaceEnvVarModel = new WorkspaceEnvVarModel();
        workspaceEnvVarModel.setWorkspaceId(workspaceId);
        List<WorkspaceEnvVarModel> list = super.listByBean(workspaceEnvVarModel);
        Map<String, String> map = CollStreamUtil.toMap(list, WorkspaceEnvVarModel::getName, WorkspaceEnvVarModel::getValue);
        // java.lang.UnsupportedOperationException
        HashMap<String, String> hashMap = new HashMap<>(CollUtil.size(list) + 10);
        hashMap.putAll(map);
        return hashMap;
    }

    public void formatCommand(String workspaceId, String[] commands) {
        WorkspaceEnvVarModel workspaceEnvVarModel = new WorkspaceEnvVarModel();
        workspaceEnvVarModel.setWorkspaceId(workspaceId);
        Map<String, String> evn = this.getEnv(workspaceId);
        for (int i = 0; i < commands.length; i++) {
            commands[i] = StringUtil.formatStrByMap(commands[i], evn);
        }
    }
}
