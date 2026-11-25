/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.service;

import org.dromara.jpom.pipeline.model.PipelineInfoModel;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

/**
 * 流水线配置服务
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineInfoService extends BaseWorkspaceService<PipelineInfoModel> {

    /**
     * 分配下一个运行编号（供产物目录与运行记录使用）
     *
     * @param id 流水线 ID
     * @return 新编号
     */
    public int nextRunNumber(String id) {
        PipelineInfoModel item = this.getByKey(id);
        int next = (item == null || item.getRunNumber() == null ? 0 : item.getRunNumber()) + 1;
        PipelineInfoModel update = new PipelineInfoModel();
        update.setId(id);
        update.setRunNumber(next);
        this.updateById(update);
        return next;
    }

    /**
     * 更新运行状态
     *
     * @param id     流水线 ID
     * @param status 状态码
     * @param msg    说明
     * @param runId  运行记录 ID
     */
    public void updateRunStatus(String id, Integer status, String msg, String runId) {
        PipelineInfoModel update = new PipelineInfoModel();
        update.setId(id);
        update.setStatus(status);
        update.setStatusMsg(msg);
        update.setLastRunId(runId);
        this.updateById(update);
    }
}
