/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
    public IJsonMessage<JSONObject> getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id error") String id,
                                                HttpServletRequest request) {
        String workspaceId = outGivingServer.getCheckUserWorkspace(request);
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Objects.requireNonNull(outGivingServerItem, "没有数据");
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
            Assert.state(zip, "不支持的文件类型:" + path.getName());
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
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        File userTempPath = serverConfig.getUserTempPath();
        // 保存文件
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5);
        return JsonMessage.success("上传成功");
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
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
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
        return JsonMessage.success("上传成功,开始分发!");
    }

    private OutGivingModel check(String id, BiConsumer<OutGivingModel.Status, OutGivingModel> consumer, HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "上传失败,没有找到对应的分发项目");
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
        Assert.hasText(url, "填写下载地址");
        Assert.state(StrUtil.length(url) <= 200, "url 长度不能超过 200");
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
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
        return JsonMessage.success("下载成功,开始分发!");
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

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");

        BuildInfoModel infoModel = buildInfoService.getByKey(buildId, request);
        Assert.notNull(infoModel, "没有对应的构建");
        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setBuildDataId(infoModel.getId());
        Integer numberId = Convert.toInt(buildNumberId, 0);
        buildHistoryLog.setBuildNumberId(numberId);
        BuildHistoryLog historyLog = dbBuildHistoryLogService.queryByBean(buildHistoryLog);
        Assert.notNull(historyLog, "没有对应的构建记录");
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
        return JsonMessage.success("开始分发!");
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

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        FileStorageModel storageModel = fileStorageService.getByKey(fileId, request);
        Assert.notNull(storageModel, "对应的文件不存在");
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
        return JsonMessage.success("开始分发!");
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

        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"), request);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
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
        return JsonMessage.success("开始分发!");
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
        Assert.state(FileUtil.isFile(file), "当前文件丢失不能执行发布任务");
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
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status == OutGivingModel.Status.ING, "当前状态不是分发中"), request);
        OutGivingRun.cancel(outGivingModel.getId(), getUser());
        //
        return JsonMessage.success("取消成功");
    }

    @PostMapping(value = "config-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public IJsonMessage<String> configProject(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, "没有任何信息");
        String id = jsonObject.getString("id");
        List<OutGivingNodeProject> list = jsonObject.getList("data", OutGivingNodeProject.class);
        Assert.notEmpty(list, "没有配置任何项目");
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发项目");
        // 更新信息
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, "分发信息错误,没有任何项目");
        for (OutGivingNodeProject outGivingNodeProject : list) {
            OutGivingNodeProject nodeProject = OutGivingModel.getNodeProject(outGivingNodeProjects, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId());
            Assert.notNull(nodeProject, "没有找到对应的项目信息");
            nodeProject.setDisabled(outGivingNodeProject.getDisabled());
            nodeProject.setSortValue(outGivingNodeProject.getSortValue());
        }
        // 更新
        OutGivingModel update = new OutGivingModel();
        update.setId(outGivingModel.getId());
        update.outGivingNodeProjectList(outGivingNodeProjects);
        outGivingServer.updateById(update);
        return JsonMessage.success("更新成功");
    }

    @GetMapping(value = "remove-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public IJsonMessage<String> removeProject(@ValidatorItem String id,
                                              @ValidatorItem String nodeId,
                                              @ValidatorItem String projectId,
                                              HttpServletRequest request) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, request);
        Assert.notNull(outGivingModel, "没有找到对应的分发项目");
        List<OutGivingNodeProject> outGivingNodeProjects = outGivingModel.outGivingNodeProjectList();
        Assert.notEmpty(outGivingNodeProjects, "分发信息错误,没有任何项目");
        //
        Assert.state(outGivingNodeProjects.size() > 1, "当前分发只有一个项目啦,删除整个分发即可");
        outGivingNodeProjects = outGivingNodeProjects.stream()
            .filter(nodeProject -> !StrUtil.equals(nodeProject.getProjectId(), projectId) || !StrUtil.equals(nodeProject.getNodeId(), nodeId))
            .collect(Collectors.toList());
        // 更新
        OutGivingModel update = new OutGivingModel();
        update.setId(outGivingModel.getId());
        update.outGivingNodeProjectList(outGivingNodeProjects);
        outGivingServer.updateById(update);
        return JsonMessage.success("删除成功");
    }
}
