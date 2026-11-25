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

import java.util.Map;

/**
 * 通知出口
 * <p>
 * 生产实现复用「脚本库」执行通知脚本；测试注入假实现，
 * 因此「多通知 / 单个通知失败不影响主流程」可以在单测里验证。
 *
 * @author jpom
 * @since 2.11.16
 */
public interface PipelineNotifier {

    /**
     * 发送一条通知
     *
     * @param target  目标（脚本 ID / 地址等）
     * @param event   事件（run_finished / stage_failed / stage_skipped）
     * @param payload 变量（流水线、运行、阶段、状态）
     * @return null 表示成功，否则错误信息
     */
    String notify(String target, String event, Map<String, String> payload) throws Exception;
}
