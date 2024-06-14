/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseDbModel;
import org.dromara.jpom.model.data.NodeModel;

/**
 * @author bwcx_jzy
 * @see NodeModel
 * @since 2023/02/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "MACHINE_NODE_STAT_LOG",
    nameKey = "i18n.asset_machine_node_statistics.4a03")
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
     * 交互内存
     */
    private Double occupySwapMemory;
    /**
     * 虚拟内存
     */
    private Double occupyVirtualMemory;
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
