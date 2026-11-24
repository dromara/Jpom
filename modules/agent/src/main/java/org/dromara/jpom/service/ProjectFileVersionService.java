/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.commander.CommandOpResult;
import org.dromara.jpom.common.commander.ProjectCommander;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.configuration.ProjectConfig;
import org.dromara.jpom.model.data.DslYmlDto;
import org.dromara.jpom.model.data.NodeProjectInfoModel;
import org.dromara.jpom.service.manage.ProjectInfoService;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.system.JpomRuntimeException;
import org.dromara.jpom.util.CommandUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 项目文件完整版本快照（构建发布触发的版本管理）
 * <p>
 * 与「项目文件自动备份」（{@link ProjectFileBackupService}）是两套独立能力：
 * 备份是文件操作触发的反向差分快照，只能用于人工救急还原；
 * 版本快照是构建发布触发的完整目录快照，用于「快速回滚」（切换本地版本，不重新传输产物）。
 *
 * @author bwcx_jzy
 * @since 2026/11/18
 */
@Slf4j
@Service
public class ProjectFileVersionService {

    /**
     * 版本元数据文件后缀，与版本目录同级，避免元数据污染版本内容
     */
    private static final String META_SUFFIX = ".json";
    /**
     * 当前版本标识文件
     */
    private static final String CURRENT_FILE = "current.txt";

    private final ProjectConfig projectConfig;
    private final ProjectInfoService projectInfoService;
    private final ProjectCommander projectCommander;

    public ProjectFileVersionService(AgentConfig agentConfig,
                                     ProjectInfoService projectInfoService,
                                     ProjectCommander projectCommander) {
        this.projectConfig = agentConfig.getProject();
        this.projectInfoService = projectInfoService;
        this.projectCommander = projectCommander;
    }

    /**
     * 解析项目生效的版本保留个数（0 表示关闭）
     *
     * @param projectInfoModel 项目
     * @return int
     */
    public int resolveVersionCount(NodeProjectInfoModel projectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        Integer dslCount = Optional.ofNullable(infoModel.dslConfig())
            .map(DslYmlDto::getFile)
            .map(DslYmlDto.FileConfig::getVersionCount)
            .orElse(null);
        return dslCount == null ? projectConfig.getVersionCount() : dslCount;
    }

    /**
     * 解析版本快照存储根目录
     *
     * @param projectInfoModel 项目
     * @return file
     */
    public File pathProjectVersion(NodeProjectInfoModel projectInfoModel) {
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        String path = Optional.ofNullable(infoModel.dslConfig())
            .map(DslYmlDto::getFile)
            .map(DslYmlDto.FileConfig::getVersionPath)
            .filter(StrUtil::isNotBlank)
            .orElse(projectConfig.getVersionPath());
        if (StrUtil.isNotBlank(path)) {
            return FileUtil.file(path, infoModel.getId());
        }
        String dataPath = JpomApplication.getInstance().getDataPath();
        return FileUtil.file(dataPath, "project_file_version", infoModel.getId());
    }

    /**
     * 能力探测（服务端据此决定是否展示快速回滚入口）
     *
     * @param projectInfoModel 项目
     * @return json
     */
    public JSONObject capability(NodeProjectInfoModel projectInfoModel) {
        int versionCount = this.resolveVersionCount(projectInfoModel);
        File root = this.pathProjectVersion(projectInfoModel);
        boolean hasVersions = !this.listMeta(root).isEmpty();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("support", true);
        // 构建配置按「保留个数默认 3」生成过版本时，插件端配置可能仍为 0：
        // 只要磁盘上已有版本，就不能报告为「未开启」，否则前端会把已有版本藏起来且无法切换。
        jsonObject.put("enabled", versionCount > 0 || hasVersions);
        jsonObject.put("hasVersions", hasVersions);
        jsonObject.put("versionCount", versionCount);
        jsonObject.put("path", FileUtil.getAbsolutePath(root));
        return jsonObject;
    }

