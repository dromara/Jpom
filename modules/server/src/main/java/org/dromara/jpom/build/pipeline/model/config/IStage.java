package org.dromara.jpom.build.pipeline.model.config;

import com.alibaba.fastjson2.JSONObject;
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

    default JSONObject toJson() {
        return JSONObject.from(this);
    }

    default String getString(String key) {
        return null;
    }

    default IStage to() {
        return null;
    }

    /**
     * 解析流程对象
     *
     * @return IStage
     */
    default IStage analyze() {
        StageType stageTypeStr = getStageType();
//        StageType stageType = Arrays.stream(StageType.values())
//            .filter(stageType1 -> StrUtil.equalsIgnoreCase(stageTypeStr, stageType1.name()))
//            .findFirst()
//            .orElseThrow(() -> new IllegalStateException("流程类型不正确：" + stageTypeStr));
//        if (stageType == StageType.EXEC) {
//            //return jsonObject.to(StageExecCommand.class);
//        } else if (stageType == StageType.PUBLISH) {
//            //return jsonObject.to(BasePublishStage.class);
//        }
        throw new IllegalStateException("不支持的流程类型：" + stageTypeStr);
    }
}
