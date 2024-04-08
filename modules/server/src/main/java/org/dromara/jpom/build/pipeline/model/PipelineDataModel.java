package org.dromara.jpom.build.pipeline.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.build.pipeline.model.config.PipelineConfig;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseGroupModel;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "BUILD_PIPELINE_INFO", name = "构建流水线信息")
@Data
@Builder
public class PipelineDataModel extends BaseGroupModel {

    /**
     * 流水线名称
     */
    private String name;
    /**
     * 流水线的配置
     *
     * @see PipelineConfig
     */
    private String jsonConfig;
}
