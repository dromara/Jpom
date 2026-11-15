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

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 插件端运行引擎（Java 版 / Go 版）
 *
 * <p>节点引擎优先读取 Agent /info 返回的 manifest.engine 字段；旧版 Go 插件端没有该字段时，
 * 由 AgentPackageResolver#resolveEngine 使用 manifest.jarFile 是否以 .jar 结尾做兜底判断
 * （Java 插件端的运行文件一定是 jar，Go 插件端是平台可执行文件）。</p>
 *
 * @author jpom
 * @since 2.11.13
 */
@Getter
public enum AgentEngine {
    /**
     * Java 版插件端
     */
    JAVA("java", "Java"),
    /**
     * Go 版插件端
     */
    GO("go", "Go");

    /**
     * /info manifest.engine 约定的值
     */
    private final String name;
    /**
     * 展示名称
     */
    private final String display;

    AgentEngine(String name, String display) {
        this.name = name;
        this.display = display;
    }

    /**
     * 解析引擎名称，未知值按 Java 处理（保持旧行为）
     *
     * @param value manifest.engine 值
     * @return 引擎
     */
    public static AgentEngine parse(String value) {
        if (StrUtil.isBlank(value)) {
            return JAVA;
        }
        return StrUtil.equalsIgnoreCase(StrUtil.trim(value), GO.name) ? GO : JAVA;
    }
}
