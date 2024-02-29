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

import org.dromara.jpom.model.BaseEnum;

/**
 * backup type
 *
 * @author Hotstrip
 * @since 2021-11-27
 */
public enum BackupStatusEnum implements BaseEnum {
    /**
     * 状态{0: 处理中; 1: 成功; 2: 失败}
     */
    DEFAULT(0, "处理中"),
    SUCCESS(1, "备份成功"),
    FAILED(2, "备份失败"),
    ;

    BackupStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    final int code;
    final String desc;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
