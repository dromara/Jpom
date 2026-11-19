/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.jpom.db.TableName;
import org.dromara.jpom.model.BaseWorkspaceModel;

/**
 * 构建在节点上生成的版本快照（与构建历史关联，用于快速回滚）
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "NODE_BUILD_VERSION",
    nameKey = "i18n.node_build_version.c4a9")
@Data
public class NodeBuildVersionModel extends BaseWorkspaceModel {

    /**
     * 构建配置ID
     */
    private String buildDataId;

    /**
     * 构建编号
     */
    private Integer buildNumberId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 节点上的版本ID
     */
    private String versionId;

    /**
     * 版本目录（排障用）
     */
    private String versionDir;

    /**
     * 版本内容指纹
     */
    private String manifest;

    /**
     * 版本文件大小
     */
    private Long fileSize;

    /**
     * 版本文件个数
     */
    private Integer fileCount;

    /**
     * 状态{0: 生成中; 1: 已下发; 2: 当前运行; 3: 已废弃; 4: 已清理}
     */
    private Integer status;

    /**
     * 服务端构建产物路径（重传发布兜底用）
     */
    private String resultDirFile;

    /**
     * 说明
     */
    private String message;
}
