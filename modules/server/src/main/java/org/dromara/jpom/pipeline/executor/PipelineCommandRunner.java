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

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 命令执行出口（阶段执行器只依赖这个接口）
 * <p>
 * 生产实现走 {@code CommandUtil.execWaitFor}（独立进程组 + 进程树终止），
 * 测试里注入记录型实现，因此「退出码判定、产物收集、失败策略」都能确定性验证。
 *
 * @author jpom
 * @since 2.11.16
 */
public interface PipelineCommandRunner {

    /**
     * 执行脚本
     *
     * @param workDir 工作目录
     * @param env     环境变量（会与系统环境合并）
     * @param script  脚本内容
     * @param lineSink 行回调（日志）
     * @return 退出码
     * @throws Exception 执行异常
     */
    int run(File workDir, Map<String, String> env, String script, Consumer<String> lineSink,
            Consumer<Process> processSink) throws Exception;
}