/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket;

import lombok.Getter;

/**
 * 控制台socket 操作枚举
 *
 * @author bwcx_jzy
 * @since 2019/4/16
 */
@Getter
public enum ConsoleCommandOp {
    /**
     * 启动
     */
    start(true),
    stop(true),
    restart(true),
    status,
    /**
     * 重载
     */
    reload(true),
    /**
     * 运行日志
     */
    showlog,
    /**
     * 心跳
     */
    heart,
    ;
    /**
     * 是否支持手动操作（执行）
     */
    private final boolean canOpt;

    ConsoleCommandOp() {
        this.canOpt = false;
    }

    ConsoleCommandOp(boolean canOpt) {
        this.canOpt = canOpt;
    }
}
