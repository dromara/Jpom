package org.dromara.jpom.configuration;

import cn.hutool.core.util.RuntimeUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author bwcx_jzy
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("jpom.node")
public class NodeConfig {
    /**
     * 检查节点心跳间隔时间,最小值 5 秒
     */
    private int heartSecond = 30;

    public int getHeartSecond() {
        return Math.max(this.heartSecond, 5);
    }

    /**
     * 上传文件的超时时间 单位秒,最短5秒中
     */
    private int uploadFileTimeout = 300;

    /**
     * 节点文件分片上传大小，单位 M
     */
    private int uploadFileSliceSize = 1;

    /**
     * 节点文件分片上传并发数,最小1 最大 服务端 CPU 核心数
     */
    private int uploadFileConcurrent = 2;

    public int getUploadFileTimeout() {
        return Math.max(this.uploadFileTimeout, 5);
    }

    public int getUploadFileSliceSize() {
        return Math.max(this.uploadFileSliceSize, 1);
    }

    public void setUploadFileConcurrent(int uploadFileConcurrent) {
        this.uploadFileConcurrent = Math.min(Math.max(uploadFileConcurrent, 1), RuntimeUtil.getProcessorCount());
    }

    /**
     * 节点统计日志保留天数，如果小于等于 0 不自动删除
     */
    private int statLogKeepDays = 3;
}
