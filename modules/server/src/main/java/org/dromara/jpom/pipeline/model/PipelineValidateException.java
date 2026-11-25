/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.model;

import lombok.Getter;

/**
 * 流水线定义校验异常
 * <p>
 * 刻意不带 i18n：校验逻辑要在单测里直接跑（i18n 依赖 Spring 的 MessageSource），
 * 因此这里只带 i18n key 与具体位置，由接口层翻译成用户可读文案。
 *
 * @author jpom
 * @since 2.11.16
 */
@Getter
public class PipelineValidateException extends IllegalArgumentException {

    private final String key;
    private final String detail;

    public PipelineValidateException(String key, String detail) {
        super(key + (detail == null ? "" : ":" + detail));
        this.key = key;
        this.detail = detail;
    }
}
