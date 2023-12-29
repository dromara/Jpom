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
package org.dromara.jpom.func.files.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.core.io.watch.watchers.DelayWatcher;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.event.IAsyncLoad;
import cn.keepbx.jpom.model.BaseIdModel;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.configuration.FileStorageConfig;
import org.dromara.jpom.controller.outgiving.OutGivingWhitelistService;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.func.files.model.StaticFileStorageModel;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.ServerWhitelist;
import org.dromara.jpom.model.data.WorkspaceModel;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseDbService;
import org.dromara.jpom.service.system.WorkspaceService;
import org.dromara.jpom.system.ServerConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 23/12/28 028
 */
@Service
@Slf4j
public class StaticFileStorageService extends BaseDbService<StaticFileStorageModel> implements IAsyncLoad, Task, Watcher, DisposableBean, ITriggerToken {

    private final FileStorageConfig fileStorageConfig;
    private final WorkspaceService workspaceService;
    private final OutGivingWhitelistService outGivingWhitelistService;
    private Map<String, WatchMonitor> watchMonitor;
    /**
     * 扫描任务进行中
     */
    private volatile boolean scanning = false;

    public StaticFileStorageService(ServerConfig serverConfig,
                                    WorkspaceService workspaceService,
                                    OutGivingWhitelistService outGivingWhitelistService) {
        this.fileStorageConfig = serverConfig.getFileStorage();
        this.workspaceService = workspaceService;
        this.outGivingWhitelistService = outGivingWhitelistService;
    }

    /**
     * 是否在扫描中
     *
     * @return true 扫描中
     */
    public boolean isScanning() {
        return scanning;
    }

    /**
     * 分页查询
     *
     * @param request     请求对象
     * @param workspaceId 工作空间id
     * @return page
     */
    public PageResultDto<StaticFileStorageModel> listPage(HttpServletRequest request, String workspaceId) {
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        ServerWhitelist whitelistData = outGivingWhitelistService.getServerWhitelistData(workspaceId);
        List<String> staticDir = whitelistData.staticDir();
        if (CollUtil.isEmpty(staticDir)) {
            return new PageResultDto<>(1, 1, 0);
        }
        paramMap.remove("staticDir");
        paramMap.put("staticDir:in", CollUtil.join(staticDir, ","));
        return super.listPage(paramMap, true);
    }

    @Override
    public void startLoad() {
        String scanStaticDirCron = fileStorageConfig.getScanStaticDirCron();
        if (StrUtil.isEmpty(scanStaticDirCron)) {
            log.debug("未开启静态文件扫描");
            this.removeTask();
            return;
        }
        List<String> list = this.staticDir();
        if (CollUtil.isEmpty(list)) {
            log.debug("未配置静态目录");
            this.removeTask();
            return;
        }
        // 先关闭已经存在的监听器
        this.closeWatchMonitor();
        CronUtils.upsert("scan-static-dir", scanStaticDirCron, this);
        Boolean monitorStaticDir = fileStorageConfig.getWatchMonitorStaticDir();
        int watchMonitorMaxDepth = ObjectUtil.defaultIfNull(fileStorageConfig.getWatchMonitorMaxDepth(), 0);
        if (monitorStaticDir != null && monitorStaticDir) {
            // 开启任务
            watchMonitor = new HashMap<>(list.size());
            for (String s : list) {
                File file = FileUtil.file(s);
                if (!FileUtil.exist(file)) {
                    log.warn("被监控的目录不存在忽略创建监听器：{}", s);
                    // 自动删除已经存在的任务
                    this.delete(file);
                    continue;
                }
                DelayWatcher delayWatcher = new DelayWatcher(StaticFileStorageService.this, 1000);
                WatchMonitor monitor = WatchUtil.createAll(s, watchMonitorMaxDepth, delayWatcher);
                watchMonitor.put(s, monitor);
                // 开启任务
                ThreadUtil.execute(monitor);
            }
        }
    }

    /**
     * 关闭监听
     */
    private void closeWatchMonitor() {
        if (watchMonitor == null) {
            return;
        }
        for (WatchMonitor value : watchMonitor.values()) {
            IoUtil.close(value);
        }
        watchMonitor = null;
    }

