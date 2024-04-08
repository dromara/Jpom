package org.dromara.jpom.build.pipeline.model.config;

import org.dromara.jpom.build.pipeline.model.StageType;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
public interface IStage extends IVerify {
    /**
     * 获取流程类型
     *
     * @return 流程类型
     */
    StageType getStageType();

    /**
     * 获取仓库标记
     *
     * @return 仓库标记
     */
    String getRepoTag();
}
