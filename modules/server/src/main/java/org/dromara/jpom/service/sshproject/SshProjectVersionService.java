/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.sshproject;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.SshProjectModel;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.sshproject.SshProjectExecuteResult;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SSH 项目版本快照（在目标机器上按目录快照，用于「快速回滚」）
 * <p>
 * 与插件端项目的版本快照是同一套语义，落地方式不同：SSH 项目没有插件端，
 * 版本目录由服务端通过 SSH 在目标机器上维护（{@code $HOME/.jpom/versions/<项目ID>}）：
 * <ul>
 *     <li>{@code <versionId>/}：项目目录内容快照（忽略顶层 logs/temp 与 *.tmp 临时项）；</li>
 *     <li>{@code <versionId>.json}：元数据（来源、构建编号、文件数/大小/内容指纹）；</li>
 *     <li>{@code current.txt}：当前运行版本。</li>
 * </ul>
 * 切换版本＝先给当前目录做安全快照，再把版本目录内容覆盖回项目目录，并用内容指纹校验；
 * 校验失败回滚到安全快照，避免把项目目录留成半新半旧。
 *
 * @author bwcx_jzy
 * @since 2026/11/20
 */
@Slf4j
@Service
public class SshProjectVersionService {

    /**
     * 版本来源：首次开启时的基线
     */
    public static final String SOURCE_BASELINE = "baseline";
    /**
     * 版本来源：构建发布
     */
    public static final String SOURCE_BUILD = "build";
    /**
     * 版本来源：切换前的自动安全快照
     */
    public static final String SOURCE_AUTO = "auto";
    /**
     * 版本来源：人工创建
     */
    public static final String SOURCE_MANUAL = "manual";
    /**
     * 版本快照默认保留个数（与构建侧口径一致：0/空按 3）
     */
    public static final int DEFAULT_KEEP_COUNT = 3;
    /**
     * 版本目录相对用户目录的位置
     */
    private static final String VERSION_HOME = "/.jpom/versions/";
    /**
     * 版本号白名单：写进 shell 前必须校验，避免路径穿越与命令注入
     */
    private static final Pattern VERSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");
    private static final String OK_FLAG = "JPOM_OK";
    private static final String ERROR_FLAG = "JPOM_ERR";
    private static final String VERSION_ROOT_DECL = "VER_ROOT=\"$HOME" + VERSION_HOME + "$PROJECT_ID\"";

    /**
     * 远端公共函数：解析项目目录、统计文件、生成内容指纹、复制/清理目录内容
     */
    private static final String PREAMBLE = String.join("\n",
        "set -u",
        VERSION_ROOT_DECL,
        "resolve_dir() {",
        "  p=\"$1\"",
        "  case \"$p\" in /*) ;; *) p=\"$HOME/$p\" ;; esac",
        "  ( cd \"$p\" 2>/dev/null && pwd )",
        "}",
        "PROJECT_DIR=\"$(resolve_dir \"$PROJECT_PATH\")\"",
        "if [ -z \"$PROJECT_DIR\" ]; then echo \"" + ERROR_FLAG + "|project dir not found: $PROJECT_PATH\"; exit 21; fi",
        "ver_manifest() {",
        "  ( cd \"$1\" 2>/dev/null || exit 0; find . -mindepth 1 -type f -not -path './logs/*' -not -path './temp/*' -not -name '*.tmp*' | sed 's|^\\./||' | LC_ALL=C sort | while IFS= read -r f; do printf '%s|%s|%s\\n' \"$f\" \"$(stat -c %s \"$f\" 2>/dev/null)\" \"$(sha1sum \"$f\" 2>/dev/null | cut -d' ' -f1)\"; done ) | sha1sum | cut -d' ' -f1",
        "}",
        "ver_count() { ( cd \"$1\" 2>/dev/null || exit 0; find . -mindepth 1 -type f -not -path './logs/*' -not -path './temp/*' -not -name '*.tmp*' | wc -l ) | tr -d ' '; }",
        "ver_size() { du -sk \"$1\" 2>/dev/null | awk '{print $1*1024}'; }",
        "copy_content() {",
        "  mkdir -p \"$2\"",
        "  ( cd \"$1\" 2>/dev/null || exit 0; find . -mindepth 1 -maxdepth 1 -not -name logs -not -name temp -not -name '*.tmp*' -print | while IFS= read -r e; do if ! cp -a \"$e\" \"$2/\" 2>/dev/null; then cp -aR \"$e\" \"$2/\" || exit 23; fi; done ) || return 23",
        "}",
        "clear_content() {",
        "  mkdir -p \"$1\"",
        "  ( cd \"$1\" 2>/dev/null || exit 0; find . -mindepth 1 -maxdepth 1 -not -name logs -not -name temp -not -name '*.tmp*' -print | while IFS= read -r e; do rm -rf \"$e\" || exit 24; done ) || return 24",
        "}"
    );

