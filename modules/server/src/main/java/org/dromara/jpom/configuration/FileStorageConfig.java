package org.dromara.jpom.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件管理存储
 *
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.file-storage")
public class FileStorageConfig {

    /**
     * 文件中心存储路径
     */
    private String savePah;
}
