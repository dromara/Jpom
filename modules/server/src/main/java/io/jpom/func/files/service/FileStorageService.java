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
package io.jpom.func.files.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.servlet.ServletUtil;
import io.jpom.common.ISystemTask;
import io.jpom.common.ServerConst;
import io.jpom.func.files.model.FileStorageModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.system.ServerConfig;
import org.springframework.stereotype.Service;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2023/3/16
 */
@Service
public class FileStorageService extends BaseWorkspaceService<FileStorageModel> implements ISystemTask {

    private final ServerConfig serverConfig;

    public FileStorageService(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 获取我所有的空间
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<FileStorageModel> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        paramMap.put("workspaceId:in", workspaceId + StrUtil.COMMA + ServerConst.WORKSPACE_GLOBAL);
        return super.listPage(paramMap);
    }

    /**
     * 添加文件
     *
     * @param source 文件来源
     * @param file   要文件的文件
     * @return 是否添加成功
     */
    public boolean addFile(File file, int source, String workspaceId, String description) {
        String md5 = SecureUtil.md5(file);
        File storageSavePath = serverConfig.fileStorageSavePath();
        String extName = FileUtil.extName(file);
        String path = StrUtil.format("/{}/{}.{}", DateTime.now().toString(DatePattern.PURE_DATE_FORMAT), md5, extName);
        FileStorageModel storageModel = this.getByKey(md5);
        if (storageModel != null) {
            return false;
        }
        // 保存
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(md5);
        fileStorageModel.setName(file.getName());
        fileStorageModel.setDescription("构建来源," + description);
        fileStorageModel.setExtName(extName);
        fileStorageModel.setPath(path);
        fileStorageModel.setSize(FileUtil.size(file));
        fileStorageModel.setSource(source);
        fileStorageModel.setWorkspaceId(workspaceId);
        fileStorageModel.validUntil(null, null);
        this.insert(fileStorageModel);
        //
        File fileStorageFile = FileUtil.file(storageSavePath, path);
        FileUtil.mkParentDirs(fileStorageFile);
        FileUtil.copyFile(file, fileStorageFile, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    @Override
    public void executeTask() {
        // 定时删除文件
    }
}
