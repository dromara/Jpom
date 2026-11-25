/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.engine;

import org.dromara.jpom.pipeline.model.PipelineStageType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阶段执行器注册表
 * <p>
 * 未注册的类型返回 null，引擎据此报「阶段类型未注册」并判该阶段失败——
 * 而不是静默跳过（跳过会让人误以为流水线跑成功了）。
 *
 * @author jpom
 * @since 2.11.16
 */
public final class StageExecutorRegistry {

    private static final Map<PipelineStageType, StageExecutor> EXECUTORS = new LinkedHashMap<>();

    private StageExecutorRegistry() {
    }

    /**
     * 注册执行器
     *
     * @param executor 执行器
     */
    public static synchronized void register(StageExecutor executor) {
        StageExecutor exists = EXECUTORS.put(executor.type(), executor);
        if (exists != null) {
            throw new IllegalStateException("阶段类型重复注册：" + executor.type());
        }
    }

    /**
     * 取执行器
     *
     * @param type 类型
     * @return 执行器；未注册返回 null
     */
    public static StageExecutor get(PipelineStageType type) {
        return EXECUTORS.get(type);
    }

    /**
     * @return 已注册类型（只读，诊断/测试用）
     */
    public static Map<PipelineStageType, StageExecutor> all() {
        return Collections.unmodifiableMap(EXECUTORS);
    }

    /**
     * 清空（仅测试用）
     */
    static synchronized void clearForTest() {
        EXECUTORS.clear();
    }
}
