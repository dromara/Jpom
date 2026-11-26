/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.repo;

import org.dromara.jpom.common.i18n.I18nMessageUtil;

/**
 * 仓库源同步的日志文案
 * <p>
 * {@link I18nMessageUtil} 依赖 Spring 的 {@code MessageSource}，而 {@link CachedGitSourceFetcher}
 * 要求能在没有 Spring 上下文的情况下被测试（缓存行为是行为级回归的重点）。这里统一兜底：
 * 取不到文案时退回 key 本身，既不在代码里复制一份中文（会和语言包漂移），也不会让测试因缺 Bean 失败。
 * </p>
 *
 * @author jpom
 * @since 2.11.15.3
 */
final class PipelineSourceMessages {

    private PipelineSourceMessages() {
    }

    static String text(String key) {
        try {
            return I18nMessageUtil.get(key);
        } catch (Exception | LinkageError e) {
            // 没有 Spring 上下文时取不到语言包（单测、脚本化调用），退回 key，别让国际化成拦路虎
            return key;
        }
    }
}
