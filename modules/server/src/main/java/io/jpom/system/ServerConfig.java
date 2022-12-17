package io.jpom.system;

import io.jpom.socket.ServiceFileTailWatcher;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * @author bwcx_jzy
 * @since 2022/12/17
 */
@EqualsAndHashCode(callSuper = true)
@Configuration
@ConfigurationProperties("jpom")
@Data
public class ServerConfig extends BaseExtConfig implements InitializingBean {

    /**
     * 系统配置参数
     */
    private SystemConfig system;

    public SystemConfig getSystem() {
        return Optional.ofNullable(this.system).orElseGet(() -> {
            this.system = new SystemConfig();
            return this.system;
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SystemConfig systemConfig = this.getSystem();
        ServiceFileTailWatcher.setCharset(systemConfig.getLogCharset());
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class SystemConfig extends BaseSystemConfig {

    }
}
