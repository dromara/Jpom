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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流水线定义（JSON 真源）
 * <p>
 * 存 {@code PIPELINE_INFO.sourcesJson / stagesJson}（分开存便于单独改源与阶段，也便于后续按源加锁）。
 * 自带 {@code schemaVersion}：后续结构升级时按版本做兼容读取，旧数据不迁移。
 *
 * @author jpom
 * @since 2.11.16
 */
@Data
public class PipelineDefinition {

    /**
     * 当前结构版本
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private Integer schemaVersion = CURRENT_SCHEMA_VERSION;

    private List<PipelineSource> sources = new ArrayList<>();

    private List<PipelineStageDef> stages = new ArrayList<>();

    /**
     * 解析仓库源
     *
     * @param json JSON 文本（可为空）
     * @return 源列表（永不为 null）
     */
    public static List<PipelineSource> parseSources(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        List<PipelineSource> list = JSON.parseArray(json, PipelineSource.class);
        return list == null ? new ArrayList<>() : list;
    }

    /**
     * 解析阶段
     *
     * @param json JSON 文本（可为空）
     * @return 阶段列表（永不为 null）
     */
    public static List<PipelineStageDef> parseStages(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        List<PipelineStageDef> list = JSON.parseArray(json, PipelineStageDef.class);
        return list == null ? new ArrayList<>() : list;
    }

    /**
     * 组装定义
     *
     * @param sourcesJson 源 JSON
     * @param stagesJson  阶段 JSON
     * @return 定义
     */
    public static PipelineDefinition of(String sourcesJson, String stagesJson) {
        PipelineDefinition definition = new PipelineDefinition();
        definition.setSources(parseSources(sourcesJson));
        definition.setStages(parseStages(stagesJson));
        return definition;
    }

    /**
     * 校验定义（保存前调用，错误信息直接可读）
     *
     * @throws IllegalArgumentException 校验失败
     */
    public void check() {
        Set<String> sourceIds = new HashSet<>();
        for (PipelineSource source : sources) {
            if (StrUtil.isBlank(source.getId())) {
                throw new PipelineValidateException("i18n.pipeline_source_id_required.9a41", null);
            }
            if (!sourceIds.add(source.getId())) {
                throw new PipelineValidateException("i18n.pipeline_source_id_duplicated.2b73", source.getId());
            }
            if (StrUtil.isBlank(source.getRepoId())) {
                throw new PipelineValidateException("i18n.pipeline_source_repo_required.c5f2", source.getId());
            }
            if (StrUtil.isNotBlank(source.getMount()) && source.getMount().contains("..")) {
                throw new PipelineValidateException("i18n.pipeline_source_mount_invalid.7e0d", null);
            }
        }
        if (CollUtil.isEmpty(stages)) {
            throw new PipelineValidateException("i18n.pipeline_stage_required.4c18", null);
        }
        Set<String> stageIds = new HashSet<>();
        for (PipelineStageDef stage : stages) {
            if (StrUtil.isBlank(stage.getId())) {
                throw new PipelineValidateException("i18n.pipeline_stage_id_required.8f51", null);
            }
            if (!stageIds.add(stage.getId())) {
                throw new PipelineValidateException("i18n.pipeline_stage_id_duplicated.1d92", stage.getId());
            }
            if (stage.getType() == null) {
                throw new PipelineValidateException("i18n.pipeline_stage_type_required.6a3c", stage.getId());
            }
        }
        if (stages.stream().noneMatch(stage -> stage.enabled() && stage.getType() == PipelineStageType.BUILD)) {
            throw new PipelineValidateException("i18n.pipeline_build_stage_required.3e7a", null);
        }
    }
}