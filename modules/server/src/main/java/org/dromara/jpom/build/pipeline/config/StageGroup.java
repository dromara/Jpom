package org.dromara.jpom.build.pipeline.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.build.pipeline.config.publish.PublishStageByProject;
import org.dromara.jpom.build.pipeline.config.stage.StageExecCommand;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@Data
public class StageGroup implements IVerify<StageGroup> {
    /**
     * 流程
     *
     * @see StageExecCommand
     * @see PublishStageByProject
     * @see IStage
     * @see BaseStage
     * @see BasePublishStage
     */
    private List<IStage> stages;
    /**
     * 组名
     */
    private String name;
    /**
     * 描述
     */
    private String description;

    @Override
    public StageGroup verify(String prefix) {
        Assert.hasLength(name, prefix + "组名称不能为空");
        Assert.notEmpty(stages, prefix + "中的子流程不能为空");
        for (int i = 0; i < stages.size(); i++) {
            IStage iStage = stages.get(i);
            iStage.verify(StrUtil.format("{}子流程{}", prefix, i + 1));
        }
        return this;
    }
}
