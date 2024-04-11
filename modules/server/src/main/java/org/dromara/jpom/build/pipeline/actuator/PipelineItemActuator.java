package org.dromara.jpom.build.pipeline.actuator;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.pipeline.model.config.EmptyStage;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
@Builder
@Slf4j
public class PipelineItemActuator {
    /**
     * 流水线数据目录
     */
    private final File pipelineDataDir;
    /**
     * 并发标识
     */
    private final String concurrentTag;

    private final List<List<IActuator<?>>> actuators;

    private final boolean debug;

    private final Map<String, IActuator<EmptyStage>> repositoryActuators;


    public void exec() {
        try {
            // 拉取仓库
            for (Map.Entry<String, IActuator<EmptyStage>> entry : repositoryActuators.entrySet()) {
                try (IActuator<EmptyStage> iActuator = entry.getValue()) {
                    iActuator.run();
                }
            }
            // 开始执行流程
            for (List<IActuator<?>> actuator : actuators) {
                for (IActuator<?> iActuator : actuator) {
                    iActuator.run();
                }
            }
        } catch (Exception e) {
            log.error("流水线执行异常", e);
        }
    }
}
