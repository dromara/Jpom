/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.build;

import org.dromara.jpom.model.enums.BuildStatus;

/**
 * 取消结果策略：把「是否确认已停止」映射成终态
 * <p>
 * 语义只有一条：**确认进程树退出才算已取消**，否则如实记录异常关闭（附卡住的阶段）。
 * 单独抽出来是为了让这条规则有行为级单测——它决定了界面会不会出现「已取消但进程还在跑」。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class CancelOutcomePolicy {

    private CancelOutcomePolicy() {
    }

    /**
     * 取消终态
     *
     * @param confirmed 是否确认进程树已退出（本机看进程、节点看回执）
     * @return {@link BuildStatus#Cancel} 或 {@link BuildStatus#AbnormalShutdown}
     */
    public static BuildStatus resolve(boolean confirmed) {
        return confirmed ? BuildStatus.Cancel : BuildStatus.AbnormalShutdown;
    }

    /**
     * 受理取消请求时的过渡态
     *
     * @return {@link BuildStatus#CancelRequested}
     */
    public static BuildStatus pending() {
        return BuildStatus.CancelRequested;
    }
}
