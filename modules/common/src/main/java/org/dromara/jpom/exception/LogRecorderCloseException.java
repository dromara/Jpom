/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.exception;

import org.dromara.jpom.common.i18n.I18nMessageUtil;

/**
 * @author bwcx_jzy
 * @since 24/1/4 004
 */
public class LogRecorderCloseException extends IllegalStateException {
    public LogRecorderCloseException() {
        super(I18nMessageUtil.get("i18n.log_recorder_error_message.ee3e"));
    }
}
