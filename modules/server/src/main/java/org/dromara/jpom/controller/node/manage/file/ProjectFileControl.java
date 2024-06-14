/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller.node.manage.file;

import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.validator.ValidatorItem;
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
 * @author bwcx_jzy
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
    public IJsonMessage<Object> getFileList(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_GetFileList);
    }

    /**
     * 上传文件
     *
     * @return json
     */
    @RequestMapping(value = "upload-sharding", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD, log = false)
    public IJsonMessage<String> uploadSharding(String sliceId) {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), I18nMessageUtil.get("i18n.invalid_shard_id.46fd"));
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
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), I18nMessageUtil.get("i18n.invalid_shard_id.46fd"));
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
    public IJsonMessage<Object> deleteFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_DeleteFile);
    }


    /**
     * 更新配置文件
     *
     * @return json
     */
    @PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public IJsonMessage<Object> updateConfigFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_UpdateConfigFile);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @GetMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
    public IJsonMessage<Object> readFile(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_ReadFile);
    }

    /**
     * 下载远程文件
     *
     * @return json
     */
    @GetMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.REMOTE_DOWNLOAD)
    public IJsonMessage<Object> remoteDownload(@ValidatorItem String url, HttpServletRequest request) {
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);
        return NodeForward.request(getNode(), request, NodeUrl.Manage_File_Remote_Download);
    }

    /**
     * 创建文件
     *
     * @return json
     */
    @GetMapping(value = "new_file_folder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public IJsonMessage<Object> newFileFolder(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_NEW_FILE_FOLDER);
    }


    /**
     * 修改文件名
     *
     * @return json
     */
    @GetMapping(value = "rename_file_folder", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public IJsonMessage<Object> renameFileFolder(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_RENAME_FILE_FOLDER);
    }

    /**
     * 复制文件
     *
     * @return json
     */
    @PostMapping(value = "copy", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public IJsonMessage<Object> copy(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_COPY);
    }

    /**
     * 压缩文件
     *
     * @return json
     */
    @PostMapping(value = "compress", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public IJsonMessage<Object> compress(HttpServletRequest request) {
        return NodeForward.request(getNode(), request, NodeUrl.MANAGE_FILE_COMPRESS);
    }

}
