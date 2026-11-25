/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.executor;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发布阶段里的单个目标配置
 * <p>
 * 用 DTO 而不是 {@code Map<String,Object>} + instanceof 解析：类型分派会让「新增一种目标」
 * 变成到处加分支，且字段错误只能在运行时暴露。
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class ReleaseTargetConfig {
    /**
     * 发布方式码（BuildReleaseMethod）
     */
    private int method;
    /**
     * 目标 ID
     */
    private String id;
    /**
     * 产物来源阶段（留空取最近产物）
     */
    private String from;
    /**
     * 目标附加配置（发布目录、发布前后命令、afterOpt 等）
     */
    private Map<String, Object> extra = new LinkedHashMap<>();
}
