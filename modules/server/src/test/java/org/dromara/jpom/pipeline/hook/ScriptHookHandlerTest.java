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

import org.dromara.jpom.pipeline.engine.PipelineContext;
import org.dromara.jpom.pipeline.engine.PipelineLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 事件脚本钩子测试：输出实时进日志、非 0 退出码判失败、异常不吞
 *
 * @author jpom
 * @since 2.11.16
 */
public class ScriptHookHandlerTest {

    private static PipelineHookDef hook(String scriptId) {
        PipelineHookDef def = new PipelineHookDef();
        def.setType("script");
        def.setScriptId(scriptId);
        def.setEvents(Arrays.asList("run_failed"));
        return def;
    }

    private static PipelineContext context(List<String> lines) {
        PipelineContext context = new PipelineContext();
        context.setPipelineId("p1");
        context.setRunNumber(1);
        context.setLog(new PipelineLog() {
            @Override
            public void info(String message) {
                lines.add(message);
            }

            @Override
            public void error(String message) {
                lines.add("E:" + message);
            }
        });
        return context;
    }

    @Test
    public void scriptOutputGoesToRunLogLineByLine() {
        List<String> lines = new ArrayList<>();
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.RUN_FAILED);
        hookContext.setPipelineContext(context(lines));
        hookContext.setRecord(new PipelineEventRecord());
        ScriptHookHandler handler = new ScriptHookHandler((request, lineSink, processSink) -> {
            lineSink.accept("line-1");
            lineSink.accept("line-2");
            return 0;
        }, null);
        String error = handler.handle(hook("s1"), hookContext);
        Assertions.assertNull(error);
        Assertions.assertTrue(lines.stream().anyMatch(line -> line.contains("line-1")), "脚本输出必须写进运行日志");
        Assertions.assertTrue(lines.stream().allMatch(line -> !line.startsWith("E:")), "成功时不应有错误日志");
        Assertions.assertEquals(1, hookContext.getRecord().getScripts().size());
        Assertions.assertTrue(hookContext.getRecord().getScripts().get(0).isSuccess());
    }

    @Test
    public void nonZeroExitIsFailureWithMessage() {
        List<String> lines = new ArrayList<>();
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.RUN_FAILED);
        hookContext.setPipelineContext(context(lines));
        hookContext.setRecord(new PipelineEventRecord());
        ScriptHookHandler handler = new ScriptHookHandler((request, lineSink, processSink) -> 3, null);
        String error = handler.handle(hook("s1"), hookContext);
        Assertions.assertNotNull(error);
        Assertions.assertTrue(error.contains("3"), error);
        Assertions.assertTrue(lines.stream().anyMatch(line -> line.startsWith("E:")), "失败原因要写进日志");
        Assertions.assertFalse(hookContext.getRecord().getScripts().get(0).isSuccess());
    }

    @Test
    public void exceptionIsReportedNotSwallowed() {
        List<String> lines = new ArrayList<>();
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.STAGE_FAILED);
        hookContext.setPipelineContext(context(lines));
        hookContext.setRecord(new PipelineEventRecord());
        ScriptHookHandler handler = new ScriptHookHandler((request, lineSink, processSink) -> {
            throw new IllegalStateException("script missing");
        }, null);
        String error = handler.handle(hook("s1"), hookContext);
        Assertions.assertEquals("script missing", error);
        Assertions.assertTrue(lines.stream().anyMatch(line -> line.startsWith("E:")));
    }
}
