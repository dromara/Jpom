/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.update;

import lombok.Data;

/**
 * 一种引擎（及平台）的升级包状态
 *
 * <p>在线升级页按「节点实际使用的插件端类型」逐条展示：Java 升级包只有一份，Go 升级容器按平台各一份。
 * 混合部署时必须两类都能看到状态，否则用户只能升级其中一类。</p>
 *
 * @author jpom
 * @since 2.11.15.4
 */
@Data
public class AgentUpgradePackageStatus {

    /**
     * 引擎：java / go
     */
    private String engine;
    /**
     * 平台（os/arch，仅 Go 升级容器有值）
     */
    private String platform;
    /**
     * 本地升级包版本（不存在时为 null）
     */
    private String version;
    /**
     * 本地升级包打包时间（不存在时为 null）
     */
    private String timeStamp;
    /**
     * 本地升级包文件名（不存在时为 null）
     */
    private String fileName;
    /**
     * 本地没有该类型的升级包
     */
    private boolean missing;
    /**
     * 远端最新版本是否已知（下载中心不可用时为 false，此时不能判断是否已是最新）
     */
    private boolean latestKnown;
    /**
     * 本地升级包是否已是远端最新版本（{@link #latestKnown} 为 false 时无意义）
     */
    private boolean upToDate;
}
