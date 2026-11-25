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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 发布命令退出码口径的行为级测试（AGENTS.md §0.8）
 * <p>
 * 反证：把 {@link ReleaseCommandExitCode#failed(int)} 改回 {@code exitCode != 0} 时，
 * {@code unknownIsNotFailure} 会失败；把 {@code shouldFailTarget} 改成恒 true 时，
 * {@code nonZeroWithoutStrictDoesNotFailTarget} 会失败。
 *
 * @author bwcx_jzy
 * @since 2026/11/24
 */
class ReleaseCommandExitCodeTest {

    @Test
    void zeroIsSuccess() {
        Assertions.assertFalse(ReleaseCommandExitCode.failed(0));
        Assertions.assertFalse(ReleaseCommandExitCode.unknown(0));
        Assertions.assertFalse(ReleaseCommandExitCode.shouldFailTarget(0, true));
    }

    @Test
    void nonZeroIsFailure() {
        Assertions.assertTrue(ReleaseCommandExitCode.failed(1));
        Assertions.assertTrue(ReleaseCommandExitCode.failed(124));
        Assertions.assertFalse(ReleaseCommandExitCode.unknown(1));
    }

    @Test
    void unknownIsNotFailure() {
        // -1 是 JSch 读不到退出码的返回值：未知不等于失败，否则会误判目标失败
        Assertions.assertTrue(ReleaseCommandExitCode.unknown(-1));
        Assertions.assertFalse(ReleaseCommandExitCode.failed(-1));
        Assertions.assertFalse(ReleaseCommandExitCode.shouldFailTarget(-1, true));
    }

    @Test
    void nonZeroWithoutStrictDoesNotFailTarget() {
        // 默认（未开严格）必须保持 2.11.15 行为：只记录，不改判发布结果
        Assertions.assertFalse(ReleaseCommandExitCode.shouldFailTarget(1, false));
    }

    @Test
    void nonZeroWithStrictFailsTarget() {
        Assertions.assertTrue(ReleaseCommandExitCode.shouldFailTarget(1, true));
        Assertions.assertTrue(ReleaseCommandExitCode.shouldFailTarget(127, true));
    }

    @Test
    void failureMessageIsNotEmpty() {
        // 单测环境未初始化 i18n bundle，I18nMessageUtil.get() 会原样返回 key：
        // 这里只校验消息非空且可定位（阶段文案或 key），文案正文由 dev 真机截图与 --i18n 门禁覆盖
        String message = ReleaseCommandExitCode.failureMessage("发布后命令", 1);
        Assertions.assertNotNull(message);
        Assertions.assertFalse(message.trim().isEmpty(), message);
        Assertions.assertTrue(message.contains("发布后命令") || message.contains("release_command_non_zero_exit_code"), message);
    }
}
