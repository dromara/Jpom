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

import cn.hutool.db.Entity;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.pipeline.model.PipelineRunModel;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * 流水线运行记录服务
 * <p>
 * 同时负责**启动恢复**：服务重启后，内存里的执行体（进程、取消意图）都不存在了，
 * 之前停留在「运行中/取消中」的记录必须如实落成「异常关闭」，否则界面会永远显示在跑
 * （实测：重启后 run #1–#3 全卡在「运行中」）。恢复只改状态，不删任何数据（§0.28）。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class PipelineRunService extends BaseWorkspaceService<PipelineRunModel> implements IStatusRecover {

    @Override
    public int statusRecover() {
        Entity update = Entity.create(getTableName());
        update.set("status", BuildStatus.AbnormalShutdown.getCode());
        update.set("statusMsg", "服务重启，执行体已丢失（进程可能残留，请检查）");
        Entity where = Entity.create(getTableName());
        where.set("status", Arrays.asList(
            BuildStatus.Ing.getCode(),
            BuildStatus.PubIng.getCode(),
            BuildStatus.WaitExec.getCode(),
            BuildStatus.CancelRequested.getCode()));
        return this.update(update, where);
    }
}
