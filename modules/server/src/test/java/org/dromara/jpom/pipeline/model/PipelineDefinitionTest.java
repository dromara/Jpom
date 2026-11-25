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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 流水线定义校验测试
 * <p>
 * 校验放在服务端（前端禁用不算校验）：这些用例就是「脏配置进不来」的证据。
 * </p>
 *
 * @author jpom
 * @since 2.11.16
 */
public class PipelineDefinitionTest {

    private PipelineSource source(String id, String repoId) {
        PipelineSource source = new PipelineSource();
        source.setId(id);
        source.setRepoId(repoId);
        return source;
    }

    private PipelineStageDef buildStage(String id) {
        PipelineStageDef stage = new PipelineStageDef();
        stage.setId(id);
        stage.setName("build");
        stage.setType(PipelineStageType.BUILD);
        return stage;
    }

    private PipelineDefinition definition(PipelineSource[] sources, PipelineStageDef[] stages) {
        PipelineDefinition definition = new PipelineDefinition();
        definition.setSources(new ArrayList<>(Arrays.asList(sources)));
        definition.setStages(new ArrayList<>(Arrays.asList(stages)));
        return definition;
    }

    @Test
    public void validDefinitionPasses() {
        definition(new PipelineSource[]{source("srv", "repo-a")},
            new PipelineStageDef[]{buildStage("b1")}).check();
    }

    @Test
    public void duplicatedSourceIdRejected() {
        PipelineValidateException error = Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source("srv", "repo-a"), source("srv", "repo-b")},
                new PipelineStageDef[]{buildStage("b1")}).check());
        Assertions.assertEquals("i18n.pipeline_source_id_duplicated.2b73", error.getKey());
        Assertions.assertEquals("srv", error.getDetail());
    }

    @Test
    public void sourceWithoutRepositoryRejected() {
        Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source("srv", null)},
                new PipelineStageDef[]{buildStage("b1")}).check());
    }

    @Test
    public void mountPathTraversalRejected() {
        PipelineSource source = source("srv", "repo-a");
        source.setMount("../outside");
        Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source},
                new PipelineStageDef[]{buildStage("b1")}).check());
    }

    @Test
    public void definitionWithoutEnabledBuildStageRejected() {
        PipelineStageDef disabled = buildStage("b1");
        disabled.setEnabled(false);
        Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source("srv", "repo-a")},
                new PipelineStageDef[]{disabled}).check());
        Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source("srv", "repo-a")},
                new PipelineStageDef[]{}).check());
    }

    @Test
    public void duplicatedStageIdRejected() {
        Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source("srv", "repo-a")},
                new PipelineStageDef[]{buildStage("b1"), buildStage("b1")}).check());
    }

    @Test
    public void stageWithoutTypeRejected() {
        PipelineStageDef stage = buildStage("b1");
        stage.setType(null);
        Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source("srv", "repo-a")},
                new PipelineStageDef[]{stage}).check());
    }

    @Test
    public void emptyJsonParsesToEmptyLists() {
        Assertions.assertTrue(PipelineDefinition.parseSources(null).isEmpty());
        Assertions.assertTrue(PipelineDefinition.parseStages("  ").isEmpty());
    }
}