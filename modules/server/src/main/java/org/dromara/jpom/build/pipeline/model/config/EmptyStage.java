package org.dromara.jpom.build.pipeline.model.config;

import org.dromara.jpom.build.pipeline.enums.StageType;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public class EmptyStage extends BaseStage {
    @Override
    public StageType getStageType() {
        return null;
    }

    @Override
    public String getRepoTag() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public EmptyStage verify(String prefix) {
        return null;
    }
}
