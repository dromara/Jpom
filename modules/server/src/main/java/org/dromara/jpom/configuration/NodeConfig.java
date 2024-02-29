/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.configuration;

import cn.hutool.core.util.RuntimeUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

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
    /**
     * web socket 消息最大长度
     */
    private DataSize webSocketMessageSizeLimit = DataSize.ofMegabytes(5);

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
