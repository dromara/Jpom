/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.service.node.ssh;

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
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.cron.CronUtils;
import org.dromara.jpom.dialect.DialectUtil;
import org.dromara.jpom.func.assets.model.MachineSshModel;
import org.dromara.jpom.func.assets.server.ScriptLibraryServer;
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
public class SshCommandService extends BaseWorkspaceService<CommandModel> implements ICron<CommandModel>, ITriggerToken {

    private final SshService sshService;
    private final CommandExecLogService commandExecLogService;
    private final WorkspaceEnvVarService workspaceEnvVarService;
    private final ScriptLibraryServer scriptLibraryServer;

    private static final byte[] LINE_BYTES = SystemUtil.getOsInfo().getLineSeparator().getBytes(CharsetUtil.CHARSET_UTF_8);

    public SshCommandService(SshService sshService,
                             CommandExecLogService commandExecLogService,
                             WorkspaceEnvVarService workspaceEnvVarService,
                             ScriptLibraryServer scriptLibraryServer) {
        this.sshService = sshService;
        this.commandExecLogService = commandExecLogService;
        this.workspaceEnvVarService = workspaceEnvVarService;
        this.scriptLibraryServer = scriptLibraryServer;
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
        CronUtils.upsert(taskId, autoExecCron, new SshCommandService.CronTask(id));
        return true;
    }

    /**
     * 开启定时构建任务
     */
    @Override
    public List<CommandModel> queryStartingList() {
        String autoExecCron = DialectUtil.wrapField("autoExecCron");
        String sql =StrUtil.format("select * from {} where {} is not null and {} <> ''",
            super.getTableName(),autoExecCron,autoExecCron);
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
                CommandModel commandModel = SshCommandService.this.getByKey(this.id);
                SshCommandService.this.executeBatch(commandModel, commandModel.getDefParams(), commandModel.getSshIds(), 1);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.trigger_auto_execute_command_template_exception.4e01"), e);
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
        return executeBatch(commandModel, params, nodes, triggerExecType, null);
    }

    /**
     * 批量执行命令
     *
     * @param commandModel    命令模版
     * @param nodes           ssh节点
     * @param params          参数
     * @param envMap          环境变量
     * @param triggerExecType 触发方式
     * @return 批次ID
     */
    public String executeBatch(CommandModel commandModel, String params, String nodes, int triggerExecType, Map<String, String> envMap) {
        Assert.notNull(commandModel, I18nMessageUtil.get("i18n.no_corresponding_command.165e"));
        List<String> sshIds = StrUtil.split(nodes, StrUtil.COMMA, true, true);
        Assert.notEmpty(sshIds, I18nMessageUtil.get("i18n.ssh_node_required.4566"));
        String batchId = IdUtil.fastSimpleUUID();
        String name = "ssh-command-batch:" + batchId;
        StrictSyncFinisher syncFinisher = SyncFinisherUtil.create(name, sshIds.size());
        for (String sshId : sshIds) {
            this.executeItem(syncFinisher, commandModel, params, sshId, batchId, triggerExecType, envMap);
        }
        I18nThreadUtil.execute(() -> {
            try {
                syncFinisher.start();
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.ssh_batch_command_execution_exception.029a"), e);
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
    private void executeItem(StrictSyncFinisher syncFinisher, CommandModel commandModel, String commandParams, String sshId, String batchId, int triggerExecType, Map<String, String> envMap) {
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
        } else {
            commandExecLogModel.setSshName(I18nMessageUtil.get("i18n.ssh_not_exist.08a2"));
        }
        commandExecLogModel.setStatus(CommandExecLogModel.Status.ING.getCode());
        // 拼接参数
        String commandParamsLine = CommandParam.toCommandLine(commandParams);
        commandExecLogService.insert(commandExecLogModel);

        syncFinisher.addWorker(() -> {
            try {
                this.execute(commandModel, commandExecLogModel, sshModel, commandParamsLine, envMap);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.command_template_execution_link_exception.51cf"), e);
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
    private void execute(CommandModel commandModel, CommandExecLogModel commandExecLogModel, SshModel sshModel, String commandParamsLine, Map<String, String> envMap) {
        File file = commandExecLogModel.logFile();
        try (LogRecorder logRecorder = LogRecorder.builder().file(file).charset(CharsetUtil.CHARSET_UTF_8).build()) {
            if (sshModel == null) {
                logRecorder.systemError(I18nMessageUtil.get("i18n.ssh_does_not_exist.88d7"));
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.ERROR, -100);
                return;
            }
            EnvironmentMapBuilder environmentMapBuilder = workspaceEnvVarService.getEnv(commandModel.getWorkspaceId());
            environmentMapBuilder.put("JPOM_SSH_ID", sshModel.getId());
            environmentMapBuilder.put("JPOM_COMMAND_ID", commandModel.getId());
            environmentMapBuilder.putStr(envMap);
            environmentMapBuilder.eachStr(logRecorder::system);
            Map<String, String> environment = environmentMapBuilder.environment();
            String commands = StringUtil.formatStrByMap(commandModel.getCommand(), environment);
            // 替换全局脚本
            commands = scriptLibraryServer.referenceReplace(commands);
            MachineSshModel machineSshModel = sshService.getMachineSshModel(sshModel);
            //
            Session session = null;
            try {
                Charset charset = machineSshModel.charset();
                int timeout = machineSshModel.timeout();
                //
                session = sshService.getSessionByModel(machineSshModel);
                int exitCode = JschUtils.execCallbackLine(session, charset, timeout, commands, commandParamsLine, logRecorder::info);
                logRecorder.system(I18nMessageUtil.get("i18n.exit_code.ea65"), exitCode);
                // 更新状态
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.DONE, exitCode);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.command_error.d0b4"), e);
                // 更新状态
                this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.ERROR);
                // 记录错误日志
                logRecorder.error(I18nMessageUtil.get("i18n.command_error.d0b4"), e);
            } finally {
                JschUtil.close(session);
            }
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
                Assert.notNull(data, I18nMessageUtil.get("i18n.no_corresponding_ssh_script_info.1c12"));
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
