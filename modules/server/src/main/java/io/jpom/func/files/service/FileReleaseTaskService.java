package io.jpom.func.files.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.stream.CollectorUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.extra.servlet.ServletUtil;
import com.jcraft.jsch.Session;
import io.jpom.JpomApplication;
import io.jpom.func.assets.model.MachineSshModel;
import io.jpom.func.files.model.FileReleaseTaskLogModel;
import io.jpom.model.EnvironmentMapBuilder;
import io.jpom.model.data.SshModel;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceEnvVarService;
import io.jpom.util.LogRecorder;
import io.jpom.util.MySftp;
import io.jpom.util.StrictSyncFinisher;
import io.jpom.util.SyncFinisherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import top.jpom.model.PageResultDto;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @since 2023/3/18
 */
@Service
@Slf4j
public class FileReleaseTaskService extends BaseWorkspaceService<FileReleaseTaskLogModel> {

    private final SshService sshService;
    private final JpomApplication jpomApplication;
    private final WorkspaceEnvVarService workspaceEnvVarService;

    public FileReleaseTaskService(SshService sshService,
                                  JpomApplication jpomApplication,
                                  WorkspaceEnvVarService workspaceEnvVarService) {
        this.sshService = sshService;
        this.jpomApplication = jpomApplication;
        this.workspaceEnvVarService = workspaceEnvVarService;
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
     * 开始任务d
     *
     * @param taskId          任务id
     * @param storageSaveFile 文件
     */
    public void startTask(String taskId, File storageSaveFile) {
        FileReleaseTaskLogModel taskRoot = this.getByKey(taskId);
        Assert.notNull(taskRoot, "没有找到父级任务");
        //
        FileReleaseTaskLogModel fileReleaseTaskLogModel = new FileReleaseTaskLogModel();
        fileReleaseTaskLogModel.setTaskId(taskId);
        List<FileReleaseTaskLogModel> logModels = this.listByBean(fileReleaseTaskLogModel);
        Assert.notEmpty(logModels, "没有对应的任务");
        //
        EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(taskRoot.getWorkspaceId());
        environmentMapBuilder.put("TASK_ID", taskRoot.getTaskId());
        environmentMapBuilder.put("FILE_ID", taskRoot.getFileId());
        Map<String, String> environment = environmentMapBuilder.environment();
        //
        String syncFinisherId = "file-release:" + taskId;
        StrictSyncFinisher strictSyncFinisher = SyncFinisherUtil.create(syncFinisherId, logModels.size());
        Integer taskType = taskRoot.getTaskType();
        if (taskType == 0) {
            crateTaskSshWork(logModels, strictSyncFinisher, taskId, environment, storageSaveFile);
        } else {
            throw new IllegalArgumentException("不支持的方式");
        }
        ThreadUtil.execute(() -> {
            try {
                strictSyncFinisher.start();
                updateRootStatus(taskId, 2, "正常结束");
            } catch (Exception e) {
                log.error("执行发布任务失败", e);
                updateRootStatus(taskId, 3, e.getMessage());
            } finally {
                SyncFinisherUtil.close(syncFinisherId);
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
        // 将未完成的任务标记为取消
        FileReleaseTaskLogModel update = new FileReleaseTaskLogModel();
        update.setStatus(4);
        update.setStatusMsg("手动取消任务");
        Entity updateEntity = this.dataBeanToEntity(update);
        //
        Entity where = Entity.create().set("taskId", taskId).set("status", CollUtil.newArrayList(0, 1));
        this.update(updateEntity, where);
        this.updateRootStatus(taskId, 4, "手动取消任务");
    }

    /**
     * 创建 ssh 发布任务
     *
     * @param values             需要发布的任务列表
     * @param strictSyncFinisher 线程同步器
     * @param taskId             任务ID
     * @param environment        环境变量
     * @param storageSaveFile    文件
     */
    private void crateTaskSshWork(Collection<FileReleaseTaskLogModel> values,
                                  StrictSyncFinisher strictSyncFinisher,
                                  String taskId,
                                  Map<String, String> environment,
                                  File storageSaveFile) {
        for (FileReleaseTaskLogModel model : values) {
            strictSyncFinisher.addWorker(() -> {
                String modelId = model.getId();
                try {
                    this.updateStatus(taskId, modelId, 1, "开始发布文件");
                    File file = logFile(model);
                    LogRecorder logRecorder = LogRecorder.builder().file(file).charset(CharsetUtil.CHARSET_UTF_8).build();
                    SshModel item = sshService.getByKey(model.getTaskDataId());
                    if (item == null) {
                        logRecorder.systemError("没有找到对应的ssh项：{}", model.getTaskDataId());
                        this.updateStatus(taskId, modelId, 3, StrUtil.format("没有找到对应的ssh项：{}", model.getTaskDataId()));
                        return;
                    }
                    if (StrUtil.isNotEmpty(model.getBeforeScript())) {
                        logRecorder.system("开始执行上传前命令");
                        String s = sshService.exec(item, model.getBeforeScript(), environment);
                        logRecorder.info(s);
                    }
                    logRecorder.system("{} start ftp upload", item.getName());
                    MachineSshModel machineSshModel = sshService.getMachineSshModel(item);
                    Session session = sshService.getSessionByModel(machineSshModel);
                    MySftp.ProgressMonitor sftpProgressMonitor = sshService.createProgressMonitor(logRecorder);
                    try (MySftp sftp = new MySftp(session, machineSshModel.charset(), machineSshModel.timeout(), sftpProgressMonitor)) {
                        String releasePath = model.getReleasePath();
                        String prefix = "";
                        if (!StrUtil.startWith(releasePath, StrUtil.SLASH)) {
                            prefix = sftp.pwd();
                        }
                        String normalizePath = FileUtil.normalize(prefix + StrUtil.SLASH + releasePath);
                        sftp.syncUpload(storageSaveFile, normalizePath);
                        logRecorder.system("{} ftp upload done", item.getName());
                    }
                    if (StrUtil.isNotEmpty(model.getAfterScript())) {
                        logRecorder.system("开始执行上传后命令");
                        String s = sshService.exec(item, model.getAfterScript(), environment);
                        logRecorder.info(s);
                    }
                    this.updateStatus(taskId, modelId, 2, "发布成功");
                } catch (Exception e) {
                    log.error("执行发布任务异常", e);
                    updateStatus(taskId, modelId, 3, e.getMessage());
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
                    stringBuilder.append("等待开始:");
                    break;
                case 1:
                    stringBuilder.append("进行中:");
                    break;
                case 2:
                    stringBuilder.append("任务结束:");
                    break;
                case 3:
                    stringBuilder.append("失败:");
                    break;
                default:
                    stringBuilder.append("未知：");
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
}
