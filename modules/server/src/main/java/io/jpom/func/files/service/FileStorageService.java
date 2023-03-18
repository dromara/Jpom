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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import io.jpom.JpomApplication;
import io.jpom.common.ISystemTask;
import io.jpom.common.ServerConst;
import io.jpom.func.files.model.FileStorageModel;
import io.jpom.service.IStatusRecover;
import io.jpom.service.ITriggerToken;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.system.ServerConfig;
import io.jpom.system.extconf.BuildExtConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @since 2023/3/16
 */
@Service
@Slf4j
public class FileStorageService extends BaseWorkspaceService<FileStorageModel> implements ISystemTask, IStatusRecover, ITriggerToken {

    private final ServerConfig serverConfig;
    private final JpomApplication configBean;
    private final BuildExtConfig buildExtConfig;

    public FileStorageService(ServerConfig serverConfig,
                              JpomApplication configBean,
                              BuildExtConfig buildExtConfig) {
        this.serverConfig = serverConfig;
        this.configBean = configBean;
        this.buildExtConfig = buildExtConfig;
    }

    @Override
    public FileStorageModel getByKey(String keyValue, HttpServletRequest request) {
        String workspace = this.getCheckUserWorkspace(request);
        return super.getByKey(keyValue, true, entity -> entity.set("workspaceId", CollUtil.newArrayList(workspace, ServerConst.WORKSPACE_GLOBAL)));
    }

    /**
     * 获取文件列表
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
     * 远程下载
     *
     * @param url         url
     * @param workspaceId 工作空间id
     * @param description 描述
     * @param global      是否为全局共享
     * @param keepDay     保留天数
     */
    public void download(String url, Boolean global, String workspaceId, Integer keepDay, String description) {
        FileStorageModel fileStorageModel = new FileStorageModel();
        // 临时使用 uuid，代替
        String uuid = IdUtil.fastSimpleUUID();
        long startTime = SystemClock.now();
        {
            fileStorageModel.setId(uuid);
            fileStorageModel.setName("文件下载中");
            String empty = StrUtil.emptyToDefault(description, StrUtil.EMPTY);

            fileStorageModel.setDescription(StrUtil.format("{} 远程下载 url：{}", empty, url));
            String extName = "download";
            String path = StrUtil.format("/{}/{}.{}", DateTime.now().toString(DatePattern.PURE_DATE_FORMAT), uuid, extName);
            fileStorageModel.setExtName("download");
            fileStorageModel.setPath(path);
            fileStorageModel.setSize(0L);
            fileStorageModel.setStatus(0);
            fileStorageModel.setSource(2);
            if (global != null && global) {
                fileStorageModel.setWorkspaceId(ServerConst.WORKSPACE_GLOBAL);
            } else {
                fileStorageModel.setWorkspaceId(workspaceId);
            }
            fileStorageModel.validUntil(keepDay, null);
            this.insert(fileStorageModel);
        }
        // 异步下载
        ThreadUtil.execute(() -> {
            try {
                File tempPath = configBean.getTempPath();
                File file = FileUtil.file(tempPath, "file-storage-download", uuid);
                FileUtil.mkdir(file);
                StreamProgress streamProgress = this.createStreamProgress(uuid);
                File fileFromUrl = HttpUtil.downloadFileFromUrl(url, file, -1, streamProgress);
                String md5 = SecureUtil.md5(fileFromUrl);
                FileStorageModel storageModel = this.getByKey(md5);
                if (storageModel != null) {
                    this.updateError(uuid, "文件已经存在啦");
                    FileUtil.del(fileFromUrl);
                    return;
                }
                String extName = FileUtil.extName(fileFromUrl);
                // 避免跨天数据
                String path = StrUtil.format("/{}/{}.{}", new DateTime(startTime).toString(DatePattern.PURE_DATE_FORMAT), md5, extName);
                File storageSavePath = serverConfig.fileStorageSavePath();
                File fileStorageFile = FileUtil.file(storageSavePath, path);
                FileUtil.mkParentDirs(fileStorageFile);
                FileUtil.move(fileFromUrl, fileStorageFile, true);
                //
                FileStorageModel update = new FileStorageModel();
                // 需要将 id 更新为真实 id
                update.setId(md5);
                update.setName(fileFromUrl.getName());
                update.setExtName(extName);
                update.setModifyTimeMillis(SystemClock.now());
                update.setPath(path);
                update.setStatus(1);
                update.setSize(FileUtil.size(fileStorageFile));
                Entity updateEntity = this.dataBeanToEntity(update);
                Entity id = Entity.create().set("id", uuid);
                this.update(updateEntity, id);
            } catch (Exception e) {
                log.error("下载文件失败", e);
                this.updateError(uuid, e.getMessage());
            }
        });
    }

