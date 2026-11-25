/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.core.exec;

import java.util.Collections;
import java.util.List;

/**
 * 进程树终止结果
 * <p>
 * 语义要点：{@link #isSuccess()} 表示「确认整棵进程树都已退出」，不是「发出过信号」。
 * 取消构建必须以这个结果决定落「已取消」还是「异常关闭」，不能凭「调用过 kill」就当成功。
 *
 * @author jpom
 * @since 2.11.16
 */
public final class ProcessKillResult {

    private final boolean success;
    private final List<Long> remainingPids;
    private final boolean groupKilled;

    ProcessKillResult(boolean success, List<Long> remainingPids, boolean groupKilled) {
        this.success = success;
        this.remainingPids = remainingPids == null ? Collections.emptyList() : Collections.unmodifiableList(remainingPids);
        this.groupKilled = groupKilled;
    }

    /**
     * @return 是否确认进程树已全部退出
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return 仍然存活的 pid（成功时为空）
     */
    public List<Long> getRemainingPids() {
        return remainingPids;
    }

    /**
     * @return 是否按进程组终止
     */
    public boolean isGroupKilled() {
        return groupKilled;
    }

    @Override
    public String toString() {
        return "ProcessKillResult{success=" + success + ", groupKilled=" + groupKilled + ", remainingPids=" + remainingPids + '}';
    }
}
