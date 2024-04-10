package org.dromara.jpom.build.pipeline.model;

import com.alibaba.fastjson2.JSONObject;
import org.dromara.jpom.build.pipeline.model.config.BasePublishStage;
import org.dromara.jpom.build.pipeline.model.config.PipelineConfig;
import org.dromara.jpom.build.pipeline.model.config.publish.PublishStageByProject;
import org.dromara.jpom.build.pipeline.model.config.stage.StageExecCommand;
import org.dromara.jpom.build.pipeline.model.enums.IEnum;
import org.dromara.jpom.build.pipeline.model.enums.StageType;
import org.dromara.jpom.build.pipeline.model.enums.SubStageType;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class StageTypeFactory {

    private static final Map<IEnum, IResolve<?>> STAGE_MAP = new HashMap<>();

    static {
        STAGE_MAP.put(StageType.EXEC, new ExecResolve());
        STAGE_MAP.put(StageType.PUBLISH, new PublishResolve());
        STAGE_MAP.put(SubStageType.PUBLISH_PROJECT, new PublishProjectResolve());
    }

    /**
     * 解析流程
     *
     * @param jsonObject json
     * @param <T>        流程泛型类
     * @return bean
     */
    public static <T> T resolve(JSONObject jsonObject) {
        StageType stageType = PipelineConfig.likeEnum(StageType.class, jsonObject.getString("stageType"), "流程类型");
        IResolve<?> iResolve = STAGE_MAP.get(stageType);
        Assert.notNull(iResolve, "未找到对应的流程类型" + stageType);
        return (T) iResolve.resolve(jsonObject);
    }

    private interface IResolve<T> {
        /**
         * 解析数据
         *
         * @param jsonObject json
         * @return 解析后的 bean
         */
        T resolve(JSONObject jsonObject);
    }

    private static class ExecResolve implements IResolve<StageExecCommand> {
        @Override
        public StageExecCommand resolve(JSONObject jsonObject) {
            return jsonObject.to(StageExecCommand.class);
        }
    }

    private static class PublishResolve implements IResolve<BasePublishStage> {
        @Override
        public BasePublishStage resolve(JSONObject jsonObject) {
            SubStageType subStageType = PipelineConfig.likeEnum(SubStageType.class, jsonObject.getString("subStageType"), "流程二级类型");
            IResolve<?> iResolve = STAGE_MAP.get(subStageType);
            Assert.notNull(iResolve, "未找到对应的二级类型" + subStageType);
            return (BasePublishStage) iResolve.resolve(jsonObject);
        }
    }

    private static class PublishProjectResolve implements IResolve<PublishStageByProject> {

        @Override
        public PublishStageByProject resolve(JSONObject jsonObject) {
            return jsonObject.to(PublishStageByProject.class);
        }
    }
}
