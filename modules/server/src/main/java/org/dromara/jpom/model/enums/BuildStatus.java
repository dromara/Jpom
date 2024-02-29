/*
 * Copyright (c) 2019 Code Technology Studio
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
