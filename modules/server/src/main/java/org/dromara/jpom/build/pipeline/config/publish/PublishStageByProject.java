package org.dromara.jpom.build.pipeline.config.publish;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.build.pipeline.config.BasePublishStage;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PublishStageByProject extends BasePublishStage {
    /**
     * 节点id
     */
    private String nodeId;
    /**
     * 项目id
     */
    private String projectId;
    /**
     * 二级目录
     */
    private String secondaryDirectory;
    /**
     * 如果是压缩包是否自动解压
     */
    private Boolean unCompression;
    /**
     * 是否清空旧包发布
     */
    private Boolean clearOld;

    /**
     * 增量同步
     */
    private Boolean diffSync;
    /**
     * 上传项目文件前先关闭
     * <p>
     * windows 文件被占用的情况
     */
    private Boolean uploadCloseFirst;

    /**
     * 分发后的操作
     * 仅在项目发布类型生效
     *
     * @see AfterOpt
     * @see BuildInfoModel#getExtraData()
     */
    private AfterOpt afterOpt;

    @Override
    public PublishStageByProject verify(String prefix) {
        super.verify(prefix);
        Assert.hasText(this.nodeId, prefix + "节点发布的节点 nodeId 不能为空");
        Assert.hasText(this.projectId, prefix + "节点发布的项目 projectId 不能为空");
        return this;
    }
}
