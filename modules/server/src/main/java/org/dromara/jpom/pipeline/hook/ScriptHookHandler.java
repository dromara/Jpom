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

import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.PipelineLog;
import org.dromara.jpom.service.script.ScriptServer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

/**
 * 事件钩子：执行服务端脚本（同步 + 实时日志）
 * <p>
 * 与在线构建「构建事件脚本」对齐的用法：按事件选择脚本；差异是这里**同步等待退出码**并
 * 把脚本输出逐行写进运行日志，失败原因直接进 {@code eventsJson} 与运行日志，不做静默失败。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
@Component
public class ScriptHookHandler implements PipelineHookHandler {

    private final PipelineScriptRunner scriptRunner;
    private final ScriptServer scriptServer;

    public ScriptHookHandler(PipelineScriptRunner scriptRunner, ScriptServer scriptServer) {
        this.scriptRunner = scriptRunner;
        this.scriptServer = scriptServer;
    }

    @Override
    public String type() {
        return "script";
    }

    @Override
    public String handle(PipelineHookDef hook, PipelineHookContext context) {
        PipelineEvent event = context.getEvent();
        String eventCode = event == null ? StrUtil.EMPTY : event.getCode();
        PipelineContext pipelineContext = context.getPipelineContext();
        PipelineLog log = pipelineContext == null ? PipelineLog.NOOP : pipelineContext.getLog();
        PipelineEventRecord.ScriptRecord record = new PipelineEventRecord.ScriptRecord();
        record.setScriptId(hook.getScriptId());
        ScriptModel scriptModel = scriptServer == null ? null : scriptServer.getByKey(hook.getScriptId(), false);
        record.setName(scriptModel == null ? hook.getScriptId() : scriptModel.getName());
        if (context.getRecord() != null) {
            context.getRecord().getScripts().add(record);
        }
        long startTime = System.currentTimeMillis();
        PipelineScriptRequest request = new PipelineScriptRequest();
        request.setScriptId(hook.getScriptId());
        request.setWorkspaceId(pipelineContext == null ? StrUtil.EMPTY : pipelineContext.getWorkspaceId());
        request.setWorkDir(pipelineContext == null ? null : pipelineContext.getWorkspaceDir());
        Map<String, String> payload = PipelineHookPayload.of(context);
        request.setEnv(payload);
        String prefix = "[" + PipelineHookMessages.get("i18n.pipeline_hook_script_prefix.8c13") + " " + eventCode + "] ";
        try {
            int exitCode = this.scriptRunner.run(request, line -> log.info(prefix + line), context.getProcessSink());
            record.setExitCode(exitCode);
            record.setDuration(System.currentTimeMillis() - startTime);
            record.setSuccess(exitCode == 0);
            if (exitCode != 0) {
                String message = StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_script_error.8c03"), hook.getScriptId(), exitCode);
                record.setMessage(message);
                log.error(message);
                return message;
            }
            return null;
        } catch (Exception e) {
            record.setDuration(System.currentTimeMillis() - startTime);
            record.setSuccess(false);
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            record.setMessage(message);
            log.error(StrUtil.format(PipelineHookMessages.get("i18n.pipeline_hook_script_exception.8c14"), hook.getScriptId(), message));
            return message;
        }
    }
}
