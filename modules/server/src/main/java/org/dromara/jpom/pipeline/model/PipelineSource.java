/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.model;

import lombok.Data;

/**
 * 流水线仓库源（一条流水线可挂多个仓库到同一工作区的不同子目录）
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineSource {
    /**
     * 源标识（阶段里用 sourceId 引用，例如 cwd=/backend）
     */
    private String id;
    /**
     * 仓库 ID（复用「仓库管理」的记录）
     */
    private String repoId;
    /**
     * 引用：{@code branch:名称} 或 {@code tag:名称}，留空按仓库默认分支
     */
    private String ref;
    /**
     * 挂载子目录（相对工作区，空＝工作区根）
     */
    private String mount;
    /**
     * 克隆深度
     */
    private Integer cloneDepth;
}
