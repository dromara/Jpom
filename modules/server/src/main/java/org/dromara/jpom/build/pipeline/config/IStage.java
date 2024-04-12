package org.dromara.jpom.build.pipeline.config;

import org.dromara.jpom.build.pipeline.enums.StageType;
import org.dromara.jpom.build.pipeline.enums.SubStageType;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
public interface IStage extends IVerify<IStage> {
    /**
     * 获取流程类型
     *
     * @return 流程类型
     */
    StageType getStageType();

    /**
     * 获取子流程类型
     *
     * @return 流程类型
     */
    default SubStageType getSubStageType() {
        return null;
    }

    /**
     * 获取仓库标记
     *
     * @return 仓库标记
     */
    String getRepoTag();

    /**
     * 描述
     *
     * @return 描述
     */
    String getDescription();
}
