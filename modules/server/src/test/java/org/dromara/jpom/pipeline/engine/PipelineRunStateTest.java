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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 运行状态机测试：合法转换、终态不可改写、取消三态
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineRunStateTest {

    @Test
    public void runningCanReachEveryTerminal() {
        int ing = BuildStatus.Ing.getCode();
        int[] terminals = {
            BuildStatus.Success.getCode(),
            BuildStatus.Error.getCode(),
            BuildStatus.Cancel.getCode(),
            BuildStatus.AbnormalShutdown.getCode(),
            BuildStatus.Interrupt.getCode(),
            BuildStatus.CancelRequested.getCode()
        };
        for (int terminal : terminals) {
            Assertions.assertTrue(PipelineRunState.canTransit(ing, terminal), "运行中应可转到 " + terminal);
        }
    }

    @Test
    public void cancelRequestedOnlyReachesCancelOrAbnormal() {
        int requested = BuildStatus.CancelRequested.getCode();
        Assertions.assertTrue(PipelineRunState.canTransit(requested, BuildStatus.Cancel.getCode()));
        Assertions.assertTrue(PipelineRunState.canTransit(requested, BuildStatus.AbnormalShutdown.getCode()));
        Assertions.assertFalse(PipelineRunState.canTransit(requested, BuildStatus.Success.getCode()), "取消中不能转成功");
        Assertions.assertFalse(PipelineRunState.canTransit(requested, BuildStatus.Error.getCode()));
    }

    @Test
    public void terminalCannotBeOverwritten() {
        for (BuildStatus status : BuildStatus.values()) {
            if (!PipelineRunState.terminal(status.getCode())) {
                continue;
            }
            for (BuildStatus target : BuildStatus.values()) {
                if (target == status) {
                    continue;
                }
                Assertions.assertFalse(PipelineRunState.canTransit(status.getCode(), target.getCode()),
                    status + " 是终态，不能再改成 " + target);
            }
            Assertions.assertTrue(PipelineRunState.canTransit(status.getCode(), status.getCode()),
                "同状态重复写入不算非法转换");
        }
    }

    @Test
    public void firstWriteOnlyRunningOrQueue() {
        Assertions.assertTrue(PipelineRunState.canTransit(null, BuildStatus.Ing.getCode()));
        Assertions.assertTrue(PipelineRunState.canTransit(null, BuildStatus.WaitExec.getCode()));
        Assertions.assertFalse(PipelineRunState.canTransit(null, BuildStatus.Success.getCode()));
    }

    @Test
    public void terminalStatusSetIsWhatUiExpects() {
        Assertions.assertTrue(PipelineRunState.terminal(BuildStatus.Cancel.getCode()));
        Assertions.assertTrue(PipelineRunState.terminal(BuildStatus.AbnormalShutdown.getCode()));
        Assertions.assertFalse(PipelineRunState.terminal(BuildStatus.Ing.getCode()));
        Assertions.assertFalse(PipelineRunState.terminal(BuildStatus.CancelRequested.getCode()), "取消中仍是进行态");
        Assertions.assertFalse(PipelineRunState.terminal(BuildStatus.No.getCode()), "未构建不是终态");
    }
}
