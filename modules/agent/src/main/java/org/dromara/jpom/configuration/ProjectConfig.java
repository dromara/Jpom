/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
