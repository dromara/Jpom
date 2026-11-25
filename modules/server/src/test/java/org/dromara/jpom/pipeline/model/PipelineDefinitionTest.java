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

import java.util.LinkedHashMap;
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
    public void blankMountDefaultsToSourceId() {
        PipelineSource source = source("backend", "repo-a");
        definition(new PipelineSource[]{source},
            new PipelineStageDef[]{buildStage("b1")}).check();
        Assertions.assertEquals("backend", source.getMount(), "挂载目录应跟随源标识，避免目录名不可预知");
    }

    @Test
    public void duplicatedMountRejected() {
        PipelineSource first = source("a", "repo-a");
        first.setMount("shared");
        PipelineSource second = source("b", "repo-b");
        second.setMount("shared");
        PipelineValidateException error = Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{first, second},
                new PipelineStageDef[]{buildStage("b1")}).check());
        Assertions.assertEquals("i18n.pipeline_source_mount_duplicated.6d21", error.getKey());
    }

    @Test
    public void workDirMustBeUnderSourceMount() {
        PipelineSource source = source("backend", "repo-a");
        PipelineStageDef stage = buildStage("b1");
        stage.setConfig(new LinkedHashMap<>());
        stage.getConfig().put("run", "mvn package");
        stage.getConfig().put("cwd", "backend/sub");
        // 源目录的子目录可以
        definition(new PipelineSource[]{source}, new PipelineStageDef[]{stage}).check();

        stage.getConfig().put("cwd", "outside");
        PipelineValidateException error = Assertions.assertThrows(PipelineValidateException.class, () ->
            definition(new PipelineSource[]{source}, new PipelineStageDef[]{stage}).check());
        Assertions.assertEquals("i18n.pipeline_work_dir_invalid.9c4a", error.getKey());
        Assertions.assertEquals("outside", error.getDetail());
    }

    @Test
    public void emptyJsonParsesToEmptyLists() {
        Assertions.assertTrue(PipelineDefinition.parseSources(null).isEmpty());
        Assertions.assertTrue(PipelineDefinition.parseStages("  ").isEmpty());
    }
}