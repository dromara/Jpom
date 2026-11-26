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
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.dromara.jpom.pipeline.hook.PipelineEvent;
import org.dromara.jpom.pipeline.hook.PipelineHookDef;

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
     * 事件钩子（脚本 / webhook；存 {@code PIPELINE_INFO.hooksJson}，旧数据为空数组）
     */
    private List<PipelineHookDef> hooks = new ArrayList<>();

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
     * 解析事件钩子
     *
     * @param json JSON 文本（可为空）
     * @return 钩子列表（永不为 null）
     */
    public static List<PipelineHookDef> parseHooks(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        List<PipelineHookDef> list = JSON.parseArray(json, PipelineHookDef.class);
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
        return of(sourcesJson, stagesJson, null);
    }

    /**
     * 组装定义（含事件钩子）
     *
     * @param sourcesJson 源 JSON
     * @param stagesJson  阶段 JSON
     * @param hooksJson   钩子 JSON（可为空）
     * @return 定义
     */
    public static PipelineDefinition of(String sourcesJson, String stagesJson, String hooksJson) {
        PipelineDefinition definition = new PipelineDefinition();
        definition.setSources(parseSources(sourcesJson));
        definition.setStages(parseStages(stagesJson));
        definition.setHooks(parseHooks(hooksJson));
        return definition;
    }

    /**
     * 校验定义（保存前调用，错误信息直接可读）
     *
     * @throws IllegalArgumentException 校验失败
     */
    public void check() {
        Set<String> sourceIds = new HashSet<>();
        Set<String> mountDirs = new HashSet<>();
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
            // 挂载目录与源标识绑定：留空时按源标识补齐（新配置一律相等，旧配置的自定义值保留）
            if (StrUtil.isBlank(source.getMount())) {
                source.setMount(source.getId());
            }
            String mount = source.getMount();
            if (mount.contains("..") || StrUtil.startWith(mount, StrUtil.SLASH) || mount.contains("\\")) {
                throw new PipelineValidateException("i18n.pipeline_source_mount_invalid.7e0d", mount);
            }
            if (!mountDirs.add(mount)) {
                throw new PipelineValidateException("i18n.pipeline_source_mount_duplicated.6d21", mount);
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
            // 工作目录只允许工作区根或某个源目录（含其子目录），避免用户写出运行期不存在的路径
            if (stage.getType() == PipelineStageType.BUILD || stage.getType() == PipelineStageType.COMMAND) {
                Object cwdValue = stage.getConfig() == null ? null : stage.getConfig().get("cwd");
                String cwd = cwdValue == null ? "" : String.valueOf(cwdValue);
                if (StrUtil.isNotBlank(cwd) && !this.underMount(cwd, mountDirs)) {
                    throw new PipelineValidateException("i18n.pipeline_work_dir_invalid.9c4a", cwd);
                }
            }
        }
        if (stages.stream().noneMatch(stage -> stage.enabled() && stage.getType() == PipelineStageType.BUILD)) {
            throw new PipelineValidateException("i18n.pipeline_build_stage_required.3e7a", null);
        }
        this.checkHooks();
    }

    /**
     * 校验事件钩子（保存前调用）
     * <p>
     * 服务端校验为主：前端隐藏/禁用只是交互提示，直接调接口仍可绕过。事件码必须是 {@link PipelineEvent} 里存在的值，
     * 脚本钩子必须有 scriptId，webhook 必须有 http(s) 地址——否则运行期才发现问题太晚。
     * </p>
     */
    private void checkHooks() {
        if (CollUtil.isEmpty(hooks)) {
            return;
        }
        for (PipelineHookDef hook : hooks) {
            if (hook == null) {
                continue;
            }
            String type = StrUtil.nullToEmpty(hook.getType());
            if (!StrUtil.equalsAny(type, "script", "webhook")) {
                throw new PipelineValidateException("i18n.pipeline_hook_type_invalid.8c10", type);
            }
            if (CollUtil.isEmpty(hook.getEvents())) {
                throw new PipelineValidateException("i18n.pipeline_hook_events_required.8c07", null);
            }
            for (String code : hook.getEvents()) {
                if (!StrUtil.equalsIgnoreCase(code, PipelineEvent.ALL) && !PipelineEvent.exists(code)) {
                    throw new PipelineValidateException("i18n.pipeline_hook_events_invalid.8c11", code);
                }
            }
            if (StrUtil.equals(type, "script") && StrUtil.isBlank(hook.getScriptId())) {
                throw new PipelineValidateException("i18n.pipeline_hook_script_required.8c08", null);
            }
            if (StrUtil.equals(type, "webhook")) {
                String url = StrUtil.nullToEmpty(hook.getUrl());
                if (StrUtil.isBlank(url) || !ReUtil.isMatch("^https?://[^\\s]+$", url)) {
                    throw new PipelineValidateException("i18n.pipeline_hook_url_invalid.8c09", url);
                }
                String method = StrUtil.emptyToDefault(hook.getMethod(), "POST");
                if (!StrUtil.equalsAnyIgnoreCase(method, "GET", "POST")) {
                    throw new PipelineValidateException("i18n.pipeline_hook_method_invalid.8c18", method);
                }
            }
        }
    }

    /**
     * 工作目录是否落在某个源目录下
     *
     * @param cwd        工作目录（相对工作区）
     * @param mountDirs  源目录集合
     * @return true 表示合法
     */
    private boolean underMount(String cwd, Set<String> mountDirs) {
        String normalized = StrUtil.removeSuffix(cwd, StrUtil.SLASH);
        for (String mount : mountDirs) {
            if (StrUtil.equals(normalized, mount) || StrUtil.startWith(normalized, mount + StrUtil.SLASH)) {
                return true;
            }
        }
        return false;
    }
}