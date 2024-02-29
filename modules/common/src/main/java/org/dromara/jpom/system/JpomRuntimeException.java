/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system;

import cn.hutool.core.util.StrUtil;

/**
 * Jpom 运行错误
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
public class JpomRuntimeException extends RuntimeException {

    /**
     * 程序是否需要关闭
     */
    private Integer exitCode;

    public JpomRuntimeException(String message) {
        super(message);
    }

    public JpomRuntimeException(String message, Integer exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public JpomRuntimeException(String message, Throwable throwable) {
        super(StrUtil.format("{} {}", message, StrUtil.emptyToDefault(throwable.getMessage(), StrUtil.EMPTY)), throwable);
    }

    public Integer getExitCode() {
        return exitCode;
    }
}
