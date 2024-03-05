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
    NULL(""),
    EDIT("修改、添加数据"),
    DEL("删除数据"),
    LIST("列表、查询"),
    DOWNLOAD("下载"),
    UPLOAD("上传"),
    EXECUTE("执行"),
    REMOTE_DOWNLOAD("下载远程文件"),
    ;

    private final String name;

    MethodFeature(String name) {
        this.name = name;
    }
}