    /**
     * 发布前准备：首次开启时把当前运行目录作为基线版本，避免第一次回滚无版本可用
     *
     * @param projectInfoModel 项目
     * @param buildNumberId    构建编号
     * @param keepCount        本次保留个数，为空使用项目配置
     * @return json
     */
    public JSONObject begin(NodeProjectInfoModel projectInfoModel, Integer buildNumberId, Integer keepCount) {
        int versionCount = this.resolveKeepCount(projectInfoModel, keepCount);
        JSONObject result = new JSONObject();
        result.put("enabled", versionCount > 0);
        result.put("versionCount", versionCount);
        if (versionCount <= 0) {
            return result;
        }
        File root = this.pathProjectVersion(projectInfoModel);
        FileUtil.mkdir(root);
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        File lib = projectInfoService.resolveLibFile(infoModel);
        if (this.listMeta(root).isEmpty() && FileUtil.isDirectory(lib) && !FileUtil.isDirEmpty(lib)) {
            JSONObject baseline = this.snapshot(root, lib, "baseline-" + this.now(), "baseline", buildNumberId, null);
            result.put("baseline", baseline);
            log.debug(I18nMessageUtil.get("i18n.version_baseline_created.2b6c"), projectInfoModel.getId(), baseline.getString("versionId"));
        }
        this.clearOldVersion(root, null, versionCount);
        result.put("current", this.readCurrent(root));
        result.put("count", this.listMeta(root).size());
        return result;
    }

    /**
     * 发布成功后登记新版本（完整快照）
     *
     * @param projectInfoModel 项目
     * @param buildNumberId    构建编号
     * @param buildDataId      构建配置ID
     * @param keepCount        保留个数
     * @return json
     */
    public JSONObject commit(NodeProjectInfoModel projectInfoModel, Integer buildNumberId, String buildDataId, Integer keepCount) {
        int versionCount = this.resolveKeepCount(projectInfoModel, keepCount);
        JSONObject result = new JSONObject();
        result.put("enabled", versionCount > 0);
        if (versionCount <= 0) {
            return result;
        }
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        File lib = projectInfoService.resolveLibFile(infoModel);
        Assert.state(FileUtil.isDirectory(lib) && !FileUtil.isDirEmpty(lib), I18nMessageUtil.get("i18n.version_project_file_empty.71d4"));
        File root = this.pathProjectVersion(projectInfoModel);
        FileUtil.mkdir(root);
        String versionId = buildNumberId == null ? "build-" + this.now() : String.valueOf(buildNumberId);
        if (FileUtil.exist(FileUtil.file(root, versionId))) {
            versionId = versionId + "-" + this.now();
        }
        JSONObject meta = this.snapshot(root, lib, versionId, "build", buildNumberId, buildDataId);
        this.writeCurrent(root, versionId);
        this.clearOldVersion(root, null, versionCount);
        result.put("versionId", meta.getString("versionId"));
        result.put("manifest", meta.getString("manifest"));
        result.put("fileSize", meta.getLong("fileSize"));
        result.put("fileCount", meta.getInteger("fileCount"));
        result.put("createTime", meta.getLong("createTime"));
        result.put("count", this.listMeta(root).size());
        return result;
    }

    /**
     * 版本列表
     *
     * @param projectInfoModel 项目
     * @return json
     */
    public JSONObject list(NodeProjectInfoModel projectInfoModel) {
        File root = this.pathProjectVersion(projectInfoModel);
        int versionCount = this.resolveVersionCount(projectInfoModel);
        List<JSONObject> metaList = this.listMeta(root);
        boolean hasVersions = !metaList.isEmpty();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("path", FileUtil.getAbsolutePath(root));
        // 与 capability 同口径：已有版本（例如按构建配置默认 3 生成的）视为可用，不因插件端配置为 0 而隐藏
        jsonObject.put("enabled", versionCount > 0 || hasVersions);
        jsonObject.put("hasVersions", hasVersions);
        jsonObject.put("versionCount", versionCount);
        jsonObject.put("current", this.readCurrent(root));
        jsonObject.put("list", metaList);
        return jsonObject;
    }

