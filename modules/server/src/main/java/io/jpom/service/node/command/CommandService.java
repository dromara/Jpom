package io.jpom.service.node.command;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.ChannelExec;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.CommandExecLogModel;
import io.jpom.model.data.CommandModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.ICron;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceEnvVarService;
import io.jpom.util.CronUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 命令管理
 *
 * @author : Arno
 * @since : 2021/12/6 22:11
 */
@Service
public class CommandService extends BaseWorkspaceService<CommandModel> implements ICron {

	private final SshService sshService;
	private final CommandExecLogService commandExecLogService;
	private final WorkspaceEnvVarService workspaceEnvVarService;

	public CommandService(SshService sshService,
						  CommandExecLogService commandExecLogService,
						  WorkspaceEnvVarService workspaceEnvVarService) {
		this.sshService = sshService;
		this.commandExecLogService = commandExecLogService;
		this.workspaceEnvVarService = workspaceEnvVarService;
	}

	@Override
	public void insert(CommandModel commandModel) {
		super.insert(commandModel);
		this.checkCron(commandModel);
	}

	@Override
	public int update(CommandModel commandModel) {
		int update = super.update(commandModel);
		if (update > 0) {
			this.checkCron(commandModel);
		}
		return update;
	}

	@Override
	public int updateById(CommandModel info) {
		int update = super.updateById(info);
		if (update > 0) {
			this.checkCron(info);
		}
		return update;
	}

	/**
	 * 检查定时任务 状态
	 *
	 * @param buildInfoModel 构建信息
	 */
	private void checkCron(CommandModel buildInfoModel) {
		String id = buildInfoModel.getId();
		String taskId = "ssh_command:" + id;
		String autoExecCron = buildInfoModel.getAutoExecCron();
		if (StrUtil.isEmpty(autoExecCron)) {
			CronUtils.remove(taskId);
			return;
		}
		DefaultSystemLog.getLog().debug("start ssh command cron {} {} {}", id, buildInfoModel.getName(), autoExecCron);
		CronUtils.upsert(taskId, autoExecCron, new CommandService.CronTask(id));
	}

	/**
	 * 开启定时构建任务
	 */
	@Override
	public int startCron() {
		String sql = "select * from " + super.getTableName() + " where autoExecCron is not null and autoExecCron <> ''";
		List<CommandModel> models = super.queryList(sql);
		if (models == null) {
			return 0;
		}
		for (CommandModel buildInfoModel : models) {
			this.checkCron(buildInfoModel);
		}
		return CollUtil.size(models);
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
				DefaultSystemLog.getLog().error("触发自动执行命令模版异常", e);
			} finally {
				BaseServerController.remove();
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
	private String executeBatch(CommandModel commandModel, String params, String nodes, int triggerExecType) {
		Assert.notNull(commandModel, "没有对应对命令");
		List<CommandModel.CommandParam> commandParams = CommandModel.params(params);
		List<String> sshIds = StrUtil.split(nodes, StrUtil.COMMA, true, true);
		Assert.notEmpty(sshIds, "请选择 ssh 节点");
		String batchId = IdUtil.fastSimpleUUID();
		for (String sshId : sshIds) {
			this.executeItem(commandModel, commandParams, sshId, batchId, triggerExecType);
		}
		return batchId;
	}

	/**
	 * 准备执行 某一个
	 *
	 * @param commandModel  命令模版
	 * @param commandParams 参数
	 * @param sshId         ssh id
	 * @param batchId       批次ID
	 */
	private void executeItem(CommandModel commandModel, List<CommandModel.CommandParam> commandParams, String sshId, String batchId, int triggerExecType) {
		SshModel sshModel = sshService.getByKey(sshId, false);
		Assert.notNull(sshModel, "不存在对应对ssh");
		CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
		commandExecLogModel.setCommandId(commandModel.getId());
		commandExecLogModel.setCommandName(commandModel.getName());
		commandExecLogModel.setBatchId(batchId);
		commandExecLogModel.setSshId(sshId);
		commandExecLogModel.setTriggerExecType(triggerExecType);
		commandExecLogModel.setSshName(sshModel.getName());
		commandExecLogModel.setStatus(CommandExecLogModel.Status.ING.getCode());
		// 拼接参数
		String commandParamsLine;
		if (commandParams != null) {
			commandExecLogModel.setParams(JSONObject.toJSONString(commandParams));
			commandParamsLine = commandParams.stream().map(CommandModel.CommandParam::getValue).collect(Collectors.joining(StrUtil.SPACE));
		} else {
			commandParamsLine = StrUtil.EMPTY;
		}
		commandExecLogService.insert(commandExecLogModel);

		ThreadUtil.execute(() -> {
			try {
				this.execute(commandModel, commandExecLogModel, sshModel, commandParamsLine);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("命令模版执行链接异常", e);
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
	 * @throws IOException io
	 */
	private void execute(CommandModel commandModel, CommandExecLogModel commandExecLogModel, SshModel sshModel, String commandParamsLine) throws IOException {
		String command = commandModel.getCommand();
		String[] commands = StrUtil.splitToArray(command, StrUtil.LF);
		//
		workspaceEnvVarService.formatCommand(commandModel.getWorkspaceId(), commands);
		//
		Charset charset = sshModel.getCharsetT();
		File file = commandExecLogModel.logFile();
		sshService.exec(sshModel, (s, session) -> {
			final ChannelExec channel = (ChannelExec) JschUtil.createChannel(session, ChannelType.EXEC);
			channel.setCommand(StrUtil.bytes(s + StrUtil.SPACE + commandParamsLine, charset));
			channel.setInputStream(null);
			BufferedOutputStream outputStream = FileUtil.getOutputStream(file);
			channel.setErrStream(outputStream, true);
			InputStream in = null;
			byte[] lineBytes = SystemUtil.getOsInfo().getLineSeparator().getBytes(charset);
			try {
				channel.connect();
				in = channel.getInputStream();
				IoUtil.readLines(in, charset, (LineHandler) line -> this.appendLine(outputStream, line, lineBytes, charset));
				// 更新状态
				this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.DONE);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("执行命令错误", e);
				// 更新状态
				this.updateStatus(commandExecLogModel.getId(), CommandExecLogModel.Status.ERROR);
				// 记录错误日志
				String stacktraceToString = ExceptionUtil.stacktraceToString(e);
				this.appendLine(outputStream, stacktraceToString, lineBytes, charset);
			} finally {
				IoUtil.close(in);
				IoUtil.close(outputStream);
				JschUtil.close(channel);
			}
			return null;
		}, commands);
	}

	/**
	 * 修改执行状态
	 *
	 * @param id     ID
	 * @param status 状态
	 */
	private void updateStatus(String id, CommandExecLogModel.Status status) {
		CommandExecLogModel commandExecLogModel = new CommandExecLogModel();
		commandExecLogModel.setId(id);
		commandExecLogModel.setStatus(status.getCode());
		commandExecLogService.update(commandExecLogModel);
	}

	/**
	 * 记录日志
	 *
	 * @param outputStream 文件输出流
	 * @param line         消息
	 * @param lineBytes    换行标记
	 * @param charset      编码格式
	 */
	private void appendLine(BufferedOutputStream outputStream, String line, byte[] lineBytes, Charset charset) {
		try {
			outputStream.write(line.getBytes(charset));
			outputStream.write(lineBytes);
			outputStream.flush();
		} catch (IOException e) {
			DefaultSystemLog.getLog().warn("command log append line:{}", e.getMessage());
		}
	}

}
