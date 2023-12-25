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
@EnableConfigurationProperties({AssetsConfig.SshConfig.class})
public class AssetsConfig {
    /**
     * ssh 资产配置
     */
    private SshConfig ssh;

    public SshConfig getSsh() {
        return ObjectUtil.defaultIfNull(this.ssh, () -> {
            this.ssh = new SshConfig();
            return ssh;
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
}
