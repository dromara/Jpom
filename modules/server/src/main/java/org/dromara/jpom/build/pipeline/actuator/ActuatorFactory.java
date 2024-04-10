package org.dromara.jpom.build.pipeline.actuator;

import org.dromara.jpom.build.pipeline.model.StageGroup;
import org.dromara.jpom.build.pipeline.model.config.IStage;
import org.dromara.jpom.build.pipeline.model.config.PipelineConfig;
import org.dromara.jpom.build.pipeline.model.config.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class ActuatorFactory {

    public static PipelineActuator resolve(PipelineConfig pipelineConfig) {
        Map<String, Repository> repositories = pipelineConfig.getRepositories();
        Map<String, IActuator> actuatorMap = new HashMap<>(repositories.size());
        repositories.forEach((s, repository) -> actuatorMap.put(s, new RepositoryActuator(s, repository)));
        //
        List<StageGroup> stageGroups = pipelineConfig.getStageGroups();
        stageGroups.stream().map(new Function<StageGroup, Object>() {
            @Override
            public Object apply(StageGroup stageGroup) {
                List<IStage> stages = stageGroup.getStages();
                return null;
            }
        });
        //
        Boolean debug = pipelineConfig.getDebug();
        PipelineActuator pipelineActuator = PipelineActuator.builder().repositoryActions(actuatorMap).debug(debug != null && debug).build();
        return pipelineActuator;
    }
}
