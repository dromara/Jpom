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

/**
 * 流水线阶段类型
 * <p>
 * 新增一种阶段 = 枚举加一项 + 实现 {@code StageExecutor} + 注册一行；调用方（引擎）零改动。
 *
 * @author jpom
 * @since 2.11.16
 */
public enum PipelineStageType {
    /**
     * 构建：在某个仓库源的工作目录里执行命令并产出产物
     */
    BUILD,
    /**
     * 自定义命令：不产出产物，常用于测试/检查
     */
    COMMAND,
    /**
     * 发布：把一个已产出阶段的产物发布到 N 个目标（跨方式）
     */
    RELEASE,
    /**
     * 人工审批：达到条件后等待审批（超时判失败）
     */
    APPROVAL,
    /**
     * 通知：阶段级通知（成功/失败/跳过）
     */
    NOTIFY,
}
