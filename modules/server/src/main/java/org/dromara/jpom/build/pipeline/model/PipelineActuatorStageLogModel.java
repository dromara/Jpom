package org.dromara.jpom.build.pipeline.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * @author bwcx_jzy
 * @since 2024/4/12
 */
@EqualsAndHashCode(callSuper = true)
//@TableName(value = "BUILD_PIPELINE_INFO_TMP", name = "构建流水线信息")
@Data
public class PipelineActuatorStageLogModel extends BaseWorkspaceModel {

    private String pipelineId;

    private String actuatorId;
    /**
     * 状态
     */
    private Integer status;

    private Long finishTime;
}
