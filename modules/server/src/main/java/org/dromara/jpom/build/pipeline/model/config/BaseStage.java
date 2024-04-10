package org.dromara.jpom.build.pipeline.model.config;

import lombok.Data;
import org.dromara.jpom.build.pipeline.model.enums.StageType;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@Data
public abstract class BaseStage implements IStage {
    /**
     * 描述
     */
    private String description;
    /**
     * 阶段类型
     */
    private StageType stageType;
    /**
     * 执行的目录
     * <p>
     * 仓库的标记
     *
     * @see PipelineConfig#getRepositories()
     */
    private String repoTag;

    @Override
    public void verify(String prefix) {
        Assert.notNull(this.stageType, "阶段类型 stageType 不能为空");
        Assert.notNull(this.repoTag, "阶段执行仓库标记 repoTag 不能为空");
        Assert.hasText(this.description, "阶段描述 description 不能为空");
    }
}
