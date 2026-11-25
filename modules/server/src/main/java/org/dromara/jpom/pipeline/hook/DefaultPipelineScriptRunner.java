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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.model.script.ScriptExecuteLogModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.service.script.ScriptExecuteLogServer;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.util.CommandUtil;
import org.dromara.jpom.util.LogRecorder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 脚本执行默认实现：与服务端脚本手动执行、在线构建事件脚本同一套执行链
 * <p>
 * 复用 {@code ScriptExecuteLogServer.toExecLogFile}（模板 + 脚本库引用替换）与
 * {@code CommandUtil.execWaitFor}（独立进程组 + 进程树终止），因此：脚本执行记录会出现在
 * 「服务端脚本 → 执行记录」（triggerExecType=3 构建事件），取消走同一个进程树内核。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Service
public class DefaultPipelineScriptRunner implements PipelineScriptRunner {

    private final ScriptServer scriptServer;
    private final ScriptExecuteLogServer scriptExecuteLogServer;

    public DefaultPipelineScriptRunner(ScriptServer scriptServer, ScriptExecuteLogServer scriptExecuteLogServer) {
        this.scriptServer = scriptServer;
        this.scriptExecuteLogServer = scriptExecuteLogServer;
    }

    @Override
    public int run(PipelineScriptRequest request, Consumer<String> lineSink, Consumer<Process> processSink) throws Exception {
        if (request == null || StrUtil.isBlank(request.getScriptId())) {
            throw new IllegalArgumentException(PipelineHookMessages.get("i18n.pipeline_hook_script_required.8c08"));
        }
        ScriptModel scriptModel = scriptServer.getByKey(request.getScriptId(), false);
        if (scriptModel == null) {
            throw new IllegalArgumentException(StrUtil.format(
                PipelineHookMessages.get("i18n.pipeline_hook_script_not_exists.8c02"), request.getScriptId()));
        }
        String workspaceId = StrUtil.emptyToDefault(request.getWorkspaceId(), scriptModel.getWorkspaceId());
        // 执行记录：triggerExecType=3 与在线构建事件脚本同源，便于在脚本执行记录里按来源筛选
        ScriptExecuteLogModel logModel = scriptExecuteLogServer.create(scriptModel, 3, workspaceId);
        File logFile = scriptModel.logFile(logModel.getId());
        LogRecorder scriptLog = LogRecorder.builder().file(logFile).build();
        File scriptFile = null;
        try {
            scriptFile = scriptExecuteLogServer.toExecLogFile(scriptModel);
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.ING);
            Map<String, String> env = request.getEnv();
            int exitCode = CommandUtil.execWaitFor(scriptFile, request.getWorkDir(), env, StrUtil.EMPTY,
                (line, process) -> {
                    // 脚本日志与运行日志同时写：脚本记录可单独回看，运行日志能实时看到
                    scriptLog.info(line);
                    if (lineSink != null) {
                        lineSink.accept(line);
                    }
                },
                process -> {
                    if (processSink != null) {
                        processSink.accept(process);
                    }
                });
            scriptLog.info(PipelineHookMessages.get("i18n.pipeline_hook_script_exit_code.8c12"), exitCode);
            // 与在线构建一致：记录 DONE + 真实退出码（非 0 是否失败由调用方按事件语义判定）
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.DONE, exitCode);
            return exitCode;
        } catch (Exception e) {
            scriptExecuteLogServer.updateStatus(logModel.getId(), CommandExecLogModel.Status.ERROR);
            throw e;
        } finally {
            if (scriptFile != null) {
                FileUtil.del(scriptFile);
            }
            IoUtil.close(scriptLog);
        }
    }
}