    private final SshProjectExecutor sshProjectExecutor;
    private final SshProjectService sshProjectService;

    public SshProjectVersionService(SshProjectExecutor sshProjectExecutor,
                                    SshProjectService sshProjectService) {
        this.sshProjectExecutor = sshProjectExecutor;
        this.sshProjectService = sshProjectService;
    }

    /**
     * 归一版本保留个数：0 / 空按默认值
     *
     * @param keepCount 保留个数
     * @return int
     */
    public static int resolveKeepCount(Integer keepCount) {
        return (keepCount == null || keepCount <= 0) ? DEFAULT_KEEP_COUNT : keepCount;
    }

    /**
     * 能力探测（前端据此决定「版本」入口的可用性）
     *
     * @param project 项目
     * @return json
     */
    public JSONObject capability(SshProjectModel project) {
        JSONObject result = new JSONObject();
        result.put("support", true);
        result.put("enabled", true);
        result.put("versionCount", DEFAULT_KEEP_COUNT);
        try {
            JSONObject list = this.list(project);
            result.put("path", list.getString("path"));
            result.put("current", list.getString("current"));
            result.put("hasVersions", list.getBoolean("hasVersions"));
            result.put("count", this.metas(list).size());
        } catch (Exception e) {
            result.put("reachable", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 版本列表
     *
     * @param project 项目
     * @return json
     */
    public JSONObject list(SshProjectModel project) {
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, this.scriptList(), 60);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.ssh_project_version_list_fail.3f21") + StrUtil.nullToEmpty(result.getMessage()));
        return this.parseList(result.getOutput());
    }

    /**
     * 发布前准备：首次开启时把当前项目目录登记为基线版本，避免第一次回滚无版本可用
     *
     * @param project       项目
     * @param buildNumberId 构建编号
     * @param keepCount     保留个数
     * @return json
     */
    public JSONObject begin(SshProjectModel project, Integer buildNumberId, Integer keepCount) {
        int versionCount = resolveKeepCount(keepCount);
        JSONObject result = new JSONObject();
        result.put("enabled", true);
        result.put("versionCount", versionCount);
        JSONObject list = this.list(project);
        if (this.metas(list).isEmpty() && list.getIntValue("projectFiles") > 0) {
            JSONObject baseline = this.snapshot(project, "baseline-" + this.now(), SOURCE_BASELINE, buildNumberId, null);
            result.put("baseline", baseline);
            log.debug("ssh project version baseline created: {} {}", project.getId(), baseline.getString("versionId"));
        }
        this.clearOldVersion(project, list, null, versionCount);
        result.put("current", list.getString("current"));
        result.put("count", this.metas(list).size());
        return result;
    }

    /**
     * 登记一个版本快照（构建发布成功 / 人工创建）
     *
     * @param project       项目
     * @param versionId     版本号（为空按时间生成）
     * @param source        来源
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置ID
     * @param keepCount     保留个数
     * @return json
     */
    public JSONObject commit(SshProjectModel project, String versionId, String source, Integer buildNumberId,
                             String buildDataId, Integer keepCount) {
        int versionCount = resolveKeepCount(keepCount);
        JSONObject list = this.list(project);
        Assert.state(list.getIntValue("projectFiles") > 0,
            I18nMessageUtil.get("i18n.ssh_project_version_project_empty.6c12"));
        String id = StrUtil.emptyToDefault(versionId, "manual-" + this.now());
        // 同编号已存在（重复发布/重试）时加时间后缀，避免覆盖历史版本
        if (this.findMeta(list, id) != null) {
            id = id + "-" + this.now();
        }
        JSONObject meta = this.snapshot(project, id, source, buildNumberId, buildDataId);
        this.writeCurrent(project, id);
        this.clearOldVersion(project, list, id, versionCount);
        JSONObject result = new JSONObject();
        result.put("enabled", true);
        result.put("versionId", meta.getString("versionId"));
        result.put("manifest", meta.getString("manifest"));
        result.put("fileSize", meta.getLong("fileSize"));
        result.put("fileCount", meta.getInteger("fileCount"));
        result.put("createTime", meta.getLong("createTime"));
        result.put("versionCount", versionCount);
        result.put("count", this.metas(list).size() + 1);
        return result;
    }

    /**
     * 切换版本（快速回滚）
     *
     * @param project         项目
     * @param versionId       目标版本
     * @param afterOpt        切换后操作（大于 0 重启）
     * @param snapshotCurrent 切换前是否把当前目录快照为安全版本
     * @param keepCount       保留个数
     * @return json
     */
    public JSONObject switchVersion(SshProjectModel project, String versionId, Integer afterOpt, Boolean snapshotCurrent, Integer keepCount) {
        this.assertVersionId(versionId);
        JSONObject list = this.list(project);
        JSONObject target = this.findMeta(list, versionId);
        Assert.notNull(target, I18nMessageUtil.get("i18n.version_not_exists.5d90") + versionId);
        Assert.state(Boolean.TRUE.equals(target.getBoolean("exist")),
            I18nMessageUtil.get("i18n.version_not_exists.5d90") + versionId);
        String previousVersionId = list.getString("current");
        JSONObject safety = null;
        if (!Boolean.FALSE.equals(snapshotCurrent) && list.getIntValue("projectFiles") > 0) {
            safety = this.snapshot(project, "auto-" + this.now(), SOURCE_AUTO, null, null);
        }
        int after = afterOpt == null ? 0 : afterOpt;
        JSONObject result = new JSONObject();
        try {
            this.restore(project, versionId, target.getString("manifest"));
            boolean restarted = false;
            if (after > 0) {
                SshProjectExecuteResult restartResult = this.restart(project);
                restarted = restartResult.isSuccess();
                result.put("restartMessage", StrUtil.nullToEmpty(restartResult.getMessage()));
                if (!restarted) {
                    log.warn("ssh project restart after version switch failed: {} {}", project.getId(), restartResult.getMessage());
                }
            }
            result.put("restarted", restarted);
        } catch (Exception e) {
            this.restoreSafety(project, safety);
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.version_switch_failed.9c31") + e.getMessage(), e);
        }
        this.writeCurrent(project, versionId);
        this.clearOldVersion(project, list, versionId, resolveKeepCount(keepCount));
        result.put("versionId", versionId);
        result.put("previousVersionId", previousVersionId);
        result.put("safetyVersionId", safety == null ? null : safety.getString("versionId"));
        return result;
    }

    /**
     * 删除版本（当前运行版本不允许删除）
     *
     * @param project   项目
     * @param versionId 版本ID
     */
    public void deleteVersion(SshProjectModel project, String versionId) {
        this.assertVersionId(versionId);
        JSONObject list = this.list(project);
        Assert.state(!StrUtil.equals(versionId, list.getString("current")),
            I18nMessageUtil.get("i18n.version_current_delete_forbidden.2e84"));
        Assert.notNull(this.findMeta(list, versionId), I18nMessageUtil.get("i18n.version_not_exists.5d90") + versionId);
        this.removeVersion(project, versionId);
    }

    /**
     * 构建历史 → 该构建在 SSH 项目上产生的版本（快速回滚弹窗用）
     *
     * @param buildHistoryLog 构建历史
     * @return json
     */
    public JSONObject listByBuildLog(BuildHistoryLog buildHistoryLog) {
        Integer buildNumberId = buildHistoryLog.getFromBuildNumberId() == null
            ? buildHistoryLog.getBuildNumberId() : buildHistoryLog.getFromBuildNumberId();
        JSONObject result = new JSONObject();
        result.put("buildNumberId", buildNumberId);
        result.put("targetType", "sshProject");
        JSONArray array = new JSONArray();
        result.put("list", array);
        List<SshProjectModel> projects = this.resolveProjects(buildHistoryLog);
        if (projects.isEmpty()) {
            result.put("reachable", false);
            result.put("message", I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
            return result;
        }
        JSONArray projectArray = new JSONArray();
        for (SshProjectModel project : projects) {
            JSONObject projectItem = new JSONObject();
            projectItem.put("projectId", project.getId());
            projectItem.put("projectName", project.getName());
            projectArray.add(projectItem);
        }
        result.put("projects", projectArray);
        if (projects.size() == 1) {
            // 单选是老口径，保留 projectId/projectName 方便旧前端与调用方
            result.put("projectId", projects.get(0).getId());
            result.put("projectName", projects.get(0).getName());
        }
        boolean reachable = false;
        List<String> errors = new ArrayList<>();
        for (SshProjectModel project : projects) {
            try {
                JSONObject list = this.list(project);
                reachable = true;
                this.appendBuildVersions(array, project, list, buildNumberId, buildHistoryLog.getBuildDataId());
            } catch (Exception e) {
                log.warn("ssh project version list error: {} {}", project.getId(), e.getMessage());
                errors.add(project.getName() + ": " + e.getMessage());
            }
        }
        result.put("reachable", reachable);
        // 多选发布时目标可能已被删除：界面要能看见「少了哪个」，而不是悄悄少一行
        List<String> missingIds = new ArrayList<>(this.releaseTargetIds(buildHistoryLog));
        projects.forEach(project -> missingIds.remove(project.getId()));
        if (!missingIds.isEmpty()) {
            errors.add(I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18") + String.join(",", missingIds));
        }
        if (!errors.isEmpty()) {
            result.put("message", String.join("; ", errors));
        }
        return result;
    }

    /**
     * 把一个 SSH 项目在该次构建产生的版本追加到列表
     *
     * @param array         结果列表
     * @param project       项目
     * @param list          远端版本列表
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置 id
     */
    private void appendBuildVersions(JSONArray array, SshProjectModel project, JSONObject list,
                                     Integer buildNumberId, String buildDataId) {
        String current = list.getString("current");
        for (JSONObject meta : this.metas(list)) {
            if (!SOURCE_BUILD.equals(meta.getString("source"))) {
                // 只有「构建发布」产生的版本属于某次构建；基线/安全快照不参与构建级回滚
                continue;
            }
            Integer metaBuildNumberId = meta.getInteger("buildNumberId");
            if (buildNumberId != null && !buildNumberId.equals(metaBuildNumberId)) {
                continue;
            }
            // 不同构建配置的构建编号会重复（各自从 1 开始），必须同时按构建配置过滤，
            // 否则 A 构建的弹窗会列出 B 构建同编号的版本（切换时切错内容）
            String metaBuildDataId = meta.getString("buildDataId");
            if (StrUtil.isNotBlank(metaBuildDataId) && !StrUtil.equals(metaBuildDataId, buildDataId)) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("projectId", project.getId());
            item.put("projectName", project.getName());
            item.put("targetType", "sshProject");
            item.put("versionId", meta.getString("versionId"));
            item.put("source", meta.getString("source"));
            item.put("buildNumberId", metaBuildNumberId);
            item.put("createTime", meta.getLong("createTime"));
            item.put("fileSize", meta.getLong("fileSize"));
            item.put("fileCount", meta.getInteger("fileCount"));
            item.put("exist", meta.getBoolean("exist"));
            item.put("reachable", true);
            item.put("status", StrUtil.equals(meta.getString("versionId"), current) ? 2 : 1);
            array.add(item);
        }
    }

    /**
     * 构建历史快速回滚：把 SSH 项目切回该构建发布的版本
     *
     * @param buildHistoryLog 构建历史
     * @param versionId       指定版本（为空按构建编号自动定位）
     * @param restart         切换后是否重启
     * @param logRecorder     构建日志（可空）
     * @return true 切换成功
     */
    public boolean quickRollback(BuildHistoryLog buildHistoryLog, String projectId, String versionId, boolean restart, LogRecorder logRecorder) {
        // 只允许回滚本次构建配置里选中的项目，projectId 由前端按版本行传入（多选时用于定位）
        SshProjectModel project = this.resolveProject(buildHistoryLog, projectId);
        Assert.notNull(project, I18nMessageUtil.get("i18n.ssh_project_not_exist.5d18"));
        Integer buildNumberId = buildHistoryLog.getFromBuildNumberId() == null
            ? buildHistoryLog.getBuildNumberId() : buildHistoryLog.getFromBuildNumberId();
        String target = StrUtil.trimToNull(versionId);
        if (target == null) {
            for (JSONObject meta : this.metas(this.list(project))) {
                if (!SOURCE_BUILD.equals(meta.getString("source"))) {
                    continue;
                }
                if (buildNumberId == null || !buildNumberId.equals(meta.getInteger("buildNumberId"))) {
                    continue;
                }
                String metaBuildDataId = meta.getString("buildDataId");
                if (StrUtil.isNotBlank(metaBuildDataId) && !StrUtil.equals(metaBuildDataId, buildHistoryLog.getBuildDataId())) {
                    continue;
                }
                target = meta.getString("versionId");
                break;
            }
        }
        Assert.hasText(target, I18nMessageUtil.get("i18n.no_node_version_for_build.3d18"));
        JSONObject switchResult = this.switchVersion(project, target, restart ? 1 : 0, true, DEFAULT_KEEP_COUNT);
        if (logRecorder != null) {
            logRecorder.system(I18nMessageUtil.get("i18n.ssh_project_version_rollback_success.5b73"),
                project.getName(), switchResult.getString("versionId"),
                switchResult.getBooleanValue("restarted") ? I18nMessageUtil.get("i18n.ssh_project_version_restarted.9e41") : StrUtil.EMPTY);
        }
        return true;
    }

    /**
     * 解析构建历史对应的 SSH 项目（发布方式 =「SSH 项目」支持多选，按逗号拆分）
     *
     * @param buildHistoryLog 构建历史
     * @return 项目列表（不会为空集合）
     */
    public List<SshProjectModel> resolveProjects(BuildHistoryLog buildHistoryLog) {
        BuildExtraModule extraModule = StringUtil.jsonConvert(buildHistoryLog.getExtraData(), BuildExtraModule.class);
        List<SshProjectModel> projects = new ArrayList<>();
        if (extraModule == null) {
            return projects;
        }
        String releaseMethodDataId = StrUtil.trimToNull(extraModule.getReleaseMethodDataId());
        if (releaseMethodDataId == null) {
            return projects;
        }
        for (String id : StrUtil.splitTrim(releaseMethodDataId, StrUtil.COMMA)) {
            SshProjectModel project = sshProjectService.getByKey(id);
            if (project != null) {
                projects.add(project);
            }
        }
        return projects;
    }

    /**
     * 本次构建选中的目标 id（多选时为逗号分隔列表）
     *
     * @param buildHistoryLog 构建历史
     * @return 目标 id（不会为空集合）
     */
    private List<String> releaseTargetIds(BuildHistoryLog buildHistoryLog) {
        BuildExtraModule extraModule = StringUtil.jsonConvert(buildHistoryLog.getExtraData(), BuildExtraModule.class);
        if (extraModule == null) {
            return new ArrayList<>();
        }
        String releaseMethodDataId = StrUtil.trimToNull(extraModule.getReleaseMethodDataId());
        return releaseMethodDataId == null ? new ArrayList<>() : StrUtil.splitTrim(releaseMethodDataId, StrUtil.COMMA);
    }

    /**
     * 解析本次构建对应的单个 SSH 项目
     *
     * @param buildHistoryLog 构建历史
     * @param projectId       指定项目（为空且只有一个项目时自动定位）
     * @return 项目（可为空）
     */
    SshProjectModel resolveProject(BuildHistoryLog buildHistoryLog, String projectId) {
        List<SshProjectModel> projects = this.resolveProjects(buildHistoryLog);
        if (StrUtil.isNotBlank(projectId)) {
            for (SshProjectModel project : projects) {
                if (StrUtil.equals(project.getId(), projectId)) {
                    return project;
                }
            }
            return null;
        }
        return projects.size() == 1 ? projects.get(0) : null;
    }

    /**
     * 重启项目（没有生命周期脚本时视为无需重启）
     *
     * @param project 项目
     * @return 执行结果
     */
    private SshProjectExecuteResult restart(SshProjectModel project) {
        SshProjectExecuteResult result = new SshProjectExecuteResult();
        DslYmlDto dsl = DslYmlDto.build(project.getDslContent());
        boolean hasRestart = dsl.tryDslProcess(ConsoleCommandOp.restart.name()) != null
            || dsl.tryDslProcess(ConsoleCommandOp.start.name()) != null;
        if (!hasRestart) {
            result.setSuccess(true);
            result.setExitCode(0);
            result.setMessage(I18nMessageUtil.get("i18n.ssh_project_version_no_script.7d35"));
            return result;
        }
        try {
            return sshProjectService.execute(project, ConsoleCommandOp.restart.name(), null);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setExitCode(-100);
            result.setMessage(e.getMessage());
            return result;
        }
    }

    /**
     * 生成一个完整版本快照
     *
     * @param project       项目
     * @param versionId     版本号
     * @param source        来源
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置ID
     * @return 元数据
     */
    private JSONObject snapshot(SshProjectModel project, String versionId, String source, Integer buildNumberId, String buildDataId) {
        long createTime = System.currentTimeMillis();
        String script = this.scriptSnapshot(versionId, source, buildNumberId, buildDataId, createTime);
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, script, 300);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.version_snapshot_failed.7b2c") + StrUtil.nullToEmpty(result.getMessage()));
        JSONObject meta = this.parseKeyValue(result.getOutput());
        Assert.state(StrUtil.isNotBlank(meta.getString("versionId")),
            I18nMessageUtil.get("i18n.version_snapshot_failed.7b2c") + StrUtil.maxLength(StrUtil.nullToEmpty(result.getOutput()), 200));
        // 脚本回传都是字符串，这里按数字口径归一，避免前端收到 "12288" 这类字符串
        meta.put("fileCount", Convert.toInt(meta.getString("fileCount"), 0));
        meta.put("fileSize", Convert.toLong(meta.getString("fileSize"), 0L));
        meta.put("source", source);
        meta.put("buildNumberId", buildNumberId == null ? 0 : buildNumberId);
        meta.put("buildDataId", StrUtil.nullToEmpty(buildDataId));
        meta.put("createTime", createTime);
        return meta;
    }

    /**
     * 把版本目录内容覆盖回项目目录，并用内容指纹校验
     *
     * @param project        项目
     * @param versionId      版本ID
     * @param expectManifest 期望内容指纹（可为空）
     */
    private void restore(SshProjectModel project, String versionId, String expectManifest) {
        this.assertVersionId(versionId);
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, this.scriptRestore(versionId), 300);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.version_switch_failed.9c31") + StrUtil.nullToEmpty(result.getMessage()));
        if (StrUtil.isNotBlank(expectManifest)) {
            JSONObject values = this.parseKeyValue(result.getOutput());
            String manifest = values.getString("MANIFEST");
            Assert.state(StrUtil.equals(expectManifest, manifest),
                I18nMessageUtil.get("i18n.version_verify_failed.d3a9") + versionId);
        }
    }

    /**
     * 切换失败时恢复安全版本
     *
     * @param project 项目
     * @param safety  安全版本元数据
     */
    private void restoreSafety(SshProjectModel project, JSONObject safety) {
        if (safety == null) {
            return;
        }
        try {
            this.restore(project, safety.getString("versionId"), safety.getString("manifest"));
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.version_switch_rollback_failed.4a7e"), e);
        }
    }

    /**
     * 写当前版本标记
     *
     * @param project   项目
     * @param versionId 版本ID
     */
    private void writeCurrent(SshProjectModel project, String versionId) {
        if (StrUtil.isNotBlank(versionId)) {
            this.assertVersionId(versionId);
        }
        String script = String.join("\n",
            "set -u",
            VERSION_ROOT_DECL,
            "mkdir -p \"$VER_ROOT\"",
            "printf '%s' " + SshProjectShell.quote(StrUtil.nullToEmpty(versionId)) + " > \"$VER_ROOT/current.txt\"",
            "echo " + OK_FLAG);
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, script, 30);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.version_snapshot_failed.7b2c") + StrUtil.nullToEmpty(result.getMessage()));
    }

    /**
     * 删除版本目录与元数据
     *
     * @param project   项目
     * @param versionId 版本ID
     */
    private void removeVersion(SshProjectModel project, String versionId) {
        this.assertVersionId(versionId);
        String script = String.join("\n",
            "set -u",
            VERSION_ROOT_DECL,
            "rm -rf \"$VER_ROOT/" + versionId + "\" \"$VER_ROOT/" + versionId + ".json\"",
            "echo " + OK_FLAG);
        SshProjectExecuteResult result = sshProjectExecutor.runScript(project, script, 60);
        Assert.state(result.isSuccess(), I18nMessageUtil.get("i18n.ssh_project_version_delete_fail.8a4c") + StrUtil.nullToEmpty(result.getMessage()));
    }

    /**
     * 清理超出保留个数的旧版本（永远保留当前版本与本次指定版本）
     *
     * @param project      项目
     * @param list         版本列表
     * @param keepAliveId  额外保留版本
     * @param versionCount 保留个数
     */
    private void clearOldVersion(SshProjectModel project, JSONObject list, String keepAliveId, int versionCount) {
        if (versionCount <= 0) {
            return;
        }
        List<JSONObject> metas = this.metas(list);
        String current = list.getString("current");
        for (int i = versionCount; i < metas.size(); i++) {
            JSONObject meta = metas.get(i);
            String versionId = meta.getString("versionId");
            if (StrUtil.equals(versionId, current) || StrUtil.equals(versionId, keepAliveId)) {
                continue;
            }
            try {
                this.removeVersion(project, versionId);
            } catch (Exception e) {
                log.warn("ssh project version prune failed: {} {}", versionId, e.getMessage());
            }
        }
    }

    /**
     * 版本列表脚本
     *
     * @return 脚本
     */
    private String scriptList() {
        return String.join("\n",
            PREAMBLE,
            "echo \"ROOT|$VER_ROOT\"",
            "echo \"PROJECT|$PROJECT_DIR\"",
            "echo \"PROJECT_FILES|$(ver_count \"$PROJECT_DIR\")\"",
            "echo \"CURRENT|$(cat \"$VER_ROOT/current.txt\" 2>/dev/null)\"",
            "if [ -d \"$VER_ROOT\" ]; then",
            "  for f in \"$VER_ROOT\"/*.json; do",
            "    [ -f \"$f\" ] || continue",
            "    vid=$(basename \"$f\" .json)",
            "    ex=0",
            "    [ -d \"$VER_ROOT/$vid\" ] && ex=1",
            "    echo \"META|$ex|$(tr -d '\\n' < \"$f\")\"",
            "  done",
            "fi",
            "echo " + OK_FLAG);
    }

    /**
     * 生成版本快照脚本（快照目录 + 元数据一次完成，避免中间态被当成可用版本）
     *
     * @param versionId     版本号
     * @param source        来源
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置ID
     * @param createTime    创建时间
     * @return 脚本
     */
    private String scriptSnapshot(String versionId, String source, Integer buildNumberId, String buildDataId, long createTime) {
        this.assertVersionId(versionId);
        String metaJsonFormat = "{\"versionId\":\"%s\",\"source\":\"%s\",\"buildNumberId\":%s,\"buildDataId\":\"%s\",\"createTime\":%s,\"fileCount\":%s,\"fileSize\":%s,\"manifest\":\"%s\"}";
        return String.join("\n",
            PREAMBLE,
            "VID=" + SshProjectShell.quote(versionId),
            "mkdir -p \"$VER_ROOT\"",
            "TMP=\"$VER_ROOT/$VID.tmp$$\"",
            "rm -rf \"$TMP\"",
            "mkdir -p \"$TMP\"",
            "copy_content \"$PROJECT_DIR\" \"$TMP\" || { echo \"" + ERROR_FLAG + "|copy project dir fail\"; exit 23; }",
            "rm -rf \"$VER_ROOT/$VID\"",
            "mv \"$TMP\" \"$VER_ROOT/$VID\" || { echo \"" + ERROR_FLAG + "|move version dir fail\"; exit 24; }",
            "COUNT=$(ver_count \"$VER_ROOT/$VID\")",
            "SIZE=$(ver_size \"$VER_ROOT/$VID\")",
            "MANIFEST=$(ver_manifest \"$VER_ROOT/$VID\")",
            "printf '" + metaJsonFormat + "' \"$VID\" " + SshProjectShell.quote(source) + " "
                + (buildNumberId == null ? "0" : String.valueOf(buildNumberId)) + " "
                + SshProjectShell.quote(StrUtil.nullToEmpty(buildDataId)) + " "
                + createTime + " \"$COUNT\" \"$SIZE\" \"$MANIFEST\" > \"$VER_ROOT/$VID.json\"",
            "echo \"versionId|$VID\"",
            "echo \"fileCount|$COUNT\"",
            "echo \"fileSize|$SIZE\"",
            "echo \"manifest|$MANIFEST\"",
            "echo " + OK_FLAG);
    }

    /**
     * 还原版本脚本（清空项目目录内容后覆盖版本内容，并输出内容指纹）
     *
     * @param versionId 版本号
     * @return 脚本
     */
    private String scriptRestore(String versionId) {
        this.assertVersionId(versionId);
        return String.join("\n",
            PREAMBLE,
            "VID=" + SshProjectShell.quote(versionId),
            "SRC=\"$VER_ROOT/$VID\"",
            "[ -d \"$SRC\" ] || { echo \"" + ERROR_FLAG + "|version dir not found: $VID\"; exit 22; }",
            "clear_content \"$PROJECT_DIR\" || { echo \"" + ERROR_FLAG + "|clear project dir fail\"; exit 24; }",
            "copy_content \"$SRC\" \"$PROJECT_DIR\" || { echo \"" + ERROR_FLAG + "|copy version fail\"; exit 23; }",
            "echo \"MANIFEST|$(ver_manifest \"$PROJECT_DIR\")\"",
            "echo \"versionId|$VID\"",
            "echo " + OK_FLAG);
    }

    /**
     * 解析版本列表脚本输出
     *
     * @param output 输出
     * @return json
     */
    private JSONObject parseList(String output) {
        JSONObject result = new JSONObject();
        List<JSONObject> list = new ArrayList<>();
        String current = null;
        int projectFiles = 0;
        for (String line : StrUtil.split(StrUtil.nullToEmpty(output), '\n')) {
            if (StrUtil.isEmpty(line)) {
                continue;
            }
            if (StrUtil.startWith(line, "ROOT|")) {
                result.put("path", StrUtil.subSuf(line, 5));
            } else if (StrUtil.startWith(line, "PROJECT|")) {
                result.put("projectDir", StrUtil.subSuf(line, 8));
            } else if (StrUtil.startWith(line, "PROJECT_FILES|")) {
                projectFiles = this.toInt(StrUtil.subSuf(line, 14));
            } else if (StrUtil.startWith(line, "CURRENT|")) {
                current = StrUtil.trimToNull(StrUtil.subSuf(line, 8));
            } else if (StrUtil.startWith(line, "META|")) {
                String rest = StrUtil.subSuf(line, 5);
                int index = rest.indexOf('|');
                if (index <= 0) {
                    continue;
                }
                String exist = rest.substring(0, index);
                String json = rest.substring(index + 1);
                try {
                    JSONObject meta = JSONObject.parseObject(json);
                    if (meta == null) {
                        continue;
                    }
                    meta.put("exist", "1".equals(exist));
                    list.add(meta);
                } catch (Exception e) {
                    log.warn("ssh project version meta parse error: {}", StrUtil.maxLength(json, 120));
                }
            }
        }
        list.sort(Comparator.comparingLong((JSONObject meta) -> {
            Long createTime = meta.getLong("createTime");
            return createTime == null ? 0L : createTime;
        }).reversed());
        result.put("current", current);
        result.put("list", list);
        result.put("projectFiles", projectFiles);
        result.put("hasVersions", !list.isEmpty());
        result.put("enabled", true);
        result.put("versionCount", DEFAULT_KEEP_COUNT);
        return result;
    }

    /**
     * 解析 key|value 形式的脚本输出
     *
     * @param output 输出
     * @return json
     */
    private JSONObject parseKeyValue(String output) {
        JSONObject result = new JSONObject();
        for (String line : StrUtil.split(StrUtil.nullToEmpty(output), '\n')) {
            if (StrUtil.isEmpty(line)) {
                continue;
            }
            int index = line.indexOf('|');
            if (index <= 0) {
                continue;
            }
            result.put(line.substring(0, index), line.substring(index + 1));
        }
        return result;
    }

    /**
     * 版本元数据列表（已按创建时间倒序）
     *
     * @param list 版本列表
     * @return list
     */
    private List<JSONObject> metas(JSONObject list) {
        List<JSONObject> metas = new ArrayList<>();
        if (list == null) {
            return metas;
        }
        JSONArray array = list.getJSONArray("list");
        if (array == null) {
            return metas;
        }
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                metas.add(item);
            }
        }
        return metas;
    }

    /**
     * 从列表中定位版本
     *
     * @param list      版本列表
     * @param versionId 版本ID
     * @return 元数据（可为空）
     */
    private JSONObject findMeta(JSONObject list, String versionId) {
        for (JSONObject meta : this.metas(list)) {
            if (StrUtil.equals(versionId, meta.getString("versionId"))) {
                return meta;
            }
        }
        return null;
    }

    /**
     * 版本号白名单校验（写入 shell / 拼接路径前必须调用）
     *
     * @param versionId 版本ID
     */
    private void assertVersionId(String versionId) {
        Assert.hasText(versionId, I18nMessageUtil.get("i18n.version_id_required.0a3e"));
        Assert.state(VERSION_ID_PATTERN.matcher(versionId).matches(),
            I18nMessageUtil.get("i18n.ssh_project_version_id_invalid.4d76") + versionId);
    }

    private int toInt(String value) {
        try {
            return Integer.parseInt(StrUtil.trimToEmpty(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private String now() {
        return DateUtil.format(new Date(), "yyyyMMddHHmmssSSS");
    }
}
