/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
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
