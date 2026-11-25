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

/**
 * 流水线日志出口
 * <p>
 * 引擎只依赖这个接口，运行期由 LogRecorder 实现（落盘 + 前端增量拉取），
 * 测试里用内存实现——引擎因此可以脱离 Spring 做行为级单测。
 *
 * @author jpom
 * @since 2.11.16
 */
public interface PipelineLog {

    /**
     * 普通日志
     *
     * @param message 内容
     */
    void info(String message);

    /**
     * 错误日志
     *
     * @param message 内容
     */
    void error(String message);

    /**
     * 空实现（不关心日志时用）
     */
    PipelineLog NOOP = new PipelineLog() {
        @Override
        public void info(String message) {
            // 不输出
        }

        @Override
        public void error(String message) {
            // 不输出
        }
    };
}
