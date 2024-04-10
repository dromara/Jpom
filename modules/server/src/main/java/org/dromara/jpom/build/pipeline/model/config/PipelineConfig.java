package org.dromara.jpom.build.pipeline.model.config;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Data;
import org.dromara.jpom.build.pipeline.model.PublishType;
import org.dromara.jpom.build.pipeline.model.StageGroup;
import org.dromara.jpom.build.pipeline.model.StageType;
import org.dromara.jpom.build.pipeline.model.config.publish.PublishStageByProject;
import org.dromara.jpom.build.pipeline.model.config.stage.StageExecCommand;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2024/4/7
 */
@Data
public class PipelineConfig implements IVerify {
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

    public static PipelineConfig fromJson(String json) {
        JSONObject jsonObject = JSONObject.parseObject(json);
        PipelineConfig pipelineConfig = new PipelineConfig();
        pipelineConfig.setVersion(jsonObject.getString("version"));
        pipelineConfig.setRepositories(jsonObject.getObject("repositories", new TypeReference<Map<String, Repository>>() {
        }));
        // 解析流程组
        Optional.ofNullable(jsonObject.getJSONArray("stageGroups"))
            .map(jsonArray -> jsonArray.stream()
                .map(o -> {
                    JSONObject jsonObject12 = (JSONObject) o;
                    StageGroup stageGroup = jsonObject12.to(StageGroup.class);
                    // 解析子流程
                    Optional.ofNullable(jsonObject12.getJSONArray("stages"))
                        .map(stagesArray -> stagesArray.stream()
                            .map((Function<Object, IStage>) o1 -> {
                                JSONObject jsonObject1 = (JSONObject) o1;
                                StageType stageType = likeEnum(StageType.class, jsonObject1.getString("stageType"), "流程类型");
                                if (stageType == StageType.EXEC) {
                                    return jsonObject1.to(StageExecCommand.class);
                                } else if (stageType == StageType.PUBLISH) {
                                    PublishType publishType = likeEnum(PublishType.class, jsonObject1.getString("publishType"), "发布类型");
                                    if (publishType == PublishType.PROJECT) {
                                        return jsonObject1.to(PublishStageByProject.class);
                                    }
                                    throw new IllegalStateException("未知的发布类型：" + publishType);
                                }
                                throw new IllegalStateException("未知的流程类型：" + stageType);
                            })
                            .collect(Collectors.toList()))
                        .ifPresent(stageGroup::setStages);
                    return stageGroup;
                }).collect(Collectors.toList()))
            .ifPresent(pipelineConfig::setStageGroups);
        return pipelineConfig;
    }

    @Override
    public void verify(String prefix) {
        Assert.notEmpty(repositories, "流水线源仓库不能为空");
        repositories.forEach((repositoryTag, repository) -> repository.verify(repositoryTag));
        List<StageGroup> stageGroups = this.getStageGroups();
        for (StageGroup stageGroup : stageGroups) {
            stageGroup.verify(prefix);
        }
    }

    /**
     * 模糊匹配枚举名称
     *
     * @param enumClass 枚举类
     * @param value     需要匹配的值
     * @param errorMsg  错误描述
     * @param <T>       枚举
     * @return 枚举
     * @see cn.hutool.core.util.EnumUtil
     */
    public static <T extends Enum<T>> T likeEnum(Class<T> enumClass, String value, String errorMsg) {
        return Arrays.stream(enumClass.getEnumConstants())
            .filter(t -> StrUtil.equalsIgnoreCase(t.name(), value))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(errorMsg + "不正确：" + value));
    }
}
