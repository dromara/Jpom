package org.dromara.jpom.build.pipeline.model;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.build.pipeline.model.config.IStage;
import org.dromara.jpom.build.pipeline.model.config.IVerify;
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
     * @see org.dromara.jpom.build.pipeline.model.config.stage.StageExecCommand
     * @see org.dromara.jpom.build.pipeline.model.config.publish.PublishStageByProject
     * @see IStage
     * @see org.dromara.jpom.build.pipeline.model.config.BaseStage
     * @see org.dromara.jpom.build.pipeline.model.config.BasePublishStage
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
