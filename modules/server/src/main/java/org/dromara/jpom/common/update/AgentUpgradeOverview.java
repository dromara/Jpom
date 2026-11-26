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

import java.util.List;

/**
 * 在线升级页的升级包总览
 *
 * <p>混合部署（同一服务端下同时存在 Java 与 Go 插件端）时，两类升级包需要分别准备；
 * 这里按节点实际使用的类型聚合，前端据此提示「需要上传/下载哪些包」，
 * 不再用「列表里有没有 Go 节点」来决定唯一一份包的下载行为。</p>
 *
 * @author jpom
 * @since 2.11.15.4
 */
@Data
public class AgentUpgradeOverview {

    /**
     * Java 插件端节点数量（按资产表已识别的引擎统计，不受分页影响）
     */
    private int javaCount;
    /**
     * Go 插件端节点数量
     */
    private int goCount;
    /**
     * 引擎尚未识别的节点数量（老节点第一次心跳前）
     */
    private int unknownCount;
    /**
     * 远端最新版本（下载中心不可用时为 null）
     */
    private String latestVersion;
    /**
     * 当前集群需要的升级包状态（按引擎/平台）
     */
    private List<AgentUpgradePackageStatus> packages;
}
