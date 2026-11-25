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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

/**
 * 发布命令退出码口径的行为级测试（AGENTS.md §0.8）
 * <p>
 * 反证：把 {@link ReleaseCommandExitCode#failed(int)} 改回 {@code exitCode != 0} 时，
 * {@code unknownExitCodeIsNotFailure} 会失败；把 {@link ReleaseCommandExitCode#unknown(int)} 改回
 * {@code exitCode < 0} 时，{@code otherNegativeExitCodeIsNotUnknown} 会失败；把 {@code shouldFailTarget}
 * 改成恒 true 时，{@code nonZeroWithoutStrictDoesNotFailTarget} 会失败；把严格模式下的「-1 判失败」去掉时，
 * {@code unknownWithStrictFailsTarget} 会失败（2026-11-25 群反馈「exit 1 仍判成功」的回归点）。
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
    void unknownExitCodeIsNotFailure() {
        // -1 是 JSch 读不到退出码的返回值：它不是「命令返回非 0」，只是「读不到」
        Assertions.assertTrue(ReleaseCommandExitCode.unknown(-1));
        Assertions.assertFalse(ReleaseCommandExitCode.failed(-1));
    }

    @Test
    void otherNegativeExitCodeIsNotUnknown() {
        // -100 是 SSH 项目执行器的「执行异常/未配置脚本」哨兵值，不能混同 JSch 的 -1
        Assertions.assertFalse(ReleaseCommandExitCode.unknown(-100));
        Assertions.assertFalse(ReleaseCommandExitCode.unknown(-2));
        Assertions.assertFalse(ReleaseCommandExitCode.shouldFailTarget(-100, true));
    }

    @Test
    void unknownWithoutStrictDoesNotFailTarget() {
        // 未开严格执行：读不到退出码只提示，不改判发布结果（2.11.15 行为）
        Assertions.assertFalse(ReleaseCommandExitCode.shouldFailTarget(-1, false));
    }

    @Test
    void unknownWithStrictFailsTarget() {
        // 开严格执行：读不到退出码＝不能证明成功，按失败处理（2026-11-25 群反馈复测口径）
        Assertions.assertTrue(ReleaseCommandExitCode.shouldFailTarget(-1, true));
    }

    @Test
    void unknownFailureMessageIsNotEmpty() {
        String message = i18nMessage(() -> ReleaseCommandExitCode.unknownFailureMessage("发布后命令"));
        if (message == null) {
            return;
        }
        Assertions.assertTrue(message.contains("发布后命令") || message.contains("release_command_exit_unknown_strict"), message);
    }

    /**
     * 在无 Spring 上下文的单测 JVM 中安全取 i18n 文案：初始化失败时跳过该断言
     *
     * @param supplier 取文案
     * @return 文案；不可用返回 null
     */
    private static String i18nMessage(Supplier<String> supplier) {
        try {
            return supplier.get();
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            Assumptions.assumeTrue(false, "单测 JVM 无 Spring 上下文，i18n 不可用：" + e.getMessage());
            return null;
        }
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
        // 单测 JVM 没有 Spring 上下文时 I18nMessageUtil 会初始化失败：此处跳过，
        // 文案正文由 --i18n 四语言门禁与 dev 真机截图覆盖
        String message = i18nMessage(() -> ReleaseCommandExitCode.failureMessage("发布后命令", 1));
        if (message == null) {
            return;
        }
        Assertions.assertNotNull(message);
        Assertions.assertFalse(message.trim().isEmpty(), message);
        Assertions.assertTrue(message.contains("发布后命令") || message.contains("release_command_non_zero_exit_code"), message);
    }

}
