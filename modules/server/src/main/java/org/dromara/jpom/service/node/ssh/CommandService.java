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
package org.dromara.jpom.service.node.ssh;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.system.SystemUtil;
import cn.keepbx.jpom.cron.ICron;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.BaseServerController;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.model.EnvironmentMapBuilder;
import org.dromara.jpom.model.data.CommandExecLogModel;
import org.dromara.jpom.model.data.CommandModel;
import org.dromara.jpom.model.data.SshModel;
import org.dromara.jpom.model.user.UserModel;
import org.dromara.jpom.plugins.JschUtils;
import org.dromara.jpom.script.CommandParam;
import org.dromara.jpom.service.ITriggerToken;
import org.dromara.jpom.service.h2db.BaseWorkspaceService;
import org.dromara.jpom.service.system.WorkspaceEnvVarService;
import org.dromara.jpom.util.LogRecorder;
import org.dromara.jpom.util.StrictSyncFinisher;
import org.dromara.jpom.util.StringUtil;
import org.dromara.jpom.util.SyncFinisherUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 命令管理
 *
 * @author : Arno
 * @since : 2021/12/6 22:11
 */
@Service
@Slf4j
public class CommandService extends BaseWorkspaceService<CommandModel> implements ICron<CommandModel>, ITriggerToken {

    private final SshService sshService;
    private final CommandExecLogService commandExecLogService;
    private final WorkspaceEnvVarService workspaceEnvVarService;

    private static final byte[] LINE_BYTES = SystemUtil.getOsInfo().getLineSeparator().getBytes(CharsetUtil.CHARSET_UTF_8);

    public CommandService(SshService sshService,
                          CommandExecLogService commandExecLogService,
                          WorkspaceEnvVarService workspaceEnvVarService) {
        this.sshService = sshService;
        this.commandExecLogService = commandExecLogService;
        this.workspaceEnvVarService = workspaceEnvVarService;
    }

    @Override
    public int insert(CommandModel commandModel) {
        int count = super.insert(commandModel);
        this.checkCron(commandModel);
        return count;
    }

    @Override
    public int updateById(CommandModel info, HttpServletRequest request) {
        int update = super.updateById(info, request);
        if (update > 0) {
            this.checkCron(info);
        }
        return update;
    }

    @Override
    public int delByKey(String keyValue, HttpServletRequest request) {
        int delByKey = super.delByKey(keyValue, request);
        if (delByKey > 0) {
            String taskId = "ssh_command:" + keyValue;
            CronUtils.remove(taskId);
        }
        return delByKey;
    }

    /**
     * 检查定时任务 状态
     *
     * @param buildInfoModel 构建信息
     */
    @Override
    public boolean checkCron(CommandModel buildInfoModel) {
        String id = buildInfoModel.getId();
        String taskId = "ssh_command:" + id;
        String autoExecCron = buildInfoModel.getAutoExecCron();
        autoExecCron = StringUtil.parseCron(autoExecCron);
        if (StrUtil.isEmpty(autoExecCron)) {
            CronUtils.remove(taskId);
            return false;
        }
        log.debug("start ssh command cron {} {} {}", id, buildInfoModel.getName(), autoExecCron);
        CronUtils.upsert(taskId, autoExecCron, new CommandService.CronTask(id));
        return true;
    }

    /**
     * 开启定时构建任务
     */
    @Override
    public List<CommandModel> queryStartingList() {
        String sql = "select * from " + super.getTableName() + " where autoExecCron is not null and autoExecCron <> ''";
        return super.queryList(sql);
    }

    @Override
    public String typeName() {
        return getTableName();
    }

    private class CronTask implements Task {

        private final String id;

        public CronTask(String id) {
            this.id = id;
        }

        @Override
        public void execute() {
            try {
                BaseServerController.resetInfo(UserModel.EMPTY);
                CommandModel commandModel = CommandService.this.getByKey(this.id);
                CommandService.this.executeBatch(commandModel, commandModel.getDefParams(), commandModel.getSshIds(), 1);
            } catch (Exception e) {
                log.error("触发自动执行命令模版异常", e);
            } finally {
                BaseServerController.removeEmpty();
            }
        }
    }

    /**
     * 批量执行命令
     *
     * @param id     命令 id
     * @param nodes  ssh节点
     * @param params 参数
     * @return 批次ID
     */
    public String executeBatch(String id, String params, String nodes) {
        CommandModel commandModel = this.getByKey(id);
        return this.executeBatch(commandModel, params, nodes, 0);
    }

    /**
     * 批量执行命令
     *
     * @param commandModel 命令模版
     * @param nodes        ssh节点
     * @param params       参数
     * @return 批次ID
     */
    public String executeBatch(CommandModel commandModel, String params, String nodes, int triggerExecType) {
        Assert.notNull(commandModel, "没有对应对命令");
        List<String> sshIds = StrUtil.split(nodes, StrUtil.COMMA, true, true);
        Assert.notEmpty(sshIds, "请选择 ssh 节点");
        String batchId = IdUtil.fastSimpleUUID();
        String name = "ssh-command-batch:" + batchId;
        StrictSyncFinisher syncFinisher = SyncFinisherUtil.create(name, sshIds.size());
        for (String sshId : sshIds) {
            this.executeItem(syncFinisher, commandModel, params, sshId, batchId, triggerExecType);
        }
        ThreadUtil.execute(() -> {
            try {
                syncFinisher.start();
            } catch (Exception e) {
                log.error("ssh 批量执行命令异常", e);
            } finally {
                SyncFinisherUtil.close(name);
            }
        });
        return batchId;
    }

