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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.util.CommandUtil;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 命令执行默认实现：落脚本文件 → 独立进程组执行 → 逐行回日志
 * <p>
 * 复用在线构建同一套 {@link CommandUtil#execWaitFor}（进程组 + 进程树终止），
 * 因此流水线阶段的「取消」与老构建走同一个内核，不存在两套杀进程逻辑。
 *
 * @author jpom
 * @since 2.11.16
 */
public class DefaultPipelineCommandRunner implements PipelineCommandRunner {

    @Override
    public int run(File workDir, Map<String, String> env, String script, Consumer<String> lineSink,
                   Consumer<Process> processSink) throws Exception {
        File tempFile = FileUtil.file(JpomApplication.getInstance().getTempPath(), "pipeline",
            "run-" + System.nanoTime() + "." + CommandUtil.SUFFIX);
        FileUtil.mkParentDirs(tempFile);
        // Unix 下用 LF，Windows 下批处理仍需 CRLF；模板与在线构建共用
        FileUtil.writeUtf8String(StrUtil.nullToEmpty(script), tempFile);
        try {
            return JpomApplication.getInstance().execScript(script, file -> {
                try {
                    return CommandUtil.execWaitFor(file, workDir, env, StrUtil.EMPTY,
                        (line, process) -> lineSink.accept(line),
                        processSink);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        } finally {
            FileUtil.del(tempFile);
        }
    }
}