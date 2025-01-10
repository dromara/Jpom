/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.files.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.stream.CollectorUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.*;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.ServerConst;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.files.model.FileReleaseTaskLogModel;
import org.dromara.jpom.func.files.model.FileStorageModel;
import org.dromara.jpom.func.files.model.IFileStorage;
import org.dromara.jpom.func.files.model.StaticFileStorageModel;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.PageResultDto;
import org.dromara.jpom.model.data.NodeModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.script.ScriptModel;
import org.dromara.jpom.plugins.JschUtils;
import org.dromara.jpom.service.IStatusRecover;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.node.NodeService;
import org.dromara.jpom.service.node.ssh.SshService;
import org.dromara.jpom.service.script.ScriptServer;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.transport.*;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.MySftp;
import org.dromara.jpom.util.StrictSyncFinisher;
import org.dromara.jpom.util.SyncFinisherUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/18
 */
@Service
@Slf4j
public class FileReleaseTaskService extends BaseWorkspaceService<FileReleaseTaskLogModel> implements IStatusRecover {

    private final SshService sshService;
    private final JpomApplication jpomApplication;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final NodeService nodeService;
    private final BuildExtConfig buildExtConfig;
    private final ServerConfig serverConfig;
    private final FileStorageService fileStorageService;
    private final StaticFileStorageService staticFileStorageService;
    private final ScriptServer scriptServer;
    private final FileReleaseTaskTemplateService fileReleaseTaskTemplateService;

    private final Map<String, String> cancelTag = new SafeConcurrentHashMap<>();

    public FileReleaseTaskService(SshService sshService,
                                  JpomApplication jpomApplication,
                                  WorkspaceEnvVarService workspaceEnvVarService,
                                  NodeService nodeService,
                                  BuildExtConfig buildExtConfig,
                                  ServerConfig serverConfig,
                                  FileStorageService fileStorageService,
                                  StaticFileStorageService staticFileStorageService,
                                  ScriptServer scriptServer,
                                  FileReleaseTaskTemplateService fileReleaseTaskTemplateService) {
        this.sshService = sshService;
        this.jpomApplication = jpomApplication;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.nodeService = nodeService;
        this.buildExtConfig = buildExtConfig;
        this.serverConfig = serverConfig;
        this.fileStorageService = fileStorageService;
        this.staticFileStorageService = staticFileStorageService;
        this.scriptServer = scriptServer;
        this.fileReleaseTaskTemplateService = fileReleaseTaskTemplateService;
    }

