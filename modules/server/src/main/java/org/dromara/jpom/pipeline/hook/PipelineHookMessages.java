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

/**
 * 事件钩子文案出口（容错版）
 * <p>
 * 事件是旁路能力：即使语言包/Spring 上下文异常，也不能让「发通知」把流水线带崩。
 * 正常运行时返回本地化文案；取不到时退回 key 与参数（仍可排查，不抛异常）。
 * 文案 key 仍写在源码字面量里，四语言一致性门禁照常校验。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
final class PipelineHookMessages {

    private PipelineHookMessages() {
    }

    /**
     * 取本地化文案
     *
     * @param key 文案 key
     * @return 文案（失败时返回 key）
     */
    static String get(String key) {
        try {
            return I18nMessageUtil.get(key);
        } catch (Throwable e) {
            return key;
        }
    }

    /**
     * 取本地化文案并格式化
     *
     * @param key  文案 key（含 {0} 占位）
     * @param args 参数
     * @return 文案（失败时返回 key + 参数）
     */
    static String format(String key, Object... args) {
        try {
            return StrUtil.format(I18nMessageUtil.get(key), args);
        } catch (Throwable e) {
            return key + " " + StrUtil.join(",", args);
        }
    }
}
