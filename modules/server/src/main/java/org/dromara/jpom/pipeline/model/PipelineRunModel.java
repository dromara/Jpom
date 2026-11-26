/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * 流水线运行记录（一次运行一行）
 * <p>
 * 阶段结果以 JSON 存在 {@link #stagesResult}：阶段数在配置里可变，列化只会变成宽表；
 * 需要按阶段聚合统计时再升级成 {@code PIPELINE_STAGE_LOG}（只增表，不动本表）。
 *
 * @author jpom
 * @since 2.11.16
 */
@TableName(value = "PIPELINE_RUN", nameKey = "i18n.pipeline_run.f3a2")
@Data
@EqualsAndHashCode(callSuper = true)
public class PipelineRunModel extends BaseWorkspaceModel {

    /**
     * 流水线 ID
     */
    private String pipelineId;
    /**
     * 运行编号（该流水线内自增）
     */
    private Integer runNumber;
    /**
     * 状态（复用构建状态码：Ing / PubIng / CancelRequested / Cancel / AbnormalShutdown / Error / Success）
     */
    private Integer status;
    /**
     * 状态说明
     */
    private String statusMsg;
    /**
     * 阶段结果（JSON 数组，见 StageResult）
     */
    private String stagesResult;
    /**
     * 源快照（本次运行使用的仓库与 ref，便于事后复盘）
     */
    private String sourcesSnapshot;
    /**
     * 事件钩子执行记录（JSON 数组，见 {@code PipelineEventRecord}）
     * <p>
     * 追加列（2.11.16）：只有真正触发了钩子的事件才有记录，旧记录为空。
     * </p>
     */
    private String eventsJson;
    /**
     * 触发方式：0 手动 / 1 定时 / 2 系统 / 3 重跑
     */
    private Integer triggerType;
    /**
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 日志文件大小
     */
    private Long logFileSize;
    /**
     * 是否有日志
     */
    private Boolean hasLog;
}
