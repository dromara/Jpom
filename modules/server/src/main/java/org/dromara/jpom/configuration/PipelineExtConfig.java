package org.dromara.jpom.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
@Configuration
@ConfigurationProperties(prefix = "jpom.build.pipeline")
@Data
public class PipelineExtConfig {
    /**
     * ${data-path}/pipeline/${workspaceId}/${pipelineId}/source/${concurrentTag}-${repoTag}
     */
    private String storageLocation;
}
