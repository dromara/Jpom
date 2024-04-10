package org.dromara.jpom.build.pipeline.model.config;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Data;
import org.dromara.jpom.build.pipeline.model.StageGroup;
import org.dromara.jpom.build.pipeline.StageTypeFactory;

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
public class PipelineConfig implements IVerify<PipelineConfig> {
    /**
     * 版本号
     */
    private String version;
    /**
     * 调试模式
     * <p>
     * 输出更多日志
     */
    private Boolean debug;
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
        //解析流程组
        Optional.ofNullable(jsonObject.getJSONArray("stageGroups"))
            .map(PipelineConfig::parseStageGroups)
            .ifPresent(pipelineConfig::setStageGroups);
        return pipelineConfig;
    }

    private static List<StageGroup> parseStageGroups(JSONArray stagesGroupArray) {
        return stagesGroupArray.stream()
            .map(o -> {
                JSONObject jsonObject12 = (JSONObject) o;
                StageGroup stageGroup = jsonObject12.to(StageGroup.class);
                // 解析子流程
                Optional.ofNullable(jsonObject12.getJSONArray("stages"))
                    .map(PipelineConfig::parseStage)
                    .ifPresent(stageGroup::setStages);
                return stageGroup;
            }).collect(Collectors.toList());
    }

    private static List<IStage> parseStage(JSONArray stagesArray) {
        return stagesArray.stream()
            .map((Function<Object, IStage>) o1 -> StageTypeFactory.resolve((JSONObject) o1))
            .collect(Collectors.toList());
    }

    @Override
    public PipelineConfig verify(String prefix) {
        Assert.notEmpty(repositories, "流水线源仓库不能为空");
        repositories.forEach((repositoryTag, repository) -> repository.verify(repositoryTag));
        List<StageGroup> stageGroups = this.getStageGroups();
        Assert.notEmpty(stageGroups, "流水线流程组不能为空");
        for (int i = 0; i < stageGroups.size(); i++) {
            StageGroup stageGroup = stageGroups.get(i);
            stageGroup.verify(StrUtil.format("流程组{}", i + 1));
        }
        return this;
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