    /**
     * 关闭任务
     */
    private void removeTask() {
        CronUtils.remove("scan-static-dir");
        this.closeWatchMonitor();
    }

    /**
     * 扫描指定工作空间
     *
     * @param workspaceId 工作空间id
     */
    public void scanByWorkspace(String workspaceId) {
        ServerWhitelist serverWhitelistData = outGivingWhitelistService.getServerWhitelistData(workspaceId);
        List<String> stringList = serverWhitelistData.staticDir();
        Assert.notEmpty(stringList, "还未配置静态目录");
        this.scanList(stringList);
    }

    /**
     * 扫描指定目录
     *
     * @param list 目录
     */
    private void scanList(List<String> list) {
        Snowflake snowflake = IdUtil.getSnowflake();
        long taskId = snowflake.nextId();
        for (String item : list) {
            // 开始扫描目录
            this.scanItem(item, item, 0, taskId);
            // 更新文件状态
            String sql = StrUtil.format("update {} set status=0 where staticDir=? and scanTaskId <> ?", this.getTableName());
            this.execute(sql, item, taskId);
        }
    }

    @Override
    public void execute() {
        try {
            this.scanning = true;
            List<String> list = this.staticDir();
            if (CollUtil.isEmpty(list)) {
                log.warn("当前没有配置静态目录，自动取消定时任务");
                this.removeTask();
                return;
            }
            this.scanList(list);
        } finally {
            this.scanning = false;
        }
    }

