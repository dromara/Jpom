/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.enums;

/**
 * 构建节点上报的任务状态（与插件端 /build/status、WS exit 帧的 state 一致）。
 * <p>
 * 不是数据库枚举，只用于协议解析与提示文案，故不实现 BaseEnum。
 *
 * @author jpom
 * @since 2.11.15
 */
public final class BuildBuildNodeState {

    public static final String RUNNING = "running";
    public static final String SUCCESS = "success";
    public static final String FAILED = "failed";
    public static final String CANCELLED = "cancelled";
    public static final String TIMEOUT = "timeout";
    public static final String SKIPPED = "skipped";
    public static final String LOST = "lost";

    private BuildBuildNodeState() {
    }
}
