/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.outgiving;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.BaseIdModel;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.build.BuildExtraModule;
import org.dromara.jpom.build.BuildUtil;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.common.validator.ValidatorRule;
import org.dromara.jpom.func.files.model.FileStorageModel;
import org.dromara.jpom.func.files.model.StaticFileStorageModel;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.func.files.service.StaticFileStorageService;
import org.dromara.jpom.model.AfterOpt;
import org.dromara.jpom.model.BaseEnum;
import org.dromara.jpom.model.BaseNodeModel;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.BuildInfoModel;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.model.log.BuildHistoryLog;
import org.dromara.jpom.model.log.OutGivingLog;
import org.dromara.jpom.model.node.ProjectInfoCacheModel;
import org.dromara.jpom.model.outgiving.BaseNodeProject;
import org.dromara.jpom.model.outgiving.OutGivingModel;
import org.dromara.jpom.model.outgiving.OutGivingNodeProject;
import org.dromara.jpom.outgiving.OutGivingRun;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.dblog.BuildInfoService;
import org.dromara.jpom.service.dblog.DbBuildHistoryLogService;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.dromara.jpom.service.outgiving.DbOutGivingLogService;
import org.dromara.jpom.service.outgiving.OutGivingServer;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 分发文件管理
 *
 * @author bwcx_jzy
 * @since 2019/4/21
 */
@RestController
@RequestMapping(value = "/outgiving")
@Feature(cls = ClassFeature.OUTGIVING)
@Slf4j
public class OutGivingProjectController extends BaseServerController {

    private final OutGivingServer outGivingServer;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final ServerConfig serverConfig;
    private final DbOutGivingLogService dbOutGivingLogService;
    private final ProjectInfoCacheService projectInfoCacheService;
    private final BuildInfoService buildInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;
    private final FileStorageService fileStorageService;
    private final StaticFileStorageService staticFileStorageService;

    public OutGivingProjectController(OutGivingServer outGivingServer,
                                      OutGivingWhitelistService outGivingWhitelistService,
                                      ServerConfig serverConfig,
                                      DbOutGivingLogService dbOutGivingLogService,
                                      ProjectInfoCacheService projectInfoCacheService,
                                      BuildInfoService buildInfoService,
                                      DbBuildHistoryLogService dbBuildHistoryLogService,
                                      FileStorageService fileStorageService,
                                      StaticFileStorageService staticFileStorageService) {
        this.outGivingServer = outGivingServer;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.serverConfig = serverConfig;
        this.dbOutGivingLogService = dbOutGivingLogService;
        this.projectInfoCacheService = projectInfoCacheService;
        this.buildInfoService = buildInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
        this.fileStorageService = fileStorageService;
        this.staticFileStorageService = staticFileStorageService;
    }

