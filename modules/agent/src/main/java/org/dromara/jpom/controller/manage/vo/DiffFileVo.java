/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.manage.vo;

import lombok.Data;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2021/12/16
 */
@Data
public class DiffFileVo {

    /**
     * 项目id
     */
    private String id;
    /**
     * 需要对比的数据
     */
    private List<DiffItem> data;
    /**
     * 需要对比的目录
     */
    private String dir;

    @Data
    public static class DiffItem {
        /**
         * 名称
         */
        private String name;
        /**
         * 文件签名
         */
        private String sha1;
    }
}
