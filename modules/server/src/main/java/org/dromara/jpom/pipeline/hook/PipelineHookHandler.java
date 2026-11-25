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

/**
 * 事件钩子投递契约
 * <p>
 * 一种投递方式（脚本 / webhook / 以后可能的 IM）＝一个实现类 + 在
 * {@link PipelineHookHandlerRegistry} 注册一行；{@link PipelineHookDispatcher} 只认
 * {@link #type()}，新增方式不需要改它。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public interface PipelineHookHandler {

    /**
     * @return 配置里的 type 值（script / webhook）
     */
    String type();

    /**
     * 执行一次投递
     *
     * @param hook    钩子配置
     * @param context 执行上下文（含记录对象）
     * @return null 表示成功；否则为失败信息（会被写进记录与运行日志）
     * @throws Exception 投递异常（调度器按失败处理）
     */
    String handle(PipelineHookDef hook, PipelineHookContext context) throws Exception;
}