    /**
     * 获取任务记录（只查看主任务）
     *
     * @param request 请求对象
     * @return page
     */
    @Override
    public PageResultDto<FileReleaseTaskLogModel> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = ServletUtil.getParamMap(request);
        String workspaceId = this.getCheckUserWorkspace(request);
        paramMap.put("workspaceId", workspaceId);
        paramMap.put("taskId", FileReleaseTaskLogModel.TASK_ROOT_ID);
        return super.listPage(paramMap);
    }

    /**
     * 获取文件中心的文件
     *
     * @param fileId  文件id
     * @param request 请求
     * @return tuple 0 文件 1 文件信息
     */
    private Tuple getFileStorage(String fileId, HttpServletRequest request) {
        FileStorageModel storageModel = fileStorageService.getByKey(fileId, request);
        Assert.notNull(storageModel, I18nMessageUtil.get("i18n.file_not_exist.ea6a"));
        File storageSavePath = serverConfig.fileStorageSavePath();
        File file = FileUtil.file(storageSavePath, storageModel.getPath());
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.file_missing_cannot_publish.3818"));

        return new Tuple(file, storageModel);
    }


    /**
     * 获取静态文件中心的文件
     *
     * @param fileId  文件id
     * @param request 请求
     * @return tuple 0 文件 1 文件信息
     */
    private Tuple getStaticFileStorage(String fileId, HttpServletRequest request) {
        StaticFileStorageModel storageModel = staticFileStorageService.getByKey(fileId);
        String workspaceId = getWorkspaceId(request);
        staticFileStorageService.checkStaticDir(storageModel, workspaceId);
        File file = FileUtil.file(storageModel.getAbsolutePath());
        Assert.state(FileUtil.isFile(file), I18nMessageUtil.get("i18n.file_missing_cannot_publish.3818"));
        return new Tuple(file, storageModel);
    }

    /**
     * 创建任务
     *
     * @param fileId       文件id
     * @param name         名称
     * @param taskType     任务类型
     * @param taskDataIds  任务关联的数据id
     * @param releasePath  发布目录
     * @param beforeScript 发布前脚本
     * @param afterScript  发布后的脚本
     * @param request      请求
     * @return fileStorage
     */
    public IFileStorage addTask(String fileId,
                                Integer fileType,
                                String name,
                                int taskType,
                                String taskDataIds,
                                String releasePath,
                                String beforeScript,
                                String afterScript,
                                Map<String, String> env,
                                HttpServletRequest request) {
        Tuple tuple;
        switch (fileType) {
            case 1:
                tuple = this.getFileStorage(fileId, request);
                break;
            case 2:
                tuple = this.getStaticFileStorage(fileId, request);
                break;
            default:
                throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_type_with_colon2.7de2") + fileType);
        }
        File file = tuple.get(0);
        IFileStorage storageModel = tuple.get(1);
        //
        List<String> list;
        if (taskType == 0) {
            list = StrUtil.splitTrim(taskDataIds, StrUtil.COMMA);
            list = list.stream().filter(s -> sshService.exists(new SshModel(s))).collect(Collectors.toList());
            Assert.notEmpty(list, I18nMessageUtil.get("i18n.select_correct_ssh.aa93"));
        } else if (taskType == 1) {
            list = StrUtil.splitTrim(taskDataIds, StrUtil.COMMA);
            list = list.stream().filter(s -> nodeService.exists(new NodeModel(s))).collect(Collectors.toList());
            Assert.notEmpty(list, I18nMessageUtil.get("i18n.select_correct_node.1b4e"));
        } else {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_method.a1de"));
        }
        // 生成任务id
        FileReleaseTaskLogModel taskRoot = new FileReleaseTaskLogModel();
        taskRoot.setId(IdUtil.fastSimpleUUID());
        taskRoot.setTaskId(FileReleaseTaskLogModel.TASK_ROOT_ID);
        taskRoot.setTaskDataId(FileReleaseTaskLogModel.TASK_ROOT_ID);
        taskRoot.setName(name);
        taskRoot.setFileId(fileId);
        taskRoot.setFileType(fileType);
        taskRoot.setStatus(0);
        taskRoot.setTaskType(taskType);
        taskRoot.setReleasePath(releasePath);
        taskRoot.setAfterScript(afterScript);
        taskRoot.setBeforeScript(beforeScript);
        this.insert(taskRoot);
        // 子任务列表
        for (String dataId : list) {
            FileReleaseTaskLogModel releaseTaskLogModel = new FileReleaseTaskLogModel();
            releaseTaskLogModel.setTaskId(taskRoot.getId());
            releaseTaskLogModel.setTaskDataId(dataId);
            releaseTaskLogModel.setName(name);
            releaseTaskLogModel.setFileId(fileId);
            releaseTaskLogModel.setFileType(fileType);
            releaseTaskLogModel.setStatus(0);
            releaseTaskLogModel.setTaskType(taskType);
            releaseTaskLogModel.setReleasePath(taskRoot.getReleasePath());
            this.insert(releaseTaskLogModel);
        }
        this.startTask(taskRoot.getId(), file, env, storageModel);
        return storageModel;
    }

    /**
     * 开始任务d
     *
     * @param taskId          任务id
     * @param storageSaveFile 文件
     */
    private void startTask(String taskId, File storageSaveFile, Map<String, String> env, IFileStorage storageModel) {
        FileReleaseTaskLogModel taskRoot = this.getByKey(taskId);
        Assert.notNull(taskRoot, I18nMessageUtil.get("i18n.parent_task_not_found.bac1"));
        //
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskId);
        List<FileReleaseTaskLogModel> logModels = this.listByBean(fileReleaseTaskLogModel);
        Assert.notEmpty(logModels, I18nMessageUtil.get("i18n.no_corresponding_task.3be5"));
        //
        EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(taskRoot.getWorkspaceId());
        Optional.ofNullable(env).ifPresent(environmentMapBuilder::putStr);
        environmentMapBuilder.put("TASK_ID", taskRoot.getTaskId());
        environmentMapBuilder.put("FILE_ID", taskRoot.getFileId());
        environmentMapBuilder.put("FILE_NAME", storageModel.getName());
        environmentMapBuilder.put("FILE_EXT_NAME", storageModel.getExtName());
        environmentMapBuilder.put("RELEASE_PATH", taskRoot.getReleasePath());
        //
        String syncFinisherId = "file-release:" + taskId;
        StrictSyncFinisher strictSyncFinisher = SyncFinisherUtil.create(syncFinisherId, logModels.size());
        Integer taskType = taskRoot.getTaskType();
        if (taskType == 0) {
            crateTaskSshWork(logModels, strictSyncFinisher, taskRoot, environmentMapBuilder, storageSaveFile);
        } else if (taskType == 1) {
            // 节点
            crateTaskNodeWork(logModels, strictSyncFinisher, taskRoot, environmentMapBuilder, storageSaveFile, storageModel);
        } else {
            throw new IllegalArgumentException(I18nMessageUtil.get("i18n.unsupported_method.a1de"));
        }
        I18nThreadUtil.execute(() -> {
            try {
                strictSyncFinisher.start();
                if (cancelTag.containsKey(taskId)) {
                    // 任务来源被取消
                    this.cancelTaskUpdate(taskId);
                } else {
                    this.updateRootStatus(taskId, 2, I18nMessageUtil.get("i18n.normal_end.3bfe"));
                }
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.publish_task_execution_failed.b075"), e);
                updateRootStatus(taskId, 3, e.getMessage());
            } finally {
                SyncFinisherUtil.close(syncFinisherId);
                cancelTag.remove(taskId);
            }
        });
    }

    /**
     * 取消任务
     *
     * @param taskId 任务id
     */
    public void cancelTask(String taskId) {
        String syncFinisherId = "file-release:" + taskId;
        SyncFinisherUtil.cancel(syncFinisherId);
        // 异步线程无法标记 ,同步监听线程去操作
        cancelTag.put(taskId, taskId);
    }

    private void cancelTaskUpdate(String taskId) {
        // 将未完成的任务标记为取消
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setStatus(4);
        update.setStatusMsg(I18nMessageUtil.get("i18n.manual_cancel_task.e592"));
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create().set("taskId", taskId).set("status", CollUtil.newArrayList(0, 1));
        this.update(updateEntity, where);
        this.updateRootStatus(taskId, 4, I18nMessageUtil.get("i18n.manual_cancel_task.e592"));
    }

    /**
     * 创建 节点 发布任务
     *
     * @param values                需要发布的任务列表
     * @param strictSyncFinisher    线程同步器
     * @param taskRoot              任务
     * @param environmentMapBuilder 环境变量
     * @param storageSaveFile       文件
     */
    private void crateTaskNodeWork(Collection<FileReleaseTaskLogModel> values,
                                   StrictSyncFinisher strictSyncFinisher,
                                   FileReleaseTaskLogModel taskRoot,
                                   EnvironmentMapBuilder environmentMapBuilder,
                                   File storageSaveFile,
                                   IFileStorage storageModel) {
        String taskId = taskRoot.getId();
        for (FileReleaseTaskLogModel model : values) {
            model.setAfterScript(taskRoot.getAfterScript());
            model.setBeforeScript(taskRoot.getBeforeScript());
            strictSyncFinisher.addWorker(() -> {
                String modelId = model.getId();
                LogRecorder logRecorder = null;
                try {
                    this.updateStatus(taskId, modelId, 1, I18nMessageUtil.get("i18n.start_publishing_file.a14e"));
                    File logFile = logFile(model);
                    logRecorder = LogRecorder.builder().file(logFile).charset(CharsetUtil.CHARSET_UTF_8).build();
                    NodeModel item = nodeService.getByKey(model.getTaskDataId());
                    if (item == null) {
                        logRecorder.systemError(I18nMessageUtil.get("i18n.no_node_entry_found.b1ef"), model.getTaskDataId());
                        this.updateStatus(taskId, modelId, 3, StrUtil.format(I18nMessageUtil.get("i18n.no_node_entry_found.b1ef"), model.getTaskDataId()));
                        return;
                    }

                    String releasePath = model.getReleasePath();
                    String beforeScript = model.getBeforeScript();
                    if (StrUtil.isNotEmpty(beforeScript)) {
                        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_upload_pre_command.fb5c"));
                        if (StrUtil.startWith(beforeScript, ServerConst.REF_SCRIPT)) {
                            String scriptId = StrUtil.removePrefix(beforeScript, ServerConst.REF_SCRIPT);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
                            beforeScript = keyAndGlobal.getContext();
                            logRecorder.system(I18nMessageUtil.get("i18n.introducing_script_content.a55b"), keyAndGlobal.getName(), scriptId);
                        }
                        this.runNodeScript(beforeScript, item, logRecorder, modelId, environmentMapBuilder, releasePath);
                    }
                    logRecorder.system("{} start file upload", item.getName());
                    // 上传文件
                    JSONObject data = new JSONObject();
                    data.put("path", releasePath);
                    Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
                    String name = storageModel.getName();
                    name = StrUtil.wrapIfMissing(name, StrUtil.EMPTY, StrUtil.DOT + storageModel.getExtName());
                    LogRecorder finalLogRecorder = logRecorder;
                    JsonMessage<String> jsonMessage = NodeForward.requestSharding(item, NodeUrl.Manage_File_Upload_Sharding2, data, storageSaveFile, name,
                        sliceData -> {
                            sliceData.putAll(data);
                            return NodeForward.request(item, NodeUrl.Manage_File_Sharding_Merge2, sliceData);
                        },
                        (total, progressSize) -> {

                            double progressPercentage = Math.floor(((float) progressSize / total) * 100);
                            int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                            if (progressRangeList.add(progressRange)) {
                                String info = I18nMessageUtil.get("i18n.upload_progress_template.ac3f");
                                finalLogRecorder.system(info,
                                    FileUtil.readableFileSize(progressSize), FileUtil.readableFileSize(total),
                                    NumberUtil.formatPercent(((float) progressSize / total), 0));
                            }
                        });
                    if (!jsonMessage.success()) {
                        throw new IllegalStateException(I18nMessageUtil.get("i18n.file_upload_failed.462e") + jsonMessage);
                    }
                    logRecorder.system("{} file upload done", item.getName());

                    String afterScript = model.getAfterScript();
                    if (StrUtil.isNotEmpty(afterScript)) {
                        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_upload_post_command.1c1b"));
                        if (StrUtil.startWith(afterScript, ServerConst.REF_SCRIPT)) {
                            String scriptId = StrUtil.removePrefix(afterScript, ServerConst.REF_SCRIPT);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
                            afterScript = keyAndGlobal.getContext();
                            logRecorder.system(I18nMessageUtil.get("i18n.introducing_script_content.a55b"), keyAndGlobal.getName(), scriptId);
                        }
                        this.runNodeScript(afterScript, item, logRecorder, modelId, environmentMapBuilder, releasePath);
                    }
                    this.updateStatus(taskId, modelId, 2, I18nMessageUtil.get("i18n.publish_success.2fff"));
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.publish_task_execution_exception.c296"), e);
                    updateStatus(taskId, modelId, 3, e.getMessage());
                } finally {
                    IoUtil.close(logRecorder);
                }
            });
        }
    }

    /**
     * 执行节点脚本
     *
     * @param content               脚本内容
     * @param model                 节点
     * @param logRecorder           日志记录器
     * @param id                    任务id
     * @param environmentMapBuilder 环境变量
     * @param path                  执行路径
     * @throws IOException io
     */
    private void runNodeScript(String content, NodeModel model, LogRecorder logRecorder, String id, EnvironmentMapBuilder environmentMapBuilder, String path) throws IOException {
        INodeInfo nodeInfo = NodeForward.parseNodeInfo(model);
        IUrlItem urlItem = NodeForward.parseUrlItem(nodeInfo, model.getWorkspaceId(), NodeUrl.FreeScriptRun, DataContentType.FORM_URLENCODED);
        try (IProxyWebSocket proxySession = TransportServerFactory.get().websocket(nodeInfo, urlItem)) {
            proxySession.onMessage(s -> {
                if (StrUtil.equals(s, "JPOM_SYSTEM_TAG:" + id)) {
                    try {
                        proxySession.close();
                    } catch (IOException e) {
                        log.error(I18nMessageUtil.get("i18n.close_session_exception.3491"), e);
                        logRecorder.systemError(I18nMessageUtil.get("i18n.close_session_exception_with_detail.85f0"), e.getMessage());
                    }
                    return;
                }
                logRecorder.info(s);
            });
            // 等待链接
            proxySession.connectBlocking();
            // 发送操作消息
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tag", id);
            jsonObject.put("path", path);
            jsonObject.put("environment", environmentMapBuilder.toDataJson());
            jsonObject.put("content", content);
            proxySession.send(jsonObject.toString());
            // 阻塞
            while (proxySession.isConnected()) {
                ThreadUtil.sleep(500, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * 创建 ssh 发布任务
     *
     * @param values                需要发布的任务列表
     * @param strictSyncFinisher    线程同步器
     * @param taskRoot              任务
     * @param environmentMapBuilder 环境变量
     * @param storageSaveFile       文件
     */
    private void crateTaskSshWork(Collection<FileReleaseTaskLogModel> values,
                                  StrictSyncFinisher strictSyncFinisher,
                                  FileReleaseTaskLogModel taskRoot,
                                  EnvironmentMapBuilder environmentMapBuilder,
                                  File storageSaveFile) {
        String taskId = taskRoot.getId();
        for (FileReleaseTaskLogModel model : values) {
            model.setAfterScript(taskRoot.getAfterScript());
            model.setBeforeScript(taskRoot.getBeforeScript());
            strictSyncFinisher.addWorker(() -> {
                String modelId = model.getId();
                Session session = null;
                ChannelSftp channelSftp = null;
                LogRecorder logRecorder = null;
                try {
                    this.updateStatus(taskId, modelId, 1, I18nMessageUtil.get("i18n.start_publishing_file.a14e"));
                    File logFile = logFile(model);
                    logRecorder = LogRecorder.builder().file(logFile).charset(CharsetUtil.CHARSET_UTF_8).build();
                    SshModel item = sshService.getByKey(model.getTaskDataId());
                    if (item == null) {
                        logRecorder.systemError(I18nMessageUtil.get("i18n.no_ssh_entry_found.d0e1"), model.getTaskDataId());
                        this.updateStatus(taskId, modelId, 3, StrUtil.format(I18nMessageUtil.get("i18n.no_ssh_entry_found.d0e1"), model.getTaskDataId()));
                        return;
                    }
                    MachineSshModel machineSshModel = sshService.getMachineSshModel(item);
                    Charset charset = machineSshModel.charset();
                    int timeout = machineSshModel.timeout();
                    session = sshService.getSessionByModel(machineSshModel);
                    Map<String, String> environment = environmentMapBuilder.environment();
                    environmentMapBuilder.eachStr(logRecorder::system);
                    String beforeScript = model.getBeforeScript();
                    if (StrUtil.isNotEmpty(beforeScript)) {
                        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_upload_pre_command.fb5c"));
                        if (StrUtil.startWith(beforeScript, ServerConst.REF_SCRIPT)) {
                            String scriptId = StrUtil.removePrefix(beforeScript, ServerConst.REF_SCRIPT);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
                            beforeScript = keyAndGlobal.getContext();
                            logRecorder.system(I18nMessageUtil.get("i18n.introducing_script_content.a55b"), keyAndGlobal.getName(), scriptId);
                        }
                        JschUtils.execCallbackLine(session, charset, timeout, beforeScript, StrUtil.EMPTY, environment, logRecorder::info);
                    }
                    logRecorder.system("{} start ftp upload", item.getName());

                    MySftp.ProgressMonitor sftpProgressMonitor = sshService.createProgressMonitor(logRecorder);
                    // 不需要关闭资源，因为共用会话
                    MySftp sftp = new MySftp(session, charset, timeout, sftpProgressMonitor);
                    channelSftp = sftp.getClient();
                    String releasePath = model.getReleasePath();
                    sftp.syncUpload(storageSaveFile, releasePath);
                    logRecorder.system("{} ftp upload done", item.getName());

                    String afterScript = model.getAfterScript();
                    if (StrUtil.isNotEmpty(afterScript)) {
                        logRecorder.system(I18nMessageUtil.get("i18n.start_executing_upload_post_command.1c1b"));
                        if (StrUtil.startWith(afterScript, ServerConst.REF_SCRIPT)) {
                            String scriptId = StrUtil.removePrefix(afterScript, ServerConst.REF_SCRIPT);
                            ScriptModel keyAndGlobal = scriptServer.getByKey(scriptId);
                            Assert.notNull(keyAndGlobal, I18nMessageUtil.get("i18n.select_correct_script.ff2d"));
                            afterScript = keyAndGlobal.getContext();
                            logRecorder.system(I18nMessageUtil.get("i18n.introducing_script_content.a55b"), keyAndGlobal.getName(), scriptId);
                        }
                        JschUtils.execCallbackLine(session, charset, timeout, afterScript, StrUtil.EMPTY, environment, logRecorder::info);
                    }
                    this.updateStatus(taskId, modelId, 2, I18nMessageUtil.get("i18n.publish_success.2fff"));
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.publish_task_execution_exception.c296"), e);
                    updateStatus(taskId, modelId, 3, e.getMessage());
                } finally {
                    IoUtil.close(logRecorder);
                    JschUtil.close(channelSftp);
                    JschUtil.close(session);
                }
            });
        }
    }

    /**
     * 更新总任务信息（忽略多线程并发问题，因为最终的更新是单线程）
     *
     * @param taskId    任务ID
     * @param status    状态
     * @param statusMsg 状态描述
     */
    private void updateRootStatus(String taskId, int status, String statusMsg) {
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskId);
        List<FileReleaseTaskLogModel> logModels = this.listByBean(fileReleaseTaskLogModel);
        Map<Integer, List<FileReleaseTaskLogModel>> map = logModels.stream()
            .collect(CollectorUtil.groupingBy(logModel -> ObjectUtil.defaultIfNull(logModel.getStatus(), 0), Collectors.toList()));
        StringBuilder stringBuilder = new StringBuilder();
        //
        Opt.ofBlankAble(statusMsg).ifPresent(s -> stringBuilder.append(s).append(StrUtil.SPACE));
        Set<Map.Entry<Integer, List<FileReleaseTaskLogModel>>> entries = map.entrySet();
        for (Map.Entry<Integer, List<FileReleaseTaskLogModel>> entry : entries) {
            Integer key = entry.getKey();
            // 0 等待开始 1 进行中 2 任务结束 3 失败
            switch (key) {
                case 0:
                    stringBuilder.append(I18nMessageUtil.get("i18n.waiting_to_start.b267"));
                    break;
                case 1:
                    stringBuilder.append(I18nMessageUtil.get("i18n.in_progress.b851"));
                    break;
                case 2:
                    stringBuilder.append(I18nMessageUtil.get("i18n.task_ended.b341"));
                    break;
                case 3:
                    stringBuilder.append(I18nMessageUtil.get("i18n.failure_prefix.115a"));
                    break;
                default:
                    stringBuilder.append(I18nMessageUtil.get("i18n.unknown_error.84d3"));
                    break;
            }
            stringBuilder.append(CollUtil.size(entry.getValue()));
        }
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setStatus(status);
        update.setId(taskId);
        update.setStatusMsg(stringBuilder.toString());
        this.updateById(update);
    }

    /**
     * 更新单给任务状态
     *
     * @param taskId    总任务
     * @param id        子任务id
     * @param status    状态
     * @param statusMsg 状态描述
     */
    private void updateStatus(String taskId, String id, int status, String statusMsg) {
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setId(id);
        fileReleaseTaskLogModel.setStatus(status);
        fileReleaseTaskLogModel.setStatusMsg(statusMsg);
        this.updateById(fileReleaseTaskLogModel);
        // 更新总任务
        updateRootStatus(taskId, 1, StrUtil.EMPTY);
    }

    public File logFile(FileReleaseTaskLogModel model) {
        return FileUtil.file(jpomApplication.getDataPath(), "file-release-log",
            model.getTaskId(),
            model.getId() + ".log"
        );
    }

    public File logTaskDir(FileReleaseTaskLogModel model) {
        return FileUtil.file(jpomApplication.getDataPath(), "file-release-log", model.getId());
    }

    @Override
    public int statusRecover() {
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setModifyTimeMillis(SystemClock.now());
        update.setStatus(4);
        update.setStatusMsg(I18nMessageUtil.get("i18n.system_cancel.3df2"));
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create()
            .set("status", CollUtil.newArrayList(0, 1));
        return this.update(updateEntity, where);
    }
}
