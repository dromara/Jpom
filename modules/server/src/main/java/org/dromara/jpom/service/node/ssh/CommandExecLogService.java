/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.node.ssh;

import cn.hutool.core.io.FileUtil;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2021/12/22
 */
@Service
public class CommandExecLogService extends BaseWorkspaceService<CommandExecLogModel> {

    @Override
    protected void fillSelectResult(CommandExecLogModel data) {
        if (data == null) {
            return;
        }
        data.setHasLog(FileUtil.exist(data.logFile()));
    }

    @Override
    protected void executeClearImpl(int h2DbLogStorageCount) {
        super.autoLoopClear("createTimeMillis", h2DbLogStorageCount, null, commandExecLogModel -> {
            File file = commandExecLogModel.logFile();
            CommandUtil.systemFastDel(file);
            File parentFile = file.getParentFile();
            boolean empty = FileUtil.isEmpty(parentFile);
            if (empty) {
                CommandUtil.systemFastDel(parentFile);
            }
            return true;
        });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }
}
