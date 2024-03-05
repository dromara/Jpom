/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 23/12/29 029
 */
@Data
@ConfigurationProperties("jpom.project")
public class ProjectConfig {
    /**
     * 项目日志配置
     */
    private ProjectLogConfig log;
    /**
     * 停止项目等待的时长 单位秒，最小为1秒
     */
    private int statusWaitTime = 10;

    /**
     * 项目状态检测间隔时间 单位毫秒，最小为1毫秒
     */
    private int statusDetectionInterval = 500;

    /**
     * 项目文件备份保留个数,大于 1 才会备份
     */
    private int fileBackupCount;

    /**
     * 限制备份指定文件后缀（支持正则）
     * [ '.jar','.html','^.+\\.(?i)(txt)$' ]
     */
    private String[] fileBackupSuffix;

    public ProjectLogConfig getLog() {
        return Optional.ofNullable(this.log).orElseGet(() -> {
            this.log = new ProjectLogConfig();
            return this.log;
        });
    }
}
