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
package io.jpom.controller.node.manage.file;

import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.permission.NodeDataPermission;
import io.jpom.service.node.ProjectInfoCacheService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.UPLOAD)
    public String upload() {
        return NodeForward.requestMultipart(getNode(), getMultiRequest(), NodeUrl.Manage_File_Upload).toString();
    }

    /**
     * 下载文件
     */
    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DOWNLOAD)
    public void download() {
        NodeForward.requestDownload(getNode(), getRequest(), getResponse(), NodeUrl.Manage_File_Download);
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @RequestMapping(value = "deleteFile", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.DEL)
    public String deleteFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_DeleteFile).toString();
    }


    /**
     * 更新配置文件
     *
     * @return json
     */
    @PostMapping(value = "update_config_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.EDIT)
    public String updateConfigFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_UpdateConfigFile).toString();
    }

    /**
     * 删除文件
     *
     * @return json
     */
    @GetMapping(value = "read_file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.LIST)
    public String readFile() {
        return NodeForward.request(getNode(), getRequest(), NodeUrl.Manage_File_ReadFile).toString();
    }

    /**
     * 下载远程文件
     *
     * @return json
     */
    @GetMapping(value = "remote_download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.PROJECT_FILE, method = MethodFeature.REMOTE_DOWNLOAD)
    public String remoteDownload() {
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
