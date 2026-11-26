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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事件调度测试：事件过滤、禁用、未知方式、单条失败不影响其它、记录落表
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineHookDispatchTest {

    private static PipelineHookDef script(String scriptId, String... events) {
        PipelineHookDef def = new PipelineHookDef();
        def.setType("script");
        def.setScriptId(scriptId);
        def.setEvents(Arrays.asList(events));
        return def;
    }

    private static PipelineContext context(List<String> lines) {
        PipelineContext context = new PipelineContext();
        context.setPipelineId("p1");
        context.setPipelineName("流水线");
        context.setRunNumber(3);
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
    public void filtersByEventAndSkipsDisabled() {
        AtomicInteger calls = new AtomicInteger();
        PipelineHookDispatcher dispatcher = new PipelineHookDispatcher(Collections.singletonList(new PipelineHookHandler() {
            @Override
            public String type() {
                return "script";
            }

            @Override
            public String handle(PipelineHookDef hook, PipelineHookContext context) {
                calls.incrementAndGet();
                return null;
            }
        }));
        PipelineHookDef disabled = script("s2", "run_failed");
        disabled.setEnabled(false);
        List<PipelineHookDef> hooks = Arrays.asList(script("s1", "run_failed"), script("s3", "run_success"), disabled);
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.RUN_FAILED);
        hookContext.setPipelineContext(context(new ArrayList<>()));
        PipelineEventRecord record = dispatcher.fire(hooks, hookContext);
        Assertions.assertNotNull(record);
        Assertions.assertEquals(1, calls.get(), "只有匹配且启用的事件钩子执行");
        Assertions.assertEquals("run_failed", record.getEvent());
    }

    @Test
    public void allEventMatchesEverything() {
        AtomicInteger calls = new AtomicInteger();
        PipelineHookDispatcher dispatcher = new PipelineHookDispatcher(Collections.singletonList(new PipelineHookHandler() {
            @Override
            public String type() {
                return "script";
            }

            @Override
            public String handle(PipelineHookDef hook, PipelineHookContext context) {
                calls.incrementAndGet();
                return null;
            }
        }));
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.STAGE_SUCCESS);
        hookContext.setPipelineContext(context(new ArrayList<>()));
        dispatcher.fire(Collections.singletonList(script("s1", PipelineEvent.ALL)), hookContext);
        Assertions.assertEquals(1, calls.get());
    }

    @Test
    public void allMixedWithConcreteEventStillMatchesEverything() {
        PipelineHookDispatcher dispatcher = new PipelineHookDispatcher(Collections.singletonList(new PipelineHookHandler() {
            @Override
            public String type() {
                return "webhook";
            }

            @Override
            public String handle(PipelineHookDef hook, PipelineHookContext context) {
                return null;
            }
        }));
        PipelineHookDef hook = new PipelineHookDef();
        hook.setType("webhook");
        hook.setUrl("https://example.com/hook");
        // 历史/手工数据可能出现「all + 具体事件」混选：语义必须仍是全部命中，不能退化成只命中列出的那些
        hook.setEvents(Arrays.asList(PipelineEvent.ALL, "run_failed"));
        for (PipelineEvent event : PipelineEvent.values()) {
            PipelineHookContext hookContext = new PipelineHookContext();
            hookContext.setEvent(event);
            hookContext.setPipelineContext(context(new ArrayList<>()));
            Assertions.assertNotNull(dispatcher.fire(Collections.singletonList(hook), hookContext),
                "混选 all 时事件 " + event.getCode() + " 必须命中");
        }
    }

    @Test
    public void eventDescriptionsStartWithAll() {
        Map<String, String> descriptions = PipelineEvent.descriptions();
        String firstKey = descriptions.keySet().iterator().next();
        Assertions.assertEquals(PipelineEvent.ALL, firstKey, "「全部事件」必须排在事件清单第一位");
        Assertions.assertEquals(PipelineEvent.values().length + 1, descriptions.size());
    }

    @Test
    public void noMatchingHookReturnsNull() {
        PipelineHookDispatcher dispatcher = new PipelineHookDispatcher(Collections.emptyList());
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.RUN_START);
        hookContext.setPipelineContext(context(new ArrayList<>()));
        Assertions.assertNull(dispatcher.fire(Collections.singletonList(script("s1", "run_failed")), hookContext));
        Assertions.assertNull(dispatcher.fire(Collections.emptyList(), hookContext));
    }

    @Test
    public void unknownTypeIsLoggedAndOthersStillRun() {
        List<String> lines = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        PipelineHookDispatcher dispatcher = new PipelineHookDispatcher(Collections.singletonList(new PipelineHookHandler() {
            @Override
            public String type() {
                return "script";
            }

            @Override
            public String handle(PipelineHookDef hook, PipelineHookContext context) {
                calls.incrementAndGet();
                return "boom";
            }
        }));
        PipelineHookDef unknown = new PipelineHookDef();
        unknown.setType("email");
        unknown.setEvents(Collections.singletonList("run_failed"));
        PipelineHookDef failing = script("s1", "run_failed");
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.RUN_FAILED);
        hookContext.setPipelineContext(context(lines));
        PipelineEventRecord record = dispatcher.fire(Arrays.asList(unknown, failing), hookContext);
        Assertions.assertNotNull(record);
        Assertions.assertEquals(1, calls.get(), "未知方式不能阻止已注册方式执行");
        Assertions.assertTrue(lines.stream().anyMatch(line -> line.startsWith("E:")), "未知投递方式必须记错误日志");
        Assertions.assertFalse(record.allSuccess(), "失败的钩子要如实记录");
    }

    @Test
    public void failedHookIsCountedOnce() {
        PipelineHookDispatcher dispatcher = new PipelineHookDispatcher(Collections.singletonList(new PipelineHookHandler() {
            @Override
            public String type() {
                return "webhook";
            }

            @Override
            public String handle(PipelineHookDef hook, PipelineHookContext context) {
                // 实现层自己写失败记录（webhook 会写 url/attempts/message），返回值又非 null
                PipelineEventRecord.WebhookRecord record = new PipelineEventRecord.WebhookRecord();
                record.setUrl(hook.getUrl());
                record.setSuccess(false);
                record.setMessage("connection refused");
                context.getRecord().getWebhooks().add(record);
                return "connection refused";
            }
        }));
        PipelineHookDef hook = new PipelineHookDef();
        hook.setType("webhook");
        hook.setUrl("http://127.0.0.1:1/hook");
        hook.setEvents(Collections.singletonList("run_finished"));
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.RUN_FINISHED);
        hookContext.setPipelineContext(context(new ArrayList<>()));
        PipelineEventRecord record = dispatcher.fire(Collections.singletonList(hook), hookContext);
        Assertions.assertNotNull(record);
        Assertions.assertEquals(1, record.hookCount(), "一条失败钩子只能计一次（不能既算 webhook 又算 error）");
        Assertions.assertFalse(record.allSuccess());
    }

    @Test
    public void payloadContainsEventAndRunInfo() {
        PipelineContext pipelineContext = context(new ArrayList<>());
        pipelineContext.putVariable("PIPELINE_SOURCE_APP_DIR", "/tmp/app");
        PipelineHookContext hookContext = new PipelineHookContext();
        hookContext.setEvent(PipelineEvent.STAGE_FAILED);
        hookContext.setPipelineContext(pipelineContext);
        hookContext.setStageId("build");
        hookContext.setStageName("构建");
        hookContext.setStatus(3);
        Map<String, String> payload = PipelineHookPayload.of(hookContext);
        Assertions.assertEquals("stage_failed", payload.get("PIPELINE_EVENT"));
        Assertions.assertEquals("stage_failed", payload.get("type"));
        Assertions.assertEquals("p1", payload.get("pipelineId"));
        Assertions.assertEquals("3", payload.get("runNumber"));
        Assertions.assertEquals("build", payload.get("stageId"));
        Assertions.assertEquals("/tmp/app", payload.get("PIPELINE_SOURCE_APP_DIR"));
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("event", "stage_failed");
        Assertions.assertEquals(expected.get("event"), payload.get("event"));
    }
}
