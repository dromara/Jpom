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
package io.jpom.controller.outgiving;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import io.jpom.JpomApplication;
import io.jpom.common.BaseServerController;
import io.jpom.common.JsonMessage;
import io.jpom.common.ServerConst;
import io.jpom.common.validator.ValidatorItem;
import io.jpom.common.validator.ValidatorRule;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseIdModel;
import io.jpom.model.BaseNodeModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.model.log.OutGivingLog;
import io.jpom.model.node.ProjectInfoCacheModel;
import io.jpom.model.outgiving.BaseNodeProject;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.outgiving.OutGivingNodeProject;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.outgiving.DbOutGivingLogService;
import io.jpom.service.outgiving.OutGivingServer;
import io.jpom.system.ServerConfig;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
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
 * @author jiangzeyin
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

    public OutGivingProjectController(OutGivingServer outGivingServer,
                                      OutGivingWhitelistService outGivingWhitelistService,
                                      ServerConfig serverConfig,
                                      DbOutGivingLogService dbOutGivingLogService,
                                      ProjectInfoCacheService projectInfoCacheService) {
        this.outGivingServer = outGivingServer;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.serverConfig = serverConfig;
        this.dbOutGivingLogService = dbOutGivingLogService;
        this.projectInfoCacheService = projectInfoCacheService;
    }

    @RequestMapping(value = "getItemData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonMessage<List<JSONObject>> getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id error") String id,
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
        return JsonMessage.success("", collect);
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
    public JsonMessage<Object> uploadSharding(String id,
                                              MultipartFile file,
                                              String sliceId,
                                              Integer totalSlice,
                                              Integer nowSlice,
                                              String fileSumMd5) throws IOException {
        // 状态判断
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"));
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
    public JsonMessage<Object> upload(String id, String afterOpt, String clearOld, String autoUnzip,
                                      String secondaryDirectory, String stripComponents,
                                      String selectProject,
                                      String sliceId,
                                      Integer totalSlice,
                                      String fileSumMd5) throws IOException {
        this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"));
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

        outGivingServer.updateById(outGivingModel);
        int stripComponentsValue = Convert.toInt(stripComponents, 0);
        // 开启
        OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
            .id(outGivingModel.getId())
            .file(dest)
            .userModel(getUser())
            .unzip(unzip)
            .stripComponents(stripComponentsValue);
        outGivingRunBuilder.build().startRun(selectProject);
        return JsonMessage.success("上传成功,开始分发!");
    }

    private OutGivingModel check(String id, BiConsumer<OutGivingModel.Status, OutGivingModel> consumer) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, getRequest());
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
    public JsonMessage<String> remoteDownload(String id, String afterOpt, String clearOld, String url, String autoUnzip,
                                              String secondaryDirectory,
                                              String stripComponents,
                                              String selectProject,
                                              HttpServletRequest request) {
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束"));
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);

        //outGivingModel = outGivingServer.getItem(id);
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());
        outGivingModel.setSecondaryDirectory(secondaryDirectory);
        outGivingServer.updateById(outGivingModel);
        //下载
        File file = FileUtil.file(serverConfig.getUserTempPath(), ServerConst.OUTGIVING_FILE, id);
        FileUtil.mkdir(file);
        File downloadFile = HttpUtil.downloadFileFromUrl(url, file);
        boolean unzip = BooleanUtil.toBoolean(autoUnzip);
        //
        this.checkZip(downloadFile, unzip);
        int stripComponentsValue = Convert.toInt(stripComponents, 0);
        // 开启
        OutGivingRun.OutGivingRunBuilder outGivingRunBuilder = OutGivingRun.builder()
            .id(outGivingModel.getId())
            .file(downloadFile)
            .userModel(getUser())
            .unzip(unzip)
            .stripComponents(stripComponentsValue);
        outGivingRunBuilder.build().startRun(selectProject);
        return JsonMessage.success("下载成功,开始分发!");
    }

    @PostMapping(value = "cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EXECUTE)
    public JsonMessage<String> cancel(@ValidatorItem String id) {
        OutGivingModel outGivingModel = this.check(id, (status, outGivingModel1) -> Assert.state(status == OutGivingModel.Status.ING, "当前状态不是分发中"));
        OutGivingRun.cancel(outGivingModel.getId());
        //
        return JsonMessage.success("取消成功");
    }

    @PostMapping(value = "config-project", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> configProject(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
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
    public JsonMessage<String> removeProject(@ValidatorItem String id,
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
