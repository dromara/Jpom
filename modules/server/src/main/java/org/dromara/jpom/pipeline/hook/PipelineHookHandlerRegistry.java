/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.hook;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件钩子投递注册表
 * <p>
 * 重复注册同一 type 直接抛错（启动期暴露，避免运行时静默覆盖）；配置里出现未注册的 type 时
 * 调度器会明确记「未支持的投递方式」，不会静默跳过。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public final class PipelineHookHandlerRegistry {

    private final Map<String, PipelineHookHandler> handlers = new LinkedHashMap<>();

    public PipelineHookHandlerRegistry(List<PipelineHookHandler> handlerList) {
        if (handlerList != null) {
            for (PipelineHookHandler handler : handlerList) {
                PipelineHookHandler exists = handlers.put(handler.type(), handler);
                if (exists != null) {
                    throw new IllegalStateException("事件钩子投递方式重复注册：" + handler.type());
                }
            }
        }
    }

    /**
     * @param type 投递方式
     * @return 实现；未注册返回 null
     */
    public PipelineHookHandler get(String type) {
        return handlers.get(type);
    }

    /**
     * @return 已注册方式（只读）
     */
    public Map<String, PipelineHookHandler> all() {
        return Collections.unmodifiableMap(handlers);
    }
}
