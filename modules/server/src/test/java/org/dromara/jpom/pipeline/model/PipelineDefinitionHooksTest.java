/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.pipeline.model;

import org.dromara.jpom.pipeline.hook.PipelineEvent;
import org.dromara.jpom.pipeline.hook.PipelineHookDef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * 事件钩子配置校验测试：保存前拦住非法配置，旧数据（无 hooksJson）按空数组处理
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineDefinitionHooksTest {

    private PipelineDefinition definition(PipelineHookDef hook) {
        PipelineDefinition definition = new PipelineDefinition();
        PipelineSource source = new PipelineSource();
        source.setId("app");
        source.setRepoId("repo1");
        definition.setSources(new ArrayList<>(Collections.singletonList(source)));
        PipelineStageDef stage = new PipelineStageDef();
        stage.setId("build");
        stage.setName("构建");
        stage.setType(PipelineStageType.BUILD);
        stage.getConfig().put("run", "echo hi");
        definition.setStages(new ArrayList<>(Collections.singletonList(stage)));
        definition.setHooks(new ArrayList<>(Collections.singletonList(hook)));
        return definition;
    }

    private PipelineHookDef scriptHook() {
        PipelineHookDef hook = new PipelineHookDef();
        hook.setType("script");
        hook.setScriptId("s1");
        hook.setEvents(new ArrayList<>(Collections.singletonList("run_failed")));
        return hook;
    }

    @Test
    public void oldDefinitionWithoutHooksIsValid() {
        PipelineDefinition definition = definition(scriptHook());
        definition.setHooks(new ArrayList<>());
        definition.check();
        Assertions.assertTrue(definition.getHooks().isEmpty());
        Assertions.assertTrue(PipelineDefinition.parseHooks(null).isEmpty());
        Assertions.assertTrue(PipelineDefinition.parseHooks("").isEmpty());
    }

    @Test
    public void validHooksPass() {
        definition(scriptHook()).check();
        PipelineHookDef webhook = new PipelineHookDef();
        webhook.setType("webhook");
        webhook.setUrl("https://example.com/hook");
        webhook.setEvents(Arrays.asList("run_success", PipelineEvent.ALL));
        definition(webhook).check();
    }

    @Test
    public void scriptHookRequiresScriptId() {
        PipelineHookDef hook = scriptHook();
        hook.setScriptId("");
        Assertions.assertThrows(PipelineValidateException.class, () -> definition(hook).check());
    }

    @Test
    public void hookRequiresKnownEvent() {
        PipelineHookDef hook = scriptHook();
        hook.setEvents(new ArrayList<>(Collections.singletonList("not_a_event")));
        Assertions.assertThrows(PipelineValidateException.class, () -> definition(hook).check());
        PipelineHookDef empty = scriptHook();
        empty.setEvents(new ArrayList<>());
        Assertions.assertThrows(PipelineValidateException.class, () -> definition(empty).check());
    }

    @Test
    public void webhookRequiresHttpUrlAndKnownMethod() {
        PipelineHookDef hook = new PipelineHookDef();
        hook.setType("webhook");
        hook.setEvents(new ArrayList<>(Collections.singletonList("run_failed")));
        hook.setUrl("ftp://example.com/hook");
        Assertions.assertThrows(PipelineValidateException.class, () -> definition(hook).check());
        hook.setUrl("https://example.com/hook");
        hook.setMethod("PUT");
        Assertions.assertThrows(PipelineValidateException.class, () -> definition(hook).check());
        hook.setMethod("GET");
        definition(hook).check();
    }

    @Test
    public void unknownHookTypeRejected() {
        PipelineHookDef hook = scriptHook();
        hook.setType("email");
        Assertions.assertThrows(PipelineValidateException.class, () -> definition(hook).check());
    }
}
