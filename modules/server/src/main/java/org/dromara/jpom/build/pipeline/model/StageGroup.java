package org.dromara.jpom.build.pipeline.model;

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
public class StageGroup implements IVerify {
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
    public void verify(String prefix) {
        Assert.hasLength(name, "流程组名称不能为空");
        Assert.notEmpty(stages, "流程组中的子流程不能为空");
        //List<IStage> iStages = this.getStages();
        for (IStage iStage : stages) {
            iStage.verify(prefix);
        }
//        for (IStage stage : stages) {
//            stage.verify(prefix);
//        }
    }
}