    private StreamProgress createStreamProgress(String uuid) {
        int logReduceProgressRatio = buildExtConfig.getLogReduceProgressRatio();
        Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / logReduceProgressRatio));
        long bytes = DataSize.ofMegabytes(1).toBytes();
        return new StreamProgress() {
            @Override
            public void start() {

            }

            @Override
            public void progress(long total, long progressSize) {
                if (total > 0) {
                    double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                    String percent = NumberUtil.formatPercent((float) progressSize / total, 0);
                    int progressRange = (int) Math.floor(progressPercentage / logReduceProgressRatio);
                    // 存在文件总大小
                    if (progressRangeList.add(progressRange)) {
                        //  total, progressSize
                        updateProgress(uuid, percent, total, progressSize);
                    }
                } else {
                    // 不存在文件总大小
                    if (progressSize % bytes == 0) {
                        updateProgress(uuid, null, total, progressSize);
                    }
                }
            }

            @Override
            public void finish() {

            }
        };
    }

    private void updateProgress(String id, String desc, long total, long progressSize) {
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(id);
        String fileSize = FileUtil.readableFileSize(progressSize);
        desc = StrUtil.emptyToDefault(desc, fileSize);
        fileStorageModel.setName("文件下载中：" + desc);
        fileStorageModel.setStatus(0);
        fileStorageModel.setSize(progressSize);

        fileStorageModel.setProgressDesc(StrUtil.format("当前进度：{} ,文件总大小：{}，已经下载：{}", desc, FileUtil.readableFileSize(total), fileSize));
        this.updateById(fileStorageModel);
    }

    /**
     * 更新进度
     *
     * @param id    数据id
     * @param error 错误信息
     */
    private void updateError(String id, String error) {
        FileStorageModel fileStorageModel = new FileStorageModel();
        fileStorageModel.setId(id);
        fileStorageModel.setName("文件下载失败：" + StrUtil.maxLength(error, 200));
        fileStorageModel.setStatus(2);
        fileStorageModel.setProgressDesc(error);
        this.updateById(fileStorageModel);
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
        Entity entity = Entity.create();
        entity.set("validUntil", " < " + SystemClock.now());
        List<FileStorageModel> storageModels = this.listByEntity(entity);
        if (CollUtil.isEmpty(storageModels)) {
            return;
        }
        File storageSavePath = serverConfig.fileStorageSavePath();
        for (FileStorageModel storageModel : storageModels) {
            log.info("开始删除 {} 文件 {}", storageModel.getName(), storageModel.getPath());
            File fileStorageFile = FileUtil.file(storageSavePath, storageModel.getPath());
            FileUtil.del(fileStorageFile);
            this.delByKey(storageModel.getId());
        }
    }

    @Override
    public int statusRecover() {
        FileStorageModel update = new FileStorageModel();
        update.setName("系统重启取消下载任务");
        update.setModifyTimeMillis(SystemClock.now());
        update.setStatus(2);
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create()
            .set("source", 2)
            .set("status", 0);
        return this.update(updateEntity, where);
    }

    @Override
    public String typeName() {
        return getTableName();
    }
}
