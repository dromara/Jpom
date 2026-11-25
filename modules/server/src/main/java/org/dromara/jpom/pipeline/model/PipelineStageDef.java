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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流水线阶段定义
 * <p>
 * {@link #config} 按类型解释（构建：cwd/run/outputs；发布：targets/before/after；通知：events/scripts），
 * 用 Map 承载是为了新增阶段类型不必改数据模型（避免又一次「宽表 + 70 字段」）。
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineStageDef {
    /**
     * 阶段 ID（同一流水线内唯一）
     */
    private String id;
    /**
     * 展示名
     */
    private String name;
    /**
     * 类型
     */
    private PipelineStageType type;
    /**
     * 是否启用（禁用只影响执行，不删配置）
     */
    private Boolean enabled = Boolean.TRUE;
    /**
     * 失败策略：true 后续阶段直接跳过（默认），false 继续执行但本次判定失败
     */
    private Boolean failFast = Boolean.TRUE;
    /**
     * 类型相关配置
     */
    private Map<String, Object> config = new LinkedHashMap<>();

    /**
     * @return 是否参与执行
     */
    public boolean enabled() {
        return enabled == null || enabled;
    }

    /**
     * @return 失败是否即停
     */
    public boolean failFast() {
        return failFast == null || failFast;
    }
}
