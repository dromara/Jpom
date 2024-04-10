package org.dromara.jpom.build.pipeline.model.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@Data
public class Repository implements IVerify {

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

    @Override
    public void verify(String prefix) {
        Assert.hasLength(repositoryId, prefix + "仓库ID不能为空");
        Assert.state(StrUtil.isNotEmpty(branchName) || StrUtil.isNotEmpty(branchTagName), prefix + "仓库分支或标签不能为空");
    }
}
