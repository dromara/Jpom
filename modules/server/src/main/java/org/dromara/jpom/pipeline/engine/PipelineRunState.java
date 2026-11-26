/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import org.dromara.jpom.model.enums.BuildStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 流水线运行状态机
 * <p>
 * 「完善的事件状态切换机制」的判定端：把「哪个状态能切到哪个状态」写成一张表，所有运行状态写入
 * 都要先过这里。目的很具体——之前的状态是「谁最后写谁生效」，终态（成功/失败/已取消）还能被后续
 * 写入覆盖，或者从成功跳成失败，界面上看不出真实结果。
 * </p>
 * <p>
 * 允许的转换（终态集合之外一律拒绝）：
 * <ul>
 *     <li>{@code 未构建 → 运行中 / 队列等待}；</li>
 *     <li>{@code 队列等待 → 运行中 / 取消中 / 已取消 / 异常关闭}；</li>
 *     <li>{@code 运行中 → 发布中 / 取消中 / 成功 / 失败 / 已取消 / 异常关闭 / 中断}；</li>
 *     <li>{@code 取消中 → 已取消 / 异常关闭}（取消意图已登记，只能落这两种终态）；</li>
 *     <li>{@code 发布中 → 发布成功 / 发布失败 / 取消中 / 失败 / 异常关闭}；</li>
 *     <li>终态（成功 / 失败 / 发布成功 / 发布失败 / 已取消 / 中断 / 异常关闭）→ 任何状态：拒绝。</li>
 * </ul>
 * 纯逻辑类，不依赖 Spring/数据库，便于确定性单测。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineRunState {

    private static final Map<Integer, Set<Integer>> TRANSITIONS = new HashMap<>();

    static {
        // 未构建（首次插入之后不再使用，但兼容「重跑」从配置行状态出发的写入）
        allow(BuildStatus.No, BuildStatus.Ing, BuildStatus.WaitExec, BuildStatus.Cancel);
        allow(BuildStatus.WaitExec, BuildStatus.Ing, BuildStatus.CancelRequested, BuildStatus.Cancel,
            BuildStatus.AbnormalShutdown);
        allow(BuildStatus.Ing, BuildStatus.PubIng, BuildStatus.CancelRequested, BuildStatus.Success,
            BuildStatus.Error, BuildStatus.Cancel, BuildStatus.AbnormalShutdown, BuildStatus.Interrupt);
        allow(BuildStatus.PubIng, BuildStatus.PubSuccess, BuildStatus.PubError, BuildStatus.CancelRequested,
            BuildStatus.Error, BuildStatus.Cancel, BuildStatus.AbnormalShutdown);
        allow(BuildStatus.CancelRequested, BuildStatus.Cancel, BuildStatus.AbnormalShutdown);
    }

    private PipelineRunState() {
    }

    private static void allow(BuildStatus from, BuildStatus... targets) {
        Set<Integer> set = TRANSITIONS.computeIfAbsent(from.getCode(), key -> new HashSet<>());
        for (BuildStatus target : targets) {
            set.add(target.getCode());
        }
    }

    /**
     * 是否为终态
     *
     * @param status 状态码
     * @return true 表示终态（不可再变更）
     */
    public static boolean terminal(Integer status) {
        if (status == null) {
            return false;
        }
        for (BuildStatus value : BuildStatus.values()) {
            if (value.getCode() == status) {
                return !value.isProgress() && value != BuildStatus.No;
            }
        }
        return false;
    }

    /**
     * 能否从 from 转到 to
     *
     * @param from 原状态（null 视为首次写入）
     * @param to   目标状态
     * @return true 表示合法
     */
    public static boolean canTransit(Integer from, Integer to) {
        if (to == null) {
            return false;
        }
        if (from == null) {
            // 首次写入只允许「运行中 / 队列等待」
            return to == BuildStatus.Ing.getCode() || to == BuildStatus.WaitExec.getCode()
                || to == BuildStatus.No.getCode();
        }
        if (from.equals(to)) {
            // 同状态重复写入（例如轮询里重复落「取消中」）不算非法转换
            return true;
        }
        Set<Integer> targets = TRANSITIONS.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * @param status 状态码
     * @return 允许的下一跳（只读，诊断/测试用）
     */
    public static Set<Integer> next(Integer status) {
        Set<Integer> targets = status == null ? null : TRANSITIONS.get(status);
        return targets == null ? Collections.emptySet() : Collections.unmodifiableSet(targets);
    }
}
