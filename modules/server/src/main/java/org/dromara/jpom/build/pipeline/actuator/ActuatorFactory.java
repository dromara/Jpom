package org.dromara.jpom.build.pipeline.actuator;

import cn.hutool.core.util.ReflectUtil;
import org.dromara.jpom.build.pipeline.enums.IStageType;
import org.dromara.jpom.build.pipeline.enums.StageType;
import org.dromara.jpom.build.pipeline.enums.SubStageType;
import org.dromara.jpom.build.pipeline.model.StageGroup;
import org.dromara.jpom.build.pipeline.model.config.EmptyStage;
import org.dromara.jpom.build.pipeline.model.config.IStage;
import org.dromara.jpom.build.pipeline.model.config.PipelineConfig;
import org.dromara.jpom.build.pipeline.model.config.Repository;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class ActuatorFactory {

    private static final Map<IStageType, Class<? extends IActuator<?>>> STAGE_MAP = new HashMap<>();

    static {
        STAGE_MAP.put(StageType.EXEC, ExecActuator.class);
        STAGE_MAP.put(SubStageType.PUBLISH_PROJECT, PublishProjectActuator.class);
    }

    public static PipelineItemActuator resolve(PipelineConfig pipelineConfig) {
        boolean debug = pipelineConfig.getDebug() != null && pipelineConfig.getDebug();
        //
        Map<String, Repository> repositories = pipelineConfig.getRepositories();
        Map<String, IActuator<EmptyStage>> actuatorMap = new LinkedHashMap<>(repositories.size());
        List<List<IActuator<?>>> actuators = new ArrayList<>();
        //
        List<StageGroup> stageGroups = pipelineConfig.getStageGroups();
        for (int i = 0; i < stageGroups.size(); i++) {
            //
            List<IActuator<?>> actuatorList = new ArrayList<>();
            //
            StageGroup stageGroup = stageGroups.get(i);
            List<IStage> stages = stageGroup.getStages();
            for (int j = 0; j < stages.size(); j++) {
                IStage iStage = stages.get(j);
                int[] position = {i, j};
                StageActuatorBaseInfo stageActuatorBaseInfo = new StageActuatorBaseInfo(position, stageGroup.getDescription(), iStage.getDescription());
                StageType stageType = iStage.getStageType();
                SubStageType subStageType = iStage.getSubStageType();
                Class<? extends IActuator<?>> actuatorClass;
                if (subStageType == null) {
                    actuatorClass = STAGE_MAP.get(stageType);
                } else {
                    actuatorClass = STAGE_MAP.get(subStageType);
                }
                Assert.notNull(actuatorClass, "未找到对应的执行器，stageType=" + stageType + ",subStageType=" + subStageType);
                IActuator<?> iActuator = ReflectUtil.newInstance(actuatorClass);
                ReflectUtil.setFieldValue(iActuator, "stage", iStage);
                ReflectUtil.setFieldValue(iActuator, "info", stageActuatorBaseInfo);
                ReflectUtil.setFieldValue(iActuator, "debug", debug);
                // 将使用到到仓库创建流程器
                String repoTag = iStage.getRepoTag();
                if (!actuatorMap.containsKey(repoTag)) {
                    Repository repository = repositories.get(repoTag);
                    actuatorMap.put(repoTag, new RepositoryActuator(null, repoTag, repository));
                }
                actuatorList.add(iActuator);
            }
            actuators.add(actuatorList);
        }
        //

        return PipelineItemActuator.builder()
                .actuators(actuators)
                .repositoryActuators(actuatorMap)
                .debug(false).build();
    }
}
