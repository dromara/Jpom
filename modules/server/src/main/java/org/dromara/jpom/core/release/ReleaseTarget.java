/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.core.release;

import org.dromara.jpom.build.ReleaseManage;

/**
 * 发布目标契约（装配点）
 * <p>
 * 一种发布方式 = 一个实现类 + {@link ReleaseTargetRegistry} 注册一行；调用方（构建编排、流水线）
 * 只认 {@link #method()} 与 {@link #release}，不再用 switch 分派。
 * <p>
 * 本轮（2.11.16）为**等价重构**：实现体暂时委托 {@link ReleaseManage} 里既有的发布方法，
 * 行为与重构前逐一相同；后续把实现搬进各自的 Target（流水线阶段复用同一批实现），
 * 调用方零改动。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public interface ReleaseTarget {

    /**
     * 对应的发布方式码
     *
     * @return 码值，见 {@code BuildReleaseMethod}
     */
    int method();

    /**
     * 执行发布
     *
     * @param manage 发布上下文（产物、日志、用户、构建编号都在里面）
     * @return 错误信息；null 表示成功
     * @throws Exception 发布过程异常（与历史行为一致：由上层判定构建失败）
     */
    String release(ReleaseManage manage) throws Exception;
}