    /**
     * 切换版本（快速回滚）
     *
     * @param projectInfoModel 项目
     * @param versionId        目标版本
     * @param afterOpt         切换后操作（0 不操作，大于 0 重启）
     * @param snapshotCurrent  切换前是否把当前目录快照为安全版本
     * @param keepCount        保留个数
     * @return json
     */
    public JSONObject switchVersion(NodeProjectInfoModel projectInfoModel, String versionId, Integer afterOpt, Boolean snapshotCurrent, Integer keepCount) {
        Assert.hasText(versionId, I18nMessageUtil.get("i18n.version_id_required.0a3e"));
        File root = this.pathProjectVersion(projectInfoModel);
        File target = FileUtil.file(root, versionId);
        Assert.state(FileUtil.isDirectory(target) && !FileUtil.isDirEmpty(target), I18nMessageUtil.get("i18n.version_not_exists.5d90") + versionId);
        NodeProjectInfoModel infoModel = projectInfoService.resolveModel(projectInfoModel);
        File lib = projectInfoService.resolveLibFile(infoModel);
        String previousVersionId = this.readCurrent(root);
        JSONObject safety = null;
        if (!Boolean.FALSE.equals(snapshotCurrent) && FileUtil.isDirectory(lib) && !FileUtil.isDirEmpty(lib)) {
            safety = this.snapshot(root, lib, "auto-" + this.now(), "auto", null, null);
        }
        int after = Convert.toInt(afterOpt, 0);
        try {
            this.clearLib(lib);
            this.copyVersionContent(target, lib);
            JSONObject meta = this.readMeta(root, versionId);
            if (meta != null && StrUtil.isNotBlank(meta.getString("manifest"))) {
                String manifest = ProjectFileVersionUtil.manifest(lib);
                Assert.state(StrUtil.equals(meta.getString("manifest"), manifest),
                    I18nMessageUtil.get("i18n.version_verify_failed.d3a9") + versionId);
            }
            if (after > 0) {
                CommandOpResult commandOpResult = projectCommander.execCommand(ConsoleCommandOp.restart, infoModel);
                Assert.state(commandOpResult.isSuccess(),
                    I18nMessageUtil.get("i18n.version_restart_failed.6b05") + commandOpResult.msgStr());
            }
        } catch (Exception e) {
            this.restore(lib, root, safety);
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.version_switch_failed.9c31") + e.getMessage(), e);
        }
        this.writeCurrent(root, versionId);
        this.clearOldVersion(root, versionId, this.resolveKeepCount(projectInfoModel, keepCount));
        JSONObject result = new JSONObject();
        result.put("versionId", versionId);
        result.put("previousVersionId", previousVersionId);
        result.put("safetyVersionId", safety == null ? null : safety.getString("versionId"));
        result.put("restarted", after > 0);
        return result;
    }

    /**
     * 删除版本
     *
     * @param projectInfoModel 项目
     * @param versionId        版本ID
     */
    public void deleteVersion(NodeProjectInfoModel projectInfoModel, String versionId) {
        Assert.hasText(versionId, I18nMessageUtil.get("i18n.version_id_required.0a3e"));
        File root = this.pathProjectVersion(projectInfoModel);
        Assert.state(!StrUtil.equals(versionId, this.readCurrent(root)),
            I18nMessageUtil.get("i18n.version_current_delete_forbidden.2e84"));
        File target = FileUtil.file(root, versionId);
        Assert.state(FileUtil.exist(target), I18nMessageUtil.get("i18n.version_not_exists.5d90") + versionId);
        CommandUtil.systemFastDel(target);
        CommandUtil.systemFastDel(FileUtil.file(root, versionId + META_SUFFIX));
    }

    /**
     * 切换失败时恢复安全版本
     *
     * @param lib    项目目录
     * @param root   版本根目录
     * @param safety 安全版本元数据
     */
    private void restore(File lib, File root, JSONObject safety) {
        if (safety == null) {
            return;
        }
        try {
            File safetyDir = FileUtil.file(root, safety.getString("versionId"));
            if (!FileUtil.isDirectory(safetyDir)) {
                return;
            }
            this.clearLib(lib);
            this.copyVersionContent(safetyDir, lib);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.version_switch_rollback_failed.4a7e"), e);
        }
    }

    private int resolveKeepCount(NodeProjectInfoModel projectInfoModel, Integer keepCount) {
        if (keepCount != null && keepCount > 0) {
            return keepCount;
        }
        return this.resolveVersionCount(projectInfoModel);
    }

    /**
     * 生成一个完整版本快照
     *
     * @param root          版本根目录
     * @param lib           项目文件目录
     * @param versionId     版本ID
     * @param source        来源：baseline 首次基线、build 构建发布、auto 切换前自动快照
     * @param buildNumberId 构建编号
     * @param buildDataId   构建配置ID
     * @return 元数据
     */
    private JSONObject snapshot(File root, File lib, String versionId, String source, Integer buildNumberId, String buildDataId) {
        File target = FileUtil.file(root, versionId);
        File temp = FileUtil.file(root, versionId + ProjectFileVersionUtil.TEMP_SUFFIX + IdUtil.fastSimpleUUID());
        try {
            FileUtil.mkdir(temp);
            ProjectFileVersionUtil.copyDirContent(lib, temp);
            CommandUtil.systemFastDel(target);
            FileUtil.move(temp, target, true);
        } catch (Exception e) {
            CommandUtil.systemFastDel(temp);
            throw new JpomRuntimeException(I18nMessageUtil.get("i18n.version_snapshot_failed.7b2c") + e.getMessage(), e);
        }
        JSONObject meta = new JSONObject();
        meta.put("versionId", versionId);
        meta.put("source", source);
        meta.put("buildNumberId", buildNumberId);
        meta.put("buildDataId", buildDataId);
        meta.put("createTime", System.currentTimeMillis());
        meta.put("fileCount", ProjectFileVersionUtil.countFiles(target));
        meta.put("fileSize", FileUtil.size(target));
        meta.put("manifest", ProjectFileVersionUtil.manifest(target));
        this.writeMeta(root, versionId, meta);
        return meta;
    }

