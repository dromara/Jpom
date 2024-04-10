package org.dromara.jpom.build.pipeline.model.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.build.pipeline.model.PublishType;
import org.springframework.util.Assert;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BasePublishStage extends BaseStage {
    /**
     * 发布类型
     */
    private PublishType publishType;
    /**
     * 产物
     */
    private List<ArtifactItem> artifacts;

    @Override
    public void verify(String prefix) {
        super.verify(prefix);
        Assert.notNull(this.publishType, this.getDescription() + "阶段发布类型 publishType 不能为空");
    }
}
