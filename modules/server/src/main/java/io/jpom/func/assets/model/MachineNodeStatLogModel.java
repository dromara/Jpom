/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.func.assets.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import top.jpom.h2db.TableName;
import top.jpom.model.BaseDbModel;

/**
 * @author bwcx_jzy
 * @see io.jpom.model.data.NodeModel
 * @since 2023/02/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_NODE_STAT_LOG", name = "资产机器节点统计")
@Data
public class MachineNodeStatLogModel extends BaseDbModel {
    /**
     * 机器id
     */
    private String machineId;
    /**
     * 占用cpu
     */
    private Double occupyCpu;
    /**
     * 占用内存 （总共）
     */
    private Double occupyMemory;
    /**
     * 占用磁盘
     */
    private Double occupyDisk;
    /**
     * 监控时间
     * 插件端返回的时间
     */
    private Long monitorTime;
    /**
     * 网络耗时（延迟）
     */
    private Integer networkDelay;
    /**
     * 每秒发送的KB数,rxkB/s
     */
    private Long netTxBytes;

    /**
     * 每秒接收的KB数,rxkB/s
     */
    private Long netRxBytes;
}
