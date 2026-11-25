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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.enums.BuildStatus;
import org.dromara.jpom.pipeline.engine.PipelineRunState;
import org.dromara.jpom.pipeline.model.PipelineRunModel;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

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
@Slf4j
@Service
public class PipelineRunService extends BaseWorkspaceService<PipelineRunModel> implements IStatusRecover {

    /**
     * 取该流水线当前正在执行的运行记录（用于「取消中」中间态）
     *
     * @param pipelineId 流水线 ID
     * @return 运行中的记录；没有返回 null
     */
    public PipelineRunModel getRunningRun(String pipelineId) {
        PipelineRunModel where = new PipelineRunModel();
        where.setPipelineId(pipelineId);
        where.setStatus(BuildStatus.Ing.getCode());
        List<PipelineRunModel> list = this.listByBean(where);
        if (CollUtil.isEmpty(list)) {
            where.setStatus(BuildStatus.PubIng.getCode());
            list = this.listByBean(where);
        }
        return CollUtil.getFirst(list);
    }

    /**
     * 受控状态转换
     * <p>
     * 所有「运行状态变化」都从这里走：{@link PipelineRunState} 判定合法才写库，非法转换直接拒绝并记
     * 日志（不静默覆盖）。终态因此不会被后续写入改写。
     * </p>
     *
     * @param run     运行记录（读当前状态）
     * @param target  目标状态
     * @param message 状态说明
     * @return true 表示写入成功；false 表示非法转换被拒绝
     */
    public boolean transit(PipelineRunModel run, BuildStatus target, String message) {
        if (run == null || target == null) {
            return false;
        }
        Integer from = run.getStatus();
        if (!PipelineRunState.canTransit(from, target.getCode())) {
            log.warn("pipeline run status transit rejected, runId:{} from:{} to:{}", run.getId(), from, target.getCode());
            return false;
        }
        this.writeStatus(run, target, message, null, null);
        return true;
    }

    /**
     * 收尾写入（终态 + 阶段结果 + 事件记录）
     * <p>
     * 收尾必须落库，否则界面会永远停在「运行中」；因此非法转换在这里是「记录并强制写入」，
     * 而不是拒绝（拒绝会把运行记录卡在非终态）。非法转换的信息写进日志与状态说明，便于追查。
     * </p>
     *
     * @param run       运行记录（已带阶段结果/耗时/日志大小）
     * @param target    终态
     * @param message   状态说明
     * @param stagesJson 阶段结果 JSON（可为 null 表示不改）
     * @param eventsJson 事件记录 JSON（可为 null 表示不改）
     */
    public void finish(PipelineRunModel run, BuildStatus target, String message, String stagesJson, String eventsJson) {
        if (run == null || target == null) {
            return;
        }
        Integer from = run.getStatus();
        String finalMessage = message;
        if (!PipelineRunState.canTransit(from, target.getCode())) {
            log.error("pipeline run finish transit rejected, force write. runId:{} from:{} to:{}", run.getId(), from, target.getCode());
            finalMessage = StrUtil.format(I18nMessageUtil.get("i18n.pipeline_state_force_finish.8d02"),
                from, target.getCode()) + " " + StrUtil.nullToEmpty(message);
        }
        this.writeStatus(run, target, finalMessage, stagesJson, eventsJson);
    }

    private void writeStatus(PipelineRunModel run, BuildStatus target, String message, String stagesJson, String eventsJson) {
        PipelineRunModel update = new PipelineRunModel();
        update.setId(run.getId());
        update.setStatus(target.getCode());
        update.setStatusMsg(message);
        update.setStagesResult(stagesJson);
        update.setEventsJson(eventsJson);
        update.setEndTime(run.getEndTime());
        update.setLogFileSize(run.getLogFileSize());
        this.updateById(update);
        run.setStatus(target.getCode());
        run.setStatusMsg(message);
        if (stagesJson != null) {
            run.setStagesResult(stagesJson);
        }
        if (eventsJson != null) {
            run.setEventsJson(eventsJson);
        }
    }

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
