package org.dromara.jpom.build.pipeline.actuator;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
@Builder
public class PipelineActuator {

    private final List<List<IActuator>> actions;

    private final boolean debug;

    private final Map<String, IActuator> repositoryActions;

}
