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
package org.dromara.jpom.func.files.controller;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import org.dromara.jpom.common.*;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.func.files.model.FileStorageModel;
import org.dromara.jpom.func.files.service.FileStorageService;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.service.user.TriggerTokenLogServer;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/3/16
 */
@RestController
@RequestMapping(value = "/file-storage")
@Feature(cls = ClassFeature.FILE_STORAGE)
public class FileStorageController extends BaseServerController {
    private final ServerConfig serverConfig;
    private final FileStorageService fileStorageService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public FileStorageController(ServerConfig serverConfig,
                                 FileStorageService fileStorageService,
                                 OutGivingWhitelistService outGivingWhitelistService,
                                 TriggerTokenLogServer triggerTokenLogServer) {
        this.serverConfig = serverConfig;
        this.fileStorageService = fileStorageService;
        this.outGivingWhitelistService = outGivingWhitelistService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<PageResultDto<FileStorageModel>> list(HttpServletRequest request) {
        File storageSavePath = serverConfig.fileStorageSavePath();
        //
        PageResultDto<FileStorageModel> listPage = fileStorageService.listPage(request);
        listPage.each(fileStorageModel -> {
            File file = FileUtil.file(storageSavePath, fileStorageModel.getPath());
            fileStorageModel.setExists(FileUtil.isFile(file));
        });
        return JsonMessage.success("", listPage);
    }

    /**
     * 判断是否存在文件
     *
     * @param fileSumMd5 文件 md5
     * @return json
     */
    @GetMapping(value = "has-file", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public JsonMessage<FileStorageModel> hasFile(@ValidatorItem String fileSumMd5) {
        FileStorageModel storageModel = fileStorageService.getByKey(fileSumMd5);
        return JsonMessage.success("", storageModel);
    }

    /**
     * 上传分片
     *
     * @param file       文件对象
     * @param sliceId    分片id
     * @param totalSlice 总分片
     * @param nowSlice   当前分片
     * @param fileSumMd5 文件 md5
     * @return json
     * @throws IOException io
     */
    @PostMapping(value = "upload-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public JsonMessage<String> uploadSharding(MultipartFile file,
                                              String sliceId,
                                              Integer totalSlice,
                                              Integer nowSlice,
                                              String fileSumMd5) throws IOException {
        File userTempPath = serverConfig.getUserTempPath();
        this.uploadSharding(file, userTempPath.getAbsolutePath(), sliceId, totalSlice, nowSlice, fileSumMd5);
        return JsonMessage.success("上传成功");
    }

    /**
     * 合并文件分片
     *
     * @param sliceId    分片id
     * @param totalSlice 增分片数
     * @param fileSumMd5 文件 md5
     * @return json
     * @throws IOException 异常
     */
    @PostMapping(value = "upload-sharding-merge", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public JsonMessage<String> uploadMerge(String sliceId,
                                           Integer totalSlice,
                                           String fileSumMd5,
                                           Integer keepDay,
                                           String description,
                                           String aliasCode,
                                           HttpServletRequest request) throws IOException {
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        File storageSavePath = serverConfig.fileStorageSavePath();
        // 验证文件
        FileStorageModel fileStorageModel1 = fileStorageService.getByKey(fileSumMd5);
        if (fileStorageModel1 != null) {
            // 如果存在记录，判断文件是否存在
            File file = FileUtil.file(storageSavePath, fileStorageModel1.getPath());
            Assert.state(!FileUtil.exist(file), "当前文件已经存在啦，请勿重复上传");
        }
        // 合并文件
        File userTempPath = serverConfig.getUserTempPath();
        File successFile = this.shardingTryMerge(userTempPath.getAbsolutePath(), sliceId, totalSlice, fileSumMd5);
        String extName = FileUtil.extName(successFile);
        String path = StrUtil.format("/{}/{}.{}", DateTime.now().toString(DatePattern.PURE_DATE_FORMAT), fileSumMd5, extName);

        File fileStorageFile = FileUtil.file(storageSavePath, path);
        FileUtil.mkParentDirs(fileStorageFile);
        FileUtil.move(successFile, fileStorageFile, true);
        // 保存
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(fileSumMd5);
        fileStorageModel.setName(successFile.getName());
        fileStorageModel.setDescription(description);
        fileStorageModel.setAliasCode(aliasCode);
        fileStorageModel.setExtName(extName);
        fileStorageModel.setPath(path);
        fileStorageModel.setSize(FileUtil.size(fileStorageFile));
        fileStorageModel.setSource(0);
        //
        fileStorageModel.setWorkspaceId(fileStorageService.covertGlobalWorkspace(request));
        fileStorageModel.validUntil(keepDay, null);
        //
        fileStorageService.insert(fileStorageModel);
        return JsonMessage.success("上传成功");
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<String> edit(@ValidatorItem String id,
                                    @ValidatorItem String name,
                                    Integer keepDay,
                                    String description,
                                    String aliasCode,
                                    HttpServletRequest request) throws IOException {
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        FileStorageModel storageModel = fileStorageService.getByKeyAndGlobal(id, request);

        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(id);
        fileStorageModel.setName(name);
        fileStorageModel.setAliasCode(aliasCode);
        fileStorageModel.setDescription(description);
        //
        fileStorageModel.setWorkspaceId(fileStorageService.covertGlobalWorkspace(request));
        //
        fileStorageModel.validUntil(keepDay, storageModel.getCreateTimeMillis());
        fileStorageService.updateById(fileStorageModel);
        return JsonMessage.success("修改成功");
    }

    @GetMapping(value = "del", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public JsonMessage<String> del(String id, String ids, HttpServletRequest request) throws IOException {
        this.delItem(id, request);
        List<String> list = StrUtil.splitTrim(ids, StrUtil.COMMA);
        for (String s : list) {
            this.delItem(s, request);
        }
        return JsonMessage.success("删除成功");
    }

    private void delItem(String id, HttpServletRequest request) {
        if (StrUtil.isEmpty(id)) {
            return;
        }
        FileStorageModel storageModel = fileStorageService.getByKeyAndGlobal(id, request);
        if (storageModel == null) {
            return;
        }
        //
        File storageSavePath = serverConfig.fileStorageSavePath();
        File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
        FileUtil.del(fileStorageFile);
        //
        fileStorageService.delByKey(id);
    }

    /**
     * 远程下载
     *
     * @param url         远程 url
     * @param keepDay     保留天数
     * @param description 描述
     * @param global      是否全局共享
     * @param request     请求
     * @return json
     * @throws IOException io
     */
    @PostMapping(value = "remote-download", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.REMOTE_DOWNLOAD)
    public JsonMessage<String> download(
        @ValidatorItem String url,
        Integer keepDay,
        String description,
        String aliasCode,
        Boolean global,
        HttpServletRequest request) throws IOException {
        Opt.ofBlankAble(aliasCode).ifPresent(s -> Validator.validateGeneral(s, "别名码只能是英文、数字"));
        // 验证远程 地址
        ServerWhitelist whitelist = outGivingWhitelistService.getServerWhitelistData(request);
        whitelist.checkAllowRemoteDownloadHost(url);
        String workspace = fileStorageService.getCheckUserWorkspace(request);
        fileStorageService.download(url, global, workspace, keepDay, description, aliasCode);
        return JsonMessage.success("开始异步下载");
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "trigger-url", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public JsonMessage<Map<String, String>> getTriggerUrl(@ValidatorItem String id, String rest, HttpServletRequest request) {
        UserModel user = getUser();
        // 查询当前工作空间
        FileStorageModel item = fileStorageService.getByKey(id, request);
        Assert.notNull(item, "没有对应的文件信息");
        //
        FileStorageModel updateInfo;
        if (StrUtil.isEmpty(item.getTriggerToken()) || StrUtil.isNotEmpty(rest)) {
            updateInfo = new FileStorageModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), fileStorageService.typeName(),
                item.getId(), user.getId()));
            fileStorageService.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        return JsonMessage.success(StrUtil.isEmpty(rest) ? "ok" : "重置成功", map);
    }

    private Map<String, String> getBuildToken(FileStorageModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        Map<String, String> map = new HashMap<>(10);
        {
            String url = ServerOpenApi.FILE_STORAGE_DOWNLOAD.
                replace("{id}", item.getId()).
                replace("{token}", item.getTriggerToken());
            String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
            map.put("triggerDownloadUrl", FileUtil.normalize(triggerBuildUrl));
        }
        if (StrUtil.isNotEmpty(item.getAliasCode())) {
            String url = ServerOpenApi.FILE_STORAGE_DOWNLOAD.
                replace("{id}", item.getAliasCode()).
                replace("{token}", item.getTriggerToken());
            String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
            map.put("triggerAliasDownloadUrl", FileUtil.normalize(triggerBuildUrl));
        }
        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }
}
