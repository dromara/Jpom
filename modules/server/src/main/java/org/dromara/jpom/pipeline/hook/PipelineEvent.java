/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.hook;

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流水线事件（状态切换点）
 * <p>
 * 这是「事件状态切换机制」的契约：运行与阶段的每一次状态切换都对应一个固定事件码，
 * 事件钩子（脚本 / webhook）只认事件码，新增事件＝这里加一项（前端选项由
 * {@code /pipeline/hook-events} 返回，不在前端复制一份）。
 * </p>
 * <p>
 * 语义对齐在线构建的「按事件选脚本」：`run_finished` 相当于在线构建的 `done`（总会触发），
 * `run_failed` / `stage_failed` 相当于 `error`，`run_cancelled` 相当于 `stop`。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public enum PipelineEvent {

    /**
     * 运行开始（源同步之前）
     */
    RUN_START("run_start", "i18n.pipeline_event_run_start.8b01"),
    /**
     * 运行成功
     */
    RUN_SUCCESS("run_success", "i18n.pipeline_event_run_success.8b02"),
    /**
     * 运行失败
     */
    RUN_FAILED("run_failed", "i18n.pipeline_event_run_failed.8b03"),
    /**
     * 运行被取消
     */
    RUN_CANCELLED("run_cancelled", "i18n.pipeline_event_run_cancelled.8b04"),
    /**
     * 运行结束（终态之后总会触发，适合做汇总通知）
     */
    RUN_FINISHED("run_finished", "i18n.pipeline_event_run_finished.8b05"),
    /**
     * 阶段开始
     */
    STAGE_START("stage_start", "i18n.pipeline_event_stage_start.8b06"),
    /**
     * 阶段成功
     */
    STAGE_SUCCESS("stage_success", "i18n.pipeline_event_stage_success.8b07"),
    /**
     * 阶段失败
     */
    STAGE_FAILED("stage_failed", "i18n.pipeline_event_stage_failed.8b08"),
    /**
     * 阶段被跳过（前置失败 + 失败即停，或阶段被禁用）
     */
    STAGE_SKIPPED("stage_skipped", "i18n.pipeline_event_stage_skipped.8b09"),
    /**
     * 阶段被取消
     */
    STAGE_CANCELLED("stage_cancelled", "i18n.pipeline_event_stage_cancelled.8b10"),
    ;

    /**
     * 匹配所有事件（与在线构建脚本 description 写 `all` 同义）
     */
    public static final String ALL = "all";

    private final String code;
    private final String descKey;

    PipelineEvent(String code, String descKey) {
        this.code = code;
        this.descKey = descKey;
    }

    public String getCode() {
        return code;
    }

    public String getDescKey() {
        return descKey;
    }

    /**
     * @return 本地化描述
     */
    public String desc() {
        return I18nMessageUtil.get(descKey);
    }

    /**
     * 按事件码解析
     *
     * @param code 事件码
     * @return 事件；未知返回 null
     */
    public static PipelineEvent of(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        for (PipelineEvent event : values()) {
            if (StrUtil.equals(event.code, code)) {
                return event;
            }
        }
        return null;
    }

    /**
     * @param code 事件码
     * @return 是否为合法事件码
     */
    public static boolean exists(String code) {
        return of(code) != null;
    }

    /**
     * @return 全部事件码（只读）
     */
    public static List<String> codes() {
        List<String> list = new ArrayList<>();
        for (PipelineEvent event : values()) {
            list.add(event.code);
        }
        return list;
    }

    /**
     * @return 事件码 → 本地化描述（顺序稳定，用于接口返回与前端下拉）
     */
    public static Map<String, String> descriptions() {
        Map<String, String> map = new LinkedHashMap<>();
        for (PipelineEvent event : values()) {
            map.put(event.code, event.desc());
        }
        return map;
    }
}
