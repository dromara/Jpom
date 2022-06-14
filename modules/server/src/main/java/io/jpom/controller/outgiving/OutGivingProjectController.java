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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.jiangzeyin.controller.multipart.MultipartFileBuilder;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.BaseNodeModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.ServerWhitelist;
import io.jpom.model.outgiving.OutGivingModel;
import io.jpom.model.outgiving.OutGivingNodeProject;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.node.ProjectInfoCacheService;
import io.jpom.service.outgiving.OutGivingServer;
import io.jpom.system.ConfigBean;
import io.jpom.system.ServerConfigBean;
import io.jpom.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final ProjectInfoCacheService projectInfoCacheService;
    private final OutGivingWhitelistService outGivingWhitelistService;

    public OutGivingProjectController(OutGivingServer outGivingServer,
                                      ProjectInfoCacheService projectInfoCacheService,
                                      OutGivingWhitelistService outGivingWhitelistService) {
        this.outGivingServer = outGivingServer;
        this.projectInfoCacheService = projectInfoCacheService;
        this.outGivingWhitelistService = outGivingWhitelistService;
    }

    @RequestMapping(value = "getProjectStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getProjectStatus() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_GetProjectStatus).toString();
    }


    @RequestMapping(value = "getItemData.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getItemData(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id error") String id) {
        HttpServletRequest request = getRequest();
        String workspaceId = outGivingServer.getCheckUserWorkspace(request);
        OutGivingModel outGivingServerItem = outGivingServer.getByKey(id, request);
        Objects.requireNonNull(outGivingServerItem, "没有数据");
        List<OutGivingNodeProject> outGivingNodeProjectList = outGivingServerItem.outGivingNodeProjectList();
        List<JSONObject> collect = outGivingNodeProjectList.stream().map(outGivingNodeProject -> {
            NodeModel nodeModel = nodeService.getByKey(outGivingNodeProject.getNodeId());
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("nodeId", outGivingNodeProject.getNodeId());
            jsonObject.put("projectId", outGivingNodeProject.getProjectId());
            jsonObject.put("nodeName", nodeModel.getName());
            jsonObject.put("id", BaseNodeModel.fullId(workspaceId, outGivingNodeProject.getNodeId(), outGivingNodeProject.getProjectId()));
            // set projectStatus property
            //NodeModel node = nodeService.getItem(outGivingNodeProject.getNodeId());
            // Project Status: data.pid > 0 means running
            JSONObject projectStatus = JsonMessage.toJson(200, "success");
            if (nodeModel.isOpenStatus()) {
                JSONObject projectInfo = null;
                try {
                    projectInfo = projectInfoCacheService.getItem(nodeModel, outGivingNodeProject.getProjectId());
                    projectStatus = NodeForward.requestBySys(nodeModel, NodeUrl.Manage_GetProjectStatus, "id", outGivingNodeProject.getProjectId()).toJson();
                } catch (Exception e) {
                    jsonObject.put("errorMsg", "error " + e.getMessage());
                }
                if (projectInfo != null) {
                    jsonObject.put("projectName", projectInfo.getString("name"));
                }
            } else {
                jsonObject.put("errorMsg", "节点未启用");
            }
            JSONObject data = projectStatus.getJSONObject("data");
            if (data != null && data.getInteger("pId") != null) {
                jsonObject.put("projectStatus", data.getIntValue("pId") > 0);
            } else {
                jsonObject.put("projectStatus", false);
            }
            // BaseEnum.getDescByCode(Status.class, getStatus())
            jsonObject.put("outGivingStatus", BaseEnum.getDescByCode(OutGivingNodeProject.Status.class, outGivingNodeProject.getStatus()));
            jsonObject.put("outGivingResult", outGivingNodeProject.getResult());
            jsonObject.put("lastTime", outGivingNodeProject.getLastOutGivingTime());
            return jsonObject;
        }).collect(Collectors.toList());
        return JsonMessage.getString(200, "", collect);
    }

    private File checkZip(String path, boolean unzip) {
        File file = FileUtil.file(path);
        return this.checkZip(file, unzip);
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
     * @param id        分发id
     * @param afterOpt  之后的操作
     * @param autoUnzip 是否自动解压
     * @param clearOld  清空发布
     * @return json
     * @throws IOException IO
     */
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public String upload(String id, String afterOpt, String clearOld, String autoUnzip) throws IOException {
        OutGivingModel outGivingModel = this.check(id);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        //
        boolean unzip = Convert.toBool(autoUnzip, false);
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), ServerConfigBean.OUTGIVING_FILE, id);
        MultipartFileBuilder multipartFileBuilder = createMultipart();
        multipartFileBuilder
            .setUseOriginalFilename(true)
            //				.setFileExt(StringUtil.PACKAGE_EXT)
            .addFieldName("file")
            .setSavePath(FileUtil.getAbsolutePath(file));
        String path = multipartFileBuilder.save();
        //
        File dest = this.checkZip(path, unzip);
        //
        //outGivingModel = outGivingServer.getItem(id);
        outGivingModel.setClearOld(Convert.toBool(clearOld, false));
        outGivingModel.setAfterOpt(afterOpt1.getCode());

        outGivingServer.update(outGivingModel);
        // 开启
        OutGivingRun.startRun(outGivingModel.getId(), dest, getUser(), unzip);
        return JsonMessage.getString(200, "分发成功");
    }

    private OutGivingModel check(String id) {
        OutGivingModel outGivingModel = outGivingServer.getByKey(id, getRequest());
        Assert.notNull(outGivingModel, "上传失败,没有找到对应的分发项目");
        // 检查状态
        Integer statusCode = outGivingModel.getStatus();
        OutGivingModel.Status status = BaseEnum.getEnum(OutGivingModel.Status.class, statusCode, OutGivingModel.Status.NO);
        Assert.state(status != OutGivingModel.Status.ING, "当前还在分发中,请等待分发结束");
        return outGivingModel;
    }


    /**
     * 远程下载节点分发文件
     *
     * @param id       分发id
     * @param afterOpt 之后的操作
     * @return json
     */
    @RequestMapping(value = "remote_download", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public String remoteDownload(String id, String afterOpt, String clearOld, String url, String autoUnzip) {
        OutGivingModel outGivingModel = this.check(id);
        AfterOpt afterOpt1 = BaseEnum.getEnum(AfterOpt.class, Convert.toInt(afterOpt, 0));
        Assert.notNull(afterOpt1, "请选择分发后的操作");
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(getRequest());
        Set<String> allowRemoteDownloadHost = whitelist.getAllowRemoteDownloadHost();
        Assert.state(CollUtil.isNotEmpty(allowRemoteDownloadHost), "还没有配置运行的远程地址");
        List<String> collect = allowRemoteDownloadHost.stream().filter(s -> StrUtil.startWith(url, s)).collect(Collectors.toList());
        Assert.state(CollUtil.isNotEmpty(collect), "不允许下载当前地址的文件");
        try {
            //outGivingModel = outGivingServer.getItem(id);
            outGivingModel.setClearOld(Convert.toBool(clearOld, false));
            outGivingModel.setAfterOpt(afterOpt1.getCode());
            outGivingServer.update(outGivingModel);
            //下载
            File file = FileUtil.file(ServerConfigBean.getInstance().getUserTempPath(), ServerConfigBean.OUTGIVING_FILE, id);
            FileUtil.mkdir(file);
            File downloadFile = HttpUtil.downloadFileFromUrl(url, file);
            boolean unzip = BooleanUtil.toBoolean(autoUnzip);
            //
            this.checkZip(downloadFile, unzip);
            // 开启
            OutGivingRun.startRun(outGivingModel.getId(), downloadFile, getUser(), unzip);
            return JsonMessage.getString(200, "分发成功");
        } catch (Exception e) {
            log.error("下载远程文件异常", e);
            return JsonMessage.getString(500, "下载远程文件失败:" + e.getMessage());
        }
    }
}