    @RequestMapping(value = "getItemData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "i18n.parameter_error_id_error.58ce") String id,
                                                HttpServletRequest request) {
        String workspaceId = outGivingServer.getCheckUserWorkspace(request);
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Objects.requireNonNull(outGivingServerItem, I18nMessageUtil.get("i18n.no_data.1ac0"));
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.outGivingNodeProjectList();
        //
        Set<String> nodeIds = outGivingNodeProjectList.stream().map(BaseNodeProject::getNodeId).collect(Collectors.toSet());
        List<NodeModel> nodeModels = nodeService.getByKey(nodeIds);
        Map<String, NodeModel> nodeMap = CollStreamUtil.toMap(nodeModels, BaseIdModel::getId, nodeModel -> nodeModel);
        //
        Set<String> projectIds = outGivingNodeProjectList.stream().map(nodeProject -> BaseNodeModel.fullId(workspaceId, nodeProject.getNodeId(), nodeProject.getProjectId())).collect(Collectors.toSet());
        List<ProjectInfoCacheModel> projectInfoCacheModels = projectInfoCacheService.getByKey(projectIds);
        Map<String, ProjectInfoCacheModel> projectMap = CollStreamUtil.toMap(projectInfoCacheModels, BaseIdModel::getId, data -> data);


        List<JSONObject> collect = outGivingNodeProjectList
            .stream()
            .map(outGivingNodeProject -> {
                NodeModel nodeModel = nodeMap.get(outGivingNodeProject.getNodeId());
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("sortValue", outGivingNodeProject.getSortValue());
                jsonObject.put("disabled", outGivingNodeProject.getDisabled());
                jsonObject.put("nodeId", outGivingNodeProject.getNodeId());
                jsonObject.put("projectId", outGivingNodeProject.getProjectId());
                jsonObject.put("nodeName", nodeModel.getName());
                String fullId = BaseNodeModel.fullId(workspaceId, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
                jsonObject.put("id", fullId);
                ProjectInfoCacheModel projectInfoCacheModel = projectMap.get(fullId);
                if (projectInfoCacheModel != null) {
                    jsonObject.put("cacheProjectName", projectInfoCacheModel.getName());
                }

                OutGivingLog outGivingLog = dbOutGivingLogService.getByProject(id, outGivingNodeProject);
                if (outGivingLog != null) {
                    jsonObject.put("outGivingStatus", outGivingLog.getStatus());
                    jsonObject.put("outGivingResult", outGivingLog.getResult());
                    jsonObject.put("lastTime", outGivingLog.getCreateTimeMillis());
                    jsonObject.put("fileSize", outGivingLog.getFileSize());
                    jsonObject.put("progressSize", outGivingLog.getProgressSize());
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
        JSONObject data = new JSONObject();
        data.put("data", outGivingServerItem);
        data.put("projectList", collect);
        return JsonMessage.success("", data);
    }

    private File checkZip(File path, boolean unzip) {
        if (unzip) {
            boolean zip = false;
            for (String i : StringUtil.PACKAGE_EXT) {
                if (FileUtil.pathEndsWith(path, i)) {
                    zip = true;
                    break;
                }
            }
            Assert.state(zip, I18nMessageUtil.get("i18n.file_type_not_supported.ae5d") + path.getName());
        }
        return path;
    }

    /**
     * 节点分发文件
     *
     * @param id 分发id
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public IJsonMessage<Object> uploadSharding(String id,
                                               MultipartFile file,
                                               String sliceId,
                                               Integer totalSlice,
                                               Integer nowSlice,
                                               String fileSumMd5,
                                               HttpServletRequest request) throws IOException {
        // 状态判断
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.distribution_in_progress.c3ae")), request);
        File userTempPath = serverConfig.getUserTempPath();
        // 保存文件
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5);
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success.a769"));
    }

    /**
     * 节点分发文件
     *
     * @param id        分发id
     * @param afterOpt  之后的操作
     * @param autoUnzip 是否自动解压
     * @param clearOld  清空发布
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "upload-sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public IJsonMessage<Object> upload(String id, String afterOpt, String clearOld, String autoUnzip,
                                       String secondaryDirectory, String stripComponents,
                                       String selectProject,
                                       String sliceId,
                                       Integer totalSlice,
                                       String fileSumMd5, HttpServletRequest request) throws IOException {
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.distribution_in_progress.c3ae")), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));
        //
        boolean unzip = Convert.toBool(autoUnzip, false);
        File file = FileUtil.file(JpomApplication.getInstance().getDataPath(), ServerConst.OUTGIVING_FILE, id);
        FileUtil.mkdir(file);
        //
        File userTempPath = serverConfig.getUserTempPath();
        File successFile = this.shardingTryMerge(userTempPath.getAbsolutePath(), sliceId, totalSlice, fileSumMd5);
        FileUtil.move(successFile, file, true);
        //
        File dest = FileUtil.file(file, successFile.getName());
        dest = this.checkZip(dest, unzip);
        //
        OutGivingModel outGivingModel = new OutGivingModel();
        outGivingModel.setId(id);
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("upload");
        outGivingModel.setModeData(successFile.getName());
        outGivingServer.updateById(outGivingModel);
        int stripComponentsValue = Convert.toInt(stripComponents, 0);
        // 开启
        OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
            .id(outGivingModel.getId())
            .file(dest)
            .userModel(getUser())
            .unzip(unzip)
            .mode(outGivingModel.getMode())
            .modeData(outGivingModel.getModeData())
            .stripComponents(stripComponentsValue);
        outGivingRunBuilder.build().startRun(selectProject);
        return JsonMessage.success(I18nMessageUtil.get("i18n.upload_success_and_distribute.f446"));
    }

    private OutGivingModel check(String id, BiConsumer<OutGivingModel.Status, OutGivingModel> consumer, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, I18nMessageUtil.get("i18n.upload_failed_no_matching_project.b219"));
        // 检查状态
        Integer statusCode = outGivingModel.getStatus();
        OutGivingModel.Status status = BaseEnum.getEnum(OutGivingModel.Status.class, statusCode, OutGivingModel.Status.NO);
        consumer.accept(status, outGivingModel);
        return outGivingModel;
    }


    /**
     * 远程下载节点分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public IJsonMessage<String> remoteDownload(String id, String afterOpt, String clearOld, String url, String autoUnzip,
                                               String secondaryDirectory,
                                               String stripComponents,
                                               String selectProject,
                                               HttpServletRequest request) {
        Assert.hasText(url, I18nMessageUtil.get("i18n.fill_download_address.763c"));
        Assert.state(StrUtil.length(url) <= 200, I18nMessageUtil.get("i18n.url_length_exceeded.ca1c"));
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.distribution_in_progress.c3ae")), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);

        //outGivingModel = outGivingServer.getItem(id);
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("download");
        outGivingModel.setModeData(url);
        outGivingServer.updateById(outGivingModel);
        //下载
        File file = FileUtil.file(serverConfig.getUserTempPath(), ServerConst.OUTGIVING_FILE, id);
        FileUtil.mkdir(file);
        File downloadFile = HttpUtil.downloadFileFromUrl(url, file);
        this.startTask(outGivingModel, downloadFile, autoUnzip, stripComponents, selectProject, true);
        return JsonMessage.success(I18nMessageUtil.get("i18n.download_success_and_distribute.ae94"));
    }

    /**
     * 通过构建历史分发
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "use-build", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> useBuild(String id, String afterOpt, String clearOld, String buildId, String buildNumberId,
                                         String secondaryDirectory,
                                         String stripComponents,
                                         String selectProject,
                                         HttpServletRequest request) {

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.distribution_in_progress.c3ae")), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));

        BuildInfoModel infoModel = buildInfoService.getByKey(buildId, request);
        Assert.notNull(infoModel, I18nMessageUtil.get("i18n.no_build.d163"));
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setBuildDataId(infoModel.getId());
        Integer numberId = Convert.toInt(buildNumberId, 0);
        buildHistoryLog.setBuildNumberId(numberId);
        BuildHistoryLog historyLog = dbBuildHistoryLogService.queryByBean(buildHistoryLog);
        Assert.notNull(historyLog, I18nMessageUtil.get("i18n.no_build_record.66a2"));
        BuildExtraModule buildExtraModule = BuildExtraModule.build(historyLog);
        //String resultDirFileStr = buildExtraModule.getResultDirFile();
        EnvironmentMapBuilder environmentMapBuilder = buildHistoryLog.toEnvironmentMapBuilder();
        boolean tarGz = environmentMapBuilder.getBool(BuildUtil.USE_TAR_GZ, false);
        int stripComponentsValue = Convert.toInt(stripComponents, 0);
        //
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("use-build");
        outGivingModel.setModeData(buildId + ":" + buildNumberId);
        File resultDirFile = buildExtraModule.resultDirFile(numberId);
        outGivingServer.updateById(outGivingModel);
        //
        BuildUtil.loadDirPackage(infoModel.getId(), numberId, resultDirFile, tarGz, (unZip, zipFile) -> {
            OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
                .id(outGivingModel.getId())
                .file(zipFile)
                .userModel(getUser())
                .unzip(unZip)
                // 由构建配置决定是否删除
                .doneDeleteFile(false)
                .mode(outGivingModel.getMode())
                .modeData(outGivingModel.getModeData())
                .stripComponents(stripComponentsValue);
            return outGivingRunBuilder.build().startRun(selectProject);
        });
        return JsonMessage.success(I18nMessageUtil.get("i18n.start_distribution_exclamation.9fc2"));
    }

    /**
     * 文件中心分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "use-file-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> useFileStorage(String id, String afterOpt, String clearOld, String fileId, String autoUnzip,
                                               String secondaryDirectory,
                                               String stripComponents,
                                               String selectProject,
                                               HttpServletRequest request) {

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.distribution_in_progress.c3ae")), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));
        FileStorageModel storageModel = fileStorageService.getByKey(fileId, request);
        Assert.notNull(storageModel, I18nMessageUtil.get("i18n.file_not_exist.5091"));
        //
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("file-storage");
        outGivingModel.setModeData(fileId);
        outGivingServer.updateById(outGivingModel);
        File storageSavePath = serverConfig.fileStorageSavePath();
        File file = FileUtil.file(storageSavePath, storageModel.getPath());
        this.startTask(outGivingModel, file, autoUnzip, stripComponents, selectProject, false);
        return JsonMessage.success(I18nMessageUtil.get("i18n.start_distribution_exclamation.9fc2"));
    }

    /**
     * 静态文件分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @PostMapping(value = "use-static-file-storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> useStaticFileStorage(String id, String afterOpt, String clearOld, String fileId, String autoUnzip,
                                                     String secondaryDirectory,
                                                     String stripComponents,
                                                     String selectProject,
                                                     HttpServletRequest request) {

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.distribution_in_progress.c3ae")), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, I18nMessageUtil.get("i18n.post_distribution_action_required.8cc8"));
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(fileId);
        String workspaceId = outGivingServer.getCheckUserWorkspace(request);
        staticFileStorageService.checkStaticDir(storageModel, workspaceId);
        //
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingModel.setMode("static-file-storage");
        outGivingModel.setModeData(fileId);
        outGivingServer.updateById(outGivingModel);

        File file = FileUtil.file(storageModel.getAbsolutePath());
        this.startTask(outGivingModel, file, autoUnzip, stripComponents, selectProject, false);
        return JsonMessage.success(I18nMessageUtil.get("i18n.start_distribution_exclamation.9fc2"));
    }

    /**
     * 开始发布任务
     *
     * @param outGivingModel  分发对象
     * @param file            文件
     * @param autoUnzip       是否解压
     * @param stripComponents 剔除目录
     * @param selectProject   选择指定项目
     */
    private void startTask(OutGivingModel outGivingModel, File file, String autoUnzip,
                           String stripComponents,
                           String selectProject,
                           boolean deleteFile) {
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.file_missing_cannot_publish.3818"));
        //
        boolean unzip = BooleanUtil.toBoolean(autoUnzip);
        //
        this.checkZip(file, unzip);
        int stripComponentsValue = Convert.toInt(stripComponents, 0);
        // 开启
        OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
            .id(outGivingModel.getId())
            .file(file)
            .userModel(getUser())
            .unzip(unzip)
            .mode(outGivingModel.getMode())
            .modeData(outGivingModel.getModeData())
            // 是否删除
            .doneDeleteFile(deleteFile)
            // 可以不再设置-会查询最新的
            //            .projectSecondaryDirectory(secondaryDirectory)
            .stripComponents(stripComponentsValue);
        outGivingRunBuilder.build().startRun(selectProject);
    }

    @PostMapping(value = "cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public IJsonMessage<String> cancel(@ValidatorItem String id, HttpServletRequest request) {
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status == OutGivingModel.Status.ING, I18nMessageUtil.get("i18n.status_not_distributing.6298")), request);
        OutGivingRun.cancel(outGivingModel.getId(), getUser());
        //
        return JsonMessage.success(I18nMessageUtil.get("i18n.cancel_success.285f"));
    }

    @PostMapping(value = "config-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> configProject(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, I18nMessageUtil.get("i18n.no_info.e59e"));
        String id = jsonObject.getString("id");
        List<OutGivingNodeProject> list = jsonObject.getList("data", OutGivingNodeProject.class);
        Assert.notEmpty(list, I18nMessageUtil.get("i18n.no_projects_configured.e873"));
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, I18nMessageUtil.get("i18n.no_distribution_project_found.90b0"));
        // 更新信息
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, I18nMessageUtil.get("i18n.distribute_info_error_no_projects.e75f"));
        for (OutGivingNodeProject outGivingNodeProject : list) {
            OutGivingNodeProject nodeProject = OutGivingModel.getNodeProject(outGivingNodeProjects, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
            Assert.notNull(nodeProject, I18nMessageUtil.get("i18n.no_project_info_found.725a"));
            nodeProject.setDisabled(outGivingNodeProject.getDisabled());
            nodeProject.setSortValue(outGivingNodeProject.getSortValue());
        }
        // 更新
        OutGivingModel update = new OutGivingModel();
        update.setId(outGivingModel.getId());
        update.outGivingNodeProjectList(outGivingNodeProjects);
        outGivingServer.updateById(update);
        return JsonMessage.success(I18nMessageUtil.get("i18n.update_success.55aa"));
    }

    @GetMapping(value = "remove-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> removeProject(@ValidatorItem String id,
                                              @ValidatorItem String nodeId,
                                              @ValidatorItem String projectId,
                                              HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, I18nMessageUtil.get("i18n.no_distribution_project_found.90b0"));
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, I18nMessageUtil.get("i18n.distribute_info_error_no_projects.e75f"));
        //
        Assert.state(outGivingNodeProjects.size() > 1, I18nMessageUtil.get("i18n.current_distribution_has_only_one_project.cd59"));
        outGivingNodeProjects = outGivingNodeProjects.stream()
            .filter(nodeProject -> !StrUtil.equals(nodeProject.getProjectId(), projectId) || !StrUtil.equals(nodeProject.getNodeId(), nodeId))
            .collect(Collectors.toList());
        // 更新
        OutGivingModel update = new OutGivingModel();
        update.setId(outGivingModel.getId());
        update.outGivingNodeProjectList(outGivingNodeProjects);
        outGivingServer.updateById(update);
        return JsonMessage.success(I18nMessageUtil.get("i18n.delete_success.0007"));
    }
}
