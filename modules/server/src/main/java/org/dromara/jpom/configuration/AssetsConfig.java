/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import cn.hutool.core.util.ObjectUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 资产配置
 *
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@ConfigurationProperties("jpom.assets")
@Data
@Configuration
@EnableConfigurationProperties({AssetsConfig.SshConfig.class, AssetsConfig.DockerConfig.class})
public class AssetsConfig {
    /**
     * 监控线程池大小,小于等于0 为CPU核心数
     */
    private int monitorPoolSize = 0;

    /**
     * 监控任务等待数量，超过此数量将取消监控任务，值最小为 1
     */
    private int monitorPoolWaitQueue = 500;
    /**
     * ssh 资产配置
     */
    private SshConfig ssh;
    /**
     * docker 资产配置
     */
    private DockerConfig docker;

    public SshConfig getSsh() {
        return ObjectUtil.defaultIfNull(this.ssh, () -> {
            this.ssh = new SshConfig();
            return ssh;
        });
    }

    public DockerConfig getDocker() {
        return ObjectUtil.defaultIfNull(this.docker, () -> {
            this.docker = new DockerConfig();
            return docker;
        });
    }

    /**
     * ssh 配置
     */
    @Data
    @ConfigurationProperties("jpom.assets.ssh")
    public static class SshConfig {

        /**
         * 监控频率
         */
        private String monitorCron;
        /**
         * 禁用监控的分组名 （如果想禁用所有配置 * 即可）
         */
        private List<String> disableMonitorGroupName;

    }

    /**
     * docker 配置
     */
    @Data
    @ConfigurationProperties("jpom.assets.docker")
    public static class DockerConfig {

        /**
         * 监控频率
         */
        private String monitorCron;
    }
}
