/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.sshproject;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * SSH 项目模板（内置资源，随版本更新）
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Data
public class SshProjectTemplate {

    private String key;
    private String name;
    private String description;
    private Integer order;
    /**
     * 表单字段
     */
    private List<Field> fields;
    /**
     * 生命周期脚本模板，key 为 start/status/stop/restart/reload
     */
    private Map<String, String> hooks;
    /**
     * 默认应用日志文件
     */
    private List<LogFile> logFiles;

    @Data
    public static class Field {
        private String key;
        private String label;
        private String type;
        private String defaultValue;
        private Boolean required;
        private String placeholder;
    }

    @Data
    public static class LogFile {
        private String name;
        private String path;
    }
}