    /**
     * 复制版本内容回项目目录（跳过忽略目录）
     *
     * @param versionDir 版本目录
     * @param lib        项目目录
     */
    private void copyVersionContent(File versionDir, File lib) {
        ProjectFileVersionUtil.copyDirContent(versionDir, lib);
    }

    /**
     * 清空项目目录（保留忽略目录）
     *
     * @param lib 项目目录
     */
    private void clearLib(File lib) {
        ProjectFileVersionUtil.clearDir(lib);
    }

    /**
     * 清理超出保留个数的旧版本（永远保留当前版本）
     *
     * @param root         版本根目录
     * @param keepAliveId  额外保留的版本
     * @param versionCount 保留个数
     */
    private void clearOldVersion(File root, String keepAliveId, int versionCount) {
        if (versionCount <= 0) {
            // 未启用（或调用方没有指定保留个数）：不清理，避免把历史版本误删成只剩当前版本
            return;
        }
        int count = versionCount;
        List<JSONObject> metas = this.listMeta(root);
        String current = this.readCurrent(root);
        for (int i = count; i < metas.size(); i++) {
            JSONObject meta = metas.get(i);
            String versionId = meta.getString("versionId");
            if (StrUtil.equals(versionId, current) || StrUtil.equals(versionId, keepAliveId)) {
                continue;
            }
            CommandUtil.systemFastDel(FileUtil.file(root, versionId));
            CommandUtil.systemFastDel(FileUtil.file(root, versionId + META_SUFFIX));
        }
    }

    /**
     * 列出全部版本元数据（新到旧）
     *
     * @param root 版本根目录
     * @return list
     */
    private List<JSONObject> listMeta(File root) {
        List<JSONObject> result = new ArrayList<>();
        if (!FileUtil.isDirectory(root)) {
            return result;
        }
        File[] files = root.listFiles();
        if (files == null) {
            return result;
        }
        for (File file : files) {
            if (!FileUtil.isFile(file) || !StrUtil.endWith(FileUtil.getName(file), META_SUFFIX)) {
                continue;
            }
            try {
                JSONObject meta = JSON.parseObject(FileUtil.readUtf8String(file));
                if (meta == null) {
                    continue;
                }
                String versionId = meta.getString("versionId");
                meta.put("versionDir", FileUtil.getAbsolutePath(FileUtil.file(root, versionId)));
                meta.put("exist", FileUtil.isDirectory(FileUtil.file(root, versionId)));
                result.add(meta);
            } catch (Exception e) {
                log.warn(I18nMessageUtil.get("i18n.version_meta_parse_error.6f18"), file.getAbsolutePath(), e);
            }
        }
        result.sort(Comparator.comparingLong(this::createTime).reversed());
        return result;
    }

    private long createTime(JSONObject meta) {
        Long createTime = meta.getLong("createTime");
        return createTime == null ? 0L : createTime;
    }

    private JSONObject readMeta(File root, String versionId) {
        File metaFile = FileUtil.file(root, versionId + META_SUFFIX);
        if (!FileUtil.isFile(metaFile)) {
            return null;
        }
        try {
            return JSON.parseObject(FileUtil.readUtf8String(metaFile));
        } catch (Exception e) {
            return null;
        }
    }

    private void writeMeta(File root, String versionId, JSONObject meta) {
        FileUtil.mkdir(root);
        FileUtil.writeUtf8String(meta.toJSONString(), FileUtil.file(root, versionId + META_SUFFIX));
    }

    private String readCurrent(File root) {
        File currentFile = FileUtil.file(root, CURRENT_FILE);
        if (!FileUtil.isFile(currentFile)) {
            return null;
        }
        return StrUtil.trimToNull(FileUtil.readUtf8String(currentFile));
    }

    private void writeCurrent(File root, String versionId) {
        FileUtil.mkdir(root);
        FileUtil.writeUtf8String(StrUtil.nullToEmpty(versionId), FileUtil.file(root, CURRENT_FILE));
    }

    private String now() {
        return DateTime.now().toString("yyyyMMddHHmmssSSS");
    }
}
