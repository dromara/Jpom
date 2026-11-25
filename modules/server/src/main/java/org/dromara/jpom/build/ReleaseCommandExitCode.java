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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

/**
 * 发布命令退出码判定（SSH 发布前/后命令、SSH 项目重启共用一处）
 * <p>
 * 口径（2026-11 群反馈「发布后命令 exit 1 仍判成功」后收敛）：
 * <ul>
 *     <li>0：成功；</li>
 *     <li>&gt;0：失败，但是否把该目标判失败取决于构建配置「严格执行」（strictlyEnforce，默认关，
 *     关闭时保持 2.11.15 行为：只记录）；</li>
 *     <li>&lt;0（-1）：未知，JSch 未在等待期内收到 close 消息时返回，只提示不判失败，避免误判。</li>
 * </ul>
 * 这里是纯判断点（不碰 Spring / IO），行为由 {@code ReleaseCommandExitCodeTest} 覆盖。
 *
 * @author bwcx_jzy
 * @since 2026/11/24
 */
public final class ReleaseCommandExitCode {

    /**
     * JSch 未知退出码
     */
    public static final int UNKNOWN = -1;

    private ReleaseCommandExitCode() {
    }

    /**
     * 退出码是否未知（读不到）
     *
     * @param exitCode 退出码
     * @return true 未知
     */
    public static boolean unknown(int exitCode) {
        return exitCode < 0;
    }

    /**
     * 退出码是否表示命令失败
     *
     * @param exitCode 退出码
     * @return true 失败（未知不算失败）
     */
    public static boolean failed(int exitCode) {
        return exitCode > 0;
    }

    /**
     * 是否应把该目标判定为发布失败
     *
     * @param exitCode        退出码
     * @param strictlyEnforce 构建配置「严格执行」
     * @return true 判目标失败
     */
    public static boolean shouldFailTarget(int exitCode, boolean strictlyEnforce) {
        return failed(exitCode) && strictlyEnforce;
    }

    /**
     * 非 0 退出码的失败消息（严格模式判定失败时使用）
     *
     * @param phaseLabel 阶段文案（发布前命令 / 发布后命令 / SSH 项目重启）
     * @param exitCode   退出码
     * @return 已国际化消息
     */
    public static String failureMessage(String phaseLabel, int exitCode) {
        return StrUtil.format(I18nMessageUtil.get("i18n.release_command_non_zero_exit_code.a7f5"), phaseLabel, exitCode);
    }
}