    /**
     * 准备执行 某一个
     *
     * @param syncFinisher  线程同步器
     * @param commandModel  命令模版
     * @param commandParams 参数
     * @param sshId         ssh id
     * @param batchId       批次ID
     */
    private void executeItem(StrictSyncFinisher syncFinisher, CommandModel commandModel, String commandParams, String sshId, String batchId, int triggerExecType) {
        SshModel sshModel = sshService.getByKey(sshId, false);

        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
        commandExecLogModel.setCommandId(commandModel.getId());
        commandExecLogModel.setCommandName(commandModel.getName());
        commandExecLogModel.setBatchId(batchId);
        commandExecLogModel.setSshId(sshId);
        commandExecLogModel.setWorkspaceId(commandModel.getWorkspaceId());
        commandExecLogModel.setTriggerExecType(triggerExecType);
        if (sshModel != null) {
            commandExecLogModel.setSshName(sshModel.getName());
        }
        commandExecLogModel.setStatus(CommandExecLogModel.Status.ING.getCode());
        // 拼接参数
        String commandParamsLine = CommandParam.toCommandLine(commandParams);
        commandExecLogService.insert(commandExecLogModel);

        syncFinisher.addWorker(() -> {
            try {
                this.execute(commandModel, commandExecLogModel, sshModel, commandParamsLine);
            } catch (Exception e) {
                log.error("命令模版执行链接异常", e);
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.SESSION_ERROR);
            }
        });
    }


    /**
     * 执行命令
     *
     * @param commandModel        命令模版
     * @param commandExecLogModel 执行记录
     * @param sshModel            ssh
     * @param commandParamsLine   参数
     */
    private void execute(CommandModel commandModel, CommandExecLogModel commandExecLogModel, SshModel sshModel, String commandParamsLine) {
        File file = commandExecLogModel.logFile();
        LogRecorder logRecorder = LogRecorder.builder().file(file).charset(CharsetUtil.CHARSET_UTF_8).build();
        if (sshModel == null) {
            logRecorder.systemError("ssh 不存在");
            return;
        }
        EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(commandModel.getWorkspaceId());
        environmentMapBuilder.put("JPOM_SSH_ID", sshModel.getId());
        environmentMapBuilder.put("JPOM_COMMAND_ID", commandModel.getId());
        environmentMapBuilder.eachStr(logRecorder::system);
        Map<String, String> environment = environmentMapBuilder.environment();
        String commands = StringUtil.formatStrByMap(commandModel.getCommand(), environment);

        MachineSshModel machineSshModel = sshService.getMachineSshModel(sshModel);
        //
        Session session = null;
        try {
            Charset charset = machineSshModel.charset();
            int timeout = machineSshModel.timeout();
            //
            session = sshService.getSessionByModel(machineSshModel);
            int exitCode = JschUtils.execCallbackLine(session, charset, timeout, commands, commandParamsLine, logRecorder::info);
            logRecorder.system("执行退出码：{}", exitCode);
            // 更新状态
            this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.DONE, exitCode);
        } catch (Exception e) {
            log.error("执行命令错误", e);
            // 更新状态
            this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.ERROR);
            // 记录错误日志
            String stacktraceToString = ExceptionUtil.stacktraceToString(e);
            logRecorder.systemError(stacktraceToString);
        } finally {
            JschUtil.close(session);
        }
    }

    /**
     * 修改执行状态
     *
     * @param id     ID
     * @param status 状态
     */
    private void updateStatus(String id, CommandExecLogModel.Status status) {
        this.updateStatus(id, status, null);
    }

    /**
     * 修改执行状态
     *
     * @param id       ID
     * @param status   状态
     * @param exitCode 退出码
     */
    private void updateStatus(String id, CommandExecLogModel.Status status, Integer exitCode) {
        CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
        commandExecLogModel.setId(id);
        commandExecLogModel.setExitCode(exitCode);
        commandExecLogModel.setStatus(status.getCode());
        commandExecLogService.updateById(commandExecLogModel);
    }

    /**
     * 将ssh 脚本信息同步到其他工作空间
     *
     * @param ids            多给节点ID
     * @param nowWorkspaceId 当前的工作空间ID
     * @param workspaceId    同步到哪个工作空间
     */
    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        StrUtil.splitTrim(ids, StrUtil.COMMA)
            .forEach(id -> {
                CommandModel data = super.getByKey(id, false, entity -> entity.set("workspaceId", nowWorkspaceId));
                Assert.notNull(data, "没有对应的ssh脚本信息");
                //
                CommandModel where = new CommandModel();
                where.setWorkspaceId(workspaceId);
                where.setName(data.getName());
                CommandModel exits = super.queryByBean(where);
                if (exits == null) {
                    // 不存在则添加 信息
                    data.setId(null);
                    data.setWorkspaceId(workspaceId);
                    data.setCreateTimeMillis(null);
                    data.setModifyTimeMillis(null);
                    data.setSshIds(null);
                    data.setModifyUser(null);
                    super.insert(data);
                } else {
                    // 修改信息
                    CommandModel update = new CommandModel();
                    update.setId(exits.getId());
                    update.setCommand(data.getCommand());
                    update.setDesc(data.getDesc());
                    update.setDefParams(data.getDefParams());
                    update.setAutoExecCron(data.getAutoExecCron());
                    super.updateById(update);
                }
            });
    }

}
