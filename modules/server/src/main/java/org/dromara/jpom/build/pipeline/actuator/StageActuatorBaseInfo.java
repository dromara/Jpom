package org.dromara.jpom.build.pipeline.actuator;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
@AllArgsConstructor
@Getter
public class StageActuatorBaseInfo {
    /**
     * 位置
     */
    private final int[] position;
    /**
     * 流程组描述
     */
    private final String groupDescription;
    /**
     * 子流程描述
     */
    private final String description;
}
