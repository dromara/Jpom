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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 构建相关配置
 *
 * @author bwcx_jzy
 * @since 2022/7/7
 */
@Configuration
@ConfigurationProperties(prefix = "jpom.build")
@Data
@EnableConfigurationProperties({
    PipelineConfig.class})
public class BuildExtConfig {
    /**
     * 流水线配置
     */
    private PipelineConfig pipeline = new PipelineConfig();

    /**
     * 构建最多保存多少份历史记录
     */
    private int maxHistoryCount = 1000;

    /**
     * 每一项构建最多保存的历史份数
     */
    private int itemMaxHistoryCount = 50;

    private boolean checkDeleteCommand = true;

    /**
     * 构建线程池大小,小于 1 则为不限制，默认大小为 5
     */
    private int poolSize = 5;

    /**
     * 构建任务等待数量，超过此数量将取消构建任务，值最小为 1
     */
    private int poolWaitQueue = 10;
    /**
     * 压缩折叠显示进度比例 范围 1-100
     */
    private int logReduceProgressRatio = 5;

    public void setLogReduceProgressRatio(int logReduceProgressRatio) {
        // 修正值
        this.logReduceProgressRatio = Math.min(Math.max(logReduceProgressRatio, 1), 100);
    }
}
