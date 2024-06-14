/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.permission;

import lombok.Getter;
import org.dromara.jpom.common.i18n.I18nMessageUtil;

import java.util.function.Supplier;

/**
 * 功能方法
 *
 * @author bwcx_jzy
 * @since 2019/8/13
 */
@Getter
public enum MethodFeature {
    /**
     * 没有
     */
    NULL(() -> ""),
    EDIT(() -> I18nMessageUtil.get("i18n.modify_or_add_data.e1f0")),
    DEL(() -> I18nMessageUtil.get("i18n.delete_data.40f8")),
    LIST(() -> I18nMessageUtil.get("i18n.list_and_query.c783")),
    DOWNLOAD(() -> I18nMessageUtil.get("i18n.download_action.f26e")),
    UPLOAD(() -> I18nMessageUtil.get("i18n.upload_action.d5a7")),
    EXECUTE(() -> I18nMessageUtil.get("i18n.execute.1a6a")),
    REMOTE_DOWNLOAD(() -> I18nMessageUtil.get("i18n.download_remote_file.ae84")),
    ;

    private final Supplier<String> name;

    MethodFeature(Supplier<String> name) {
        this.name = name;
    }
}
