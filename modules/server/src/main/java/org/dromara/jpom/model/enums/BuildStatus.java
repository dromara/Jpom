/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.enums;

import lombok.Getter;
import org.dromara.jpom.model.BaseEnum;

/**
 * @author bwcx_jzy
 * @since 2021/8/27
 */
@Getter
public enum BuildStatus implements BaseEnum {
    /**
     *
     */
    No(0, "未构建"),
    Ing(1, "构建中", true),
    Success(2, "构建结束"),
    Error(3, "构建失败"),
    PubIng(4, "发布中", true),
    PubSuccess(5, "发布成功"),
    PubError(6, "发布失败"),
    Cancel(7, "取消构建"),
    Interrupt(8, "构建中断"),
    WaitExec(9, "队列等待", true),
    AbnormalShutdown(10, "异常关闭"),
    /**
     * 已受理取消请求，正在等待进程树退出（此时不能显示「已取消」）
     * <p>
     * 追加码（2.11.16）：既有 0–10 的语义不变。终态只有两种：
     * 进程树确认退出 → {@link #Cancel}；杀不掉/找不到执行体 → {@link #AbnormalShutdown}（附残留 pid）。
     * </p>
     */
    CancelRequested(11, "取消中", true),
    ;

    private final int code;
    private final String desc;
    private final boolean progress;

    BuildStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
        this.progress = false;
    }

    BuildStatus(int code, String desc, boolean progress) {
        this.code = code;
        this.desc = desc;
        this.progress = progress;
    }

}
