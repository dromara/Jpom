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
package org.dromara.jpom.service.outgiving;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import org.dromara.jpom.model.log.OutGivingLog;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分发日志
 *
 * @author bwcx_jzy
 * @since 2019/7/20
 */
@Service
public class DbOutGivingLogService extends BaseWorkspaceService<OutGivingLog> {


    /**
     * 查询最新的分发日志
     *
     * @param outId       分发id
     * @param nodeProject 项目信息
     * @return log
     */
    public OutGivingLog getByProject(String outId, OutGivingNodeProject nodeProject) {
        OutGivingLog outGivingLog = new OutGivingLog();
        outGivingLog.setOutGivingId(outId);
        outGivingLog.setNodeId(nodeProject.getNodeId());
        outGivingLog.setProjectId(nodeProject.getProjectId());
        List<OutGivingLog> givingLogs = this.queryList(outGivingLog, 1, new Order("createTimeMillis", Direction.DESC));
        return CollUtil.getFirst(givingLogs);
    }
}
