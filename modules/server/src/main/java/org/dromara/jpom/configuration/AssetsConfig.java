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
