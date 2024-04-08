package org.dromara.jpom.build.pipeline.model.config;

import lombok.Data;
import org.dromara.jpom.build.pipeline.model.StageGroup;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/7
 */
@Data
public class PipelineConfig {
    /**
     * 版本号
     */
    private String version;
    /**
     * 仓库源
     */
    private Map<String, Repository> repositories;
    /**
     * 流程组
     */
    private List<StageGroup> stageGroups;
}
