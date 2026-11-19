/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import lombok.Data;

/**
 * 项目可查看的运行日志文件（控制台日志文件下拉列表中的一项）
 *
 * @author bwcx_jzy
 * @since 2026/11/19
 */
@Data
public class ProjectLogFileItem {

    /**
     * 日志标识，客户端切换日志时回传该值（不直接回传文件路径）
     */
    private String key;
    /**
     * 显示名称
     */
    private String name;
    /**
     * 日志文件绝对路径
     */
    private String path;
    /**
     * 文件编码
     */
    private String charset;
    /**
     * 文件是否存在
     */
    private Boolean exists;
    /**
     * 文件大小（字节）
     */
    private Long size;
    /**
     * 文件大小（可读）
     */
    private String sizeText;
}
