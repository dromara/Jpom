package org.dromara.jpom.build.pipeline.model;

import lombok.Data;
import org.dromara.jpom.build.pipeline.model.config.IStage;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@Data
public class StageGroup {
    /**
     * 流程
     */
    private List<IStage> stages;

    /**
     * 组名
     */
    private String name;
    /**
     * 描述
     */
    private String description;
}
