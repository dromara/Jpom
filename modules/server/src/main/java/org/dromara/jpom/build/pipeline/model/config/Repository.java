package org.dromara.jpom.build.pipeline.model.config;

import lombok.Data;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@Data
public class Repository {

    /**
     * 仓库ID
     */
    private String repositoryId;
    /**
     * 分支
     */
    private String branchName;
    /**
     * 标签
     */
    private String branchTagName;
    /**
     * 克隆深度
     */
    private Integer cloneDepth;
    /**
     * 排序
     */
    private Integer sort;
}
