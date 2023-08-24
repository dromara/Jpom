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
package org.dromara.jpom.controller.node.manage.file;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.NodeDataPermission;
import org.dromara.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 文件管理
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/node/manage/file/")
@Feature(cls = ClassFeature.PROJECT_FILE)
@NodeDataPermission(cls = ProjectInfoCacheService.class)
public class ProjectFileControl extends BaseServerController {
    private final OutGivingWhitelistService outGivingWhitelistService;

    public ProjectFileControl(OutGivingWhitelistService outGivingWhitelistService) {
        this.outGivingWhitelistService = outGivingWhitelistService;
    }


    /**
     * 列出目录下的文件
     *
     * @return json
     */
    @RequestMapping(value = "getFileList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
    public String getFileList() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_GetFileList).toString();
    }

    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD, log = false)
    public IJsonMessage<String> uploadSharding(String sliceId) {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Manage_File_Upload_Sharding);
    }

    /**
     * 合并分片
     *
     * @return json
     */
    @RequestMapping(value = "sharding-merge", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD)
    public IJsonMessage<String> shardingMerge(String sliceId, HttpServletRequest request) {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        JsonMessage<String> message = NodeForward.request(getNode(), request, NodeUrl.Manage_File_Sharding_Merge);
        // 判断-删除分片id
        BaseServerController.SHARDING_IDS.remove(sliceId);
        return message;
    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DOWNLOAD)
    public void download(HttpServletRequest request, HttpServletResponse response) {
        NodeForward.requestDownload(getNode(), request, response, NodeUrl.Manage_File_Download);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DEL)
    public String deleteFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_DeleteFile).toString();
    }


    /**
     * 更新配置文件
     *
     * @return json
     */
    @PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public String updateConfigFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_UpdateConfigFile).toString();
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @GetMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
    public String readFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_ReadFile).toString();
    }

    /**
     * 下载远程文件
     *
     * @return json
     */
    @GetMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.REMOTE_DOWNLOAD)
    public String remoteDownload(String url, HttpServletRequest request) {
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_Remote_Download).toString();
    }

    /**
     * 创建文件
     *
     * @return json
     */
    @GetMapping(value = "new_file_folder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public String newFileFolder() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.MANAGE_FILE_NEW_FILE_FOLDER).toString();
    }


    /**
     * 修改文件名
     *
     * @return json
     */
    @GetMapping(value = "rename_file_folder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public String renameFileFolder() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.MANAGE_FILE_RENAME_FILE_FOLDER).toString();
    }

}
