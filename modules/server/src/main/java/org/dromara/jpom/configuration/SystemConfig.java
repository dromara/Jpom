package org.dromara.jpom.configuration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.socket.ServiceFileTailWatcher;
import org.dromara.jpom.system.BaseSystemConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("jpom.system")
public class SystemConfig extends BaseSystemConfig {

    @Override
    public void setLogCharset(Charset logCharset) {
        super.setLogCharset(logCharset);
        ServiceFileTailWatcher.setCharset(getLogCharset());
    }
}