    /**
     * 扫描目录
     *
     * @param staticDir 静态目录
     * @param item      开始目录
     * @param level     目前层级
     * @param taskId    任务id
     */
    private void scanItem(String staticDir, String item, int level, Long taskId) {
        File file = FileUtil.file(item);
        if (!FileUtil.exist(file)) {
            // 目录不存在了，自动删除
            this.delete(file);
            return;
        }
        if (FileUtil.isFile(file)) {
            // 文件夹
            this.doFile(file, staticDir, level, taskId, 1);
        } else if (FileUtil.isDirectory(file)) {
            // 处理自身
            this.doFile(file, staticDir, level, taskId, 0);
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File subFile : files) {
                this.scanItem(staticDir, subFile.getAbsolutePath(), level + 1, taskId);
            }
        } else {
            log.warn("不支持的文件类型:{}", file.getAbsolutePath());
        }
    }

    /**
     * 处理文件缓存对象
     *
     * @param file      文件
     * @param staticDir 静态目录
     * @param level     层级
     * @param taskId    任务id
     * @param fileType  文件类型
     */
    private void doFile(File file, String staticDir, int level, Long taskId, int fileType) {
        String name = file.getName();
        String absolutePath = this.absNormalize(file);
        long length = file.length();
        long lastModified = file.lastModified();
        String parentAbsolutePath = this.absNormalize(file.getParentFile());
        if (StrUtil.length(absolutePath) > 500) {
            log.warn("文件目录长度超过 500 ，自动忽略此类文件：{}", absolutePath);
            return;
        }
        StaticFileStorageModel storageModel = new StaticFileStorageModel();
        storageModel.setId(SecureUtil.md5(absolutePath));
        storageModel.setName(name);
        storageModel.setAbsolutePath(absolutePath);
        storageModel.setParentAbsolutePath(parentAbsolutePath);
        storageModel.setLevel(level);
        storageModel.setStaticDir(staticDir);
        storageModel.setStatus(1);
        storageModel.setType(fileType);
        storageModel.setScanTaskId(taskId);
        storageModel.setExtName(FileUtil.extName(file));
        storageModel.setLastModified(lastModified);
        storageModel.setSize(length);
        this.upsert(storageModel);
    }

    private String absNormalize(File file) {
        String absolutePath = file.getAbsolutePath();
        return FileUtil.normalize(absolutePath);
    }

    /**
     * 处理文件缓存对象
     *
     * @param path          文件
     * @param staticDirPath 静态目录
     */
    private void doFile(Path path, Path staticDirPath) {
        try {
            File file = path.toFile();
            String absolutePath = this.absNormalize(file);
            File parentFile = file.getParentFile();
            String parentAbsolutePath = this.absNormalize(parentFile);
            if (StrUtil.length(absolutePath) > 500) {
                log.warn("文件目录长度超过 500 ，自动忽略此类文件：{}", absolutePath);
                return;
            }
            // 计算层级
            File staticDir = staticDirPath.toFile();
            String staticStr = this.absNormalize(staticDir);
            String subPath = FileUtil.subPath(staticStr, absolutePath);
            int level = CollUtil.size(StrUtil.splitTrim(subPath, StrUtil.SLASH));
            StaticFileStorageModel storageModel = new StaticFileStorageModel();
            storageModel.setId(SecureUtil.md5(absolutePath));
            storageModel.setName(file.getName());
            storageModel.setAbsolutePath(absolutePath);
            storageModel.setParentAbsolutePath(parentAbsolutePath);
            storageModel.setLevel(level + 1);
            storageModel.setStaticDir(staticStr);
            storageModel.setStatus(1);
            if (FileUtil.isFile(file)) {
                // 文件夹
                storageModel.setType(1);
            } else if (FileUtil.isDirectory(file)) {
                storageModel.setType(0);
            } else {
                log.warn("不支持的文件类型：{}", absolutePath);
                return;
            }
            storageModel.setExtName(FileUtil.extName(file));
            storageModel.setLastModified(file.lastModified());
            storageModel.setSize(file.length());
            this.upsert(storageModel);
            // 判断类型
            if (storageModel.getType() == 0) {
                // 文件夹类型
                // 需要扫描父级，可能避免删除无法正常监听到并且变更
                Snowflake snowflake = IdUtil.getSnowflake();
                long taskId = snowflake.nextId();
                this.scanItem(storageModel.getStaticDir(), absolutePath, level, taskId);
                // 删除未被更新的数据
                StaticFileStorageModel where = new StaticFileStorageModel();
                where.setParentAbsolutePath(absolutePath);
                List<StaticFileStorageModel> list = this.listByBean(where);
                if (CollUtil.isNotEmpty(list)) {
                    List<String> collect = list.stream()
                        .filter(staticFileStorageModel -> !ObjectUtil.equals(staticFileStorageModel.getScanTaskId(), taskId))
                        .filter(staticFileStorageModel -> {
                            File file1 = FileUtil.file(staticFileStorageModel.getAbsolutePath());
                            if (staticFileStorageModel.type() == 1) {
                                return !FileUtil.exist(file1);
                            }
                            // 删除子目录
                            this.delete(staticFileStorageModel.getId(), staticFileStorageModel.getAbsolutePath());
                            return !FileUtil.exist(file1);
                        })
                        .map(BaseIdModel::getId)
                        .collect(Collectors.toList());
                    this.update(Entity.create().set("status", 0), Entity.create().set("id", collect));
                }
            }
        } catch (Exception e) {
            log.error("处理文件事件异常", e);
        }
    }

    /**
     * 查询当前授权的静态目录
     *
     * @return list
     */
    private List<String> staticDir() {
        List<WorkspaceModel> list = workspaceService.list();
        if (list == null) {
            return null;
        }
        return list.stream()
            .map(workspaceModel -> outGivingWhitelistService.getServerWhitelistData(workspaceModel.getId()))
            .map(ServerWhitelist::staticDir)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 解下监听的 key
     *
     * @param path 路径
     * @return key
     */
    private WatchKey getWatchKey(Path path) {
        File file = path.toFile();
        String staticPath = this.absNormalize(file);
        WatchMonitor watchMonitor1 = MapUtil.get(watchMonitor, staticPath, WatchMonitor.class);
        Assert.notNull(watchMonitor1, "监听任务丢失");
        Map<WatchKey, Path> keyPathMap = (Map<WatchKey, Path>) ReflectUtil.getFieldValue(watchMonitor1, "watchKeyPathMap");
        Assert.notNull(keyPathMap, "没有找到监听的key");
        for (Map.Entry<WatchKey, Path> entry : keyPathMap.entrySet()) {
            if (entry.getValue().equals(path)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("没有找到监听 key");
    }

    /**
     * 解析事件全路径
     *
     * @param currentPath 监听路径
     * @param context     上下文
     * @return path
     */
    private Path fullPath(Path currentPath, Object context) {
        WatchKey watchKey = getWatchKey(currentPath);
        Path watchablePath = (Path) watchKey.watchable();
        return watchablePath.resolve((Path) context);
    }

    @Override
    public void onCreate(WatchEvent<?> event, Path currentPath) {
        log.debug("文件修改事件：{} {}", currentPath, event);
        this.onModify2(event, currentPath);
    }

    @Override
    public void onModify(WatchEvent<?> event, Path currentPath) {
        log.debug("文件修改事件：{} {}", currentPath, event);
        this.onModify2(event, currentPath);
    }

    private void onModify2(WatchEvent<?> event, Path currentPath) {
        Object context = event.context();
        if (context instanceof Path) {
            Path path = this.fullPath(currentPath, context);
            log.debug("文件全路径：{}", path);
            this.doFile(path, currentPath);
        } else {
            log.warn("不支持的事件类型：{}", context.getClass().getName());
        }
    }

    @Override
    public void onDelete(WatchEvent<?> event, Path currentPath) {
        log.debug("文件删除事件：{} {}", currentPath, event);
        Object context = event.context();
        if (context instanceof Path) {
            Path path = this.fullPath(currentPath, context);
            File file = path.toFile();
            try {
                // 处理文件删除事件
                this.delete(file);
            } catch (Exception e) {
                log.error("处理文件删除异常", e);
            }

        } else {
            log.warn("不支持的事件类型：{}", context.getClass().getName());
        }
    }

    /**
     * 删除文件
     *
     * @param file 文件
     */
    private void delete(File file) {
        String absolutePath = this.absNormalize(file);
        String md5 = SecureUtil.md5(absolutePath);
        this.delete(md5, absolutePath);
    }

    /**
     * 删除指定文件
     *
     * @param md5          文件id
     * @param absolutePath 文件路径
     */
    private void delete(String md5, String absolutePath) {
        StaticFileStorageModel storageModel = new StaticFileStorageModel();
        storageModel.setId(md5);
        storageModel.setStatus(0);
        int update = this.updateById(storageModel);
        log.debug("文件删除事件：{}", absolutePath);
        if (update > 0) {
            StaticFileStorageModel model = this.getByKey(md5);
            if (model != null && model.type() == 0) {
                // 文件夹
                StaticFileStorageModel where = new StaticFileStorageModel();
                where.setParentAbsolutePath(absolutePath);
                List<StaticFileStorageModel> list = this.listByBean(where);
                if (CollUtil.isNotEmpty(list)) {
                    List<String> collect = list.stream()
                        .filter(staticFileStorageModel -> {
                            if (staticFileStorageModel.type() == 1) {
                                return true;
                            }
                            // 删除子目录
                            this.delete(staticFileStorageModel.getId(), staticFileStorageModel.getAbsolutePath());
                            return false;
                        })
                        .map(BaseIdModel::getId)
                        .collect(Collectors.toList());
                    this.update(Entity.create().set("status", 0), Entity.create().set("id", collect));
                }
            }
        }
    }

    @Override
    public void onOverflow(WatchEvent<?> event, Path currentPath) {
        log.error("事件丢失或者执行错误：{} {}", currentPath, event.context());
    }

    @Override
    public void destroy() throws Exception {
        this.closeWatchMonitor();
    }

    @Override
    public String typeName() {
        return getTableName();
    }

    /**
     * 判断是否有权限操作
     *
     * @param storageModel 静态文件
     */
    public void checkStaticDir(StaticFileStorageModel storageModel, String workspaceId) {
        org.springframework.util.Assert.notNull(storageModel, "没有对应的文件信息");
        ServerWhitelist whitelistData = outGivingWhitelistService.getServerWhitelistData(workspaceId);
        whitelistData.checkStaticDir(storageModel.getStaticDir());
    }
}
