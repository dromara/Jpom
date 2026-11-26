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

import cn.hutool.core.util.StrUtil;
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

    /**
     * 是否标签引用（{@code tag:名称}）
     *
     * @return true 标签
     */
    public boolean isTagRef() {
        return StrUtil.startWith(StrUtil.trimToEmpty(this.ref), "tag:");
    }

    /**
     * 引用名（去掉 {@code tag:}/{@code branch:} 前缀；留空表示仓库默认分支）
     * <p>
     * 解析只在这里做：界面、同步、缓存 key 都从同一个方法取值，避免「界面按 tag 保存、后端按分支拉」。
     * </p>
     *
     * @return 分支名/标签名
     */
    public String refName() {
        String text = StrUtil.trimToEmpty(this.ref);
        if (StrUtil.startWith(text, "tag:")) {
            return StrUtil.trimToEmpty(StrUtil.removePrefix(text, "tag:"));
        }
        if (StrUtil.startWith(text, "branch:")) {
            return StrUtil.trimToEmpty(StrUtil.removePrefix(text, "branch:"));
        }
        return text;
    }

    /**
     * 生效的克隆深度（0 与空都表示完整克隆）
     *
     * @return 深度；null 表示全量
     */
    public Integer effectiveCloneDepth() {
        return this.cloneDepth == null || this.cloneDepth <= 0 ? null : this.cloneDepth;
    }
}
