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
package io.jpom.build;

import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.http.HttpStatus;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.jcraft.jsch.Session;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.data.WorkspaceEnvVarModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.outgiving.OutGivingRun;
import io.jpom.service.node.NodeService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.service.system.WorkspaceEnvVarService;
import io.jpom.system.ConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * 发布管理
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public class ReleaseManage extends BaseBuild {

	private final UserModel userModel;
	private final int buildId;
	private final BuildExtraModule buildExtraModule;
	private final File resultFile;
	private BaseBuild baseBuild;

	/**
	 * new ReleaseManage constructor
	 *
	 * @param buildModel 构建信息
	 * @param userModel  用户信息
	 * @param baseBuild  基础构建
	 * @param buildId    构建序号ID
	 */
	ReleaseManage(BuildExtraModule buildModel, UserModel userModel, BaseBuild baseBuild, int buildId) {
		super(BuildUtil.getLogFile(buildModel.getId(), buildId),
				buildModel.getId());
		this.buildExtraModule = buildModel;
		this.buildId = buildId;
		this.userModel = userModel;
		this.baseBuild = baseBuild;
		this.resultFile = BuildUtil.getHistoryPackageFile(this.buildModelId, this.buildId, buildModel.getResultDirFile());
	}

	/**
	 * 重新发布
	 *
	 * @param buildHistoryLog 构建历史
	 * @param userModel       用户
	 */
	public ReleaseManage(BuildHistoryLog buildHistoryLog, UserModel userModel) {
		super(BuildUtil.getLogFile(buildHistoryLog.getBuildDataId(), buildHistoryLog.getBuildNumberId()),
				buildHistoryLog.getBuildDataId());
		this.buildExtraModule = new BuildExtraModule();
		this.buildExtraModule.updateValue(buildHistoryLog);

		this.buildId = buildHistoryLog.getBuildNumberId();
		this.userModel = userModel;
		this.resultFile = BuildUtil.getHistoryPackageFile(this.buildModelId, this.buildId, buildHistoryLog.getResultDirFile());
	}


	@Override
	public boolean updateStatus(BuildStatus status) {
		if (baseBuild == null) {
			return super.updateStatus(status);
		} else {
			return baseBuild.updateStatus(status);
		}
	}

	/**
	 * 不修改为发布中状态
	 */
	public void start2() {
		this.log("start release：" + FileUtil.readableFileSize(FileUtil.size(this.resultFile)));
		if (!this.resultFile.exists()) {
			this.log("不存在构建产物");
			updateStatus(BuildStatus.PubError);
			return;
		}
		long time = SystemClock.now();
		int releaseMethod = this.buildExtraModule.getReleaseMethod();
		this.log("release method:" + BaseEnum.getDescByCode(BuildReleaseMethod.class, releaseMethod));
		try {
			if (releaseMethod == BuildReleaseMethod.Outgiving.getCode()) {
				//
				this.doOutGiving();
			} else if (releaseMethod == BuildReleaseMethod.Project.getCode()) {
				AfterOpt afterOpt = BaseEnum.getEnum(AfterOpt.class, this.buildExtraModule.getAfterOpt(), AfterOpt.No);
				this.doProject(afterOpt, this.buildExtraModule.isClearOld(), this.buildExtraModule.isDiffSync());
			} else if (releaseMethod == BuildReleaseMethod.Ssh.getCode()) {
				this.doSsh();
			} else if (releaseMethod == BuildReleaseMethod.LocalCommand.getCode()) {
				this.localCommand();
			} else {
				this.log(" 没有实现的发布分发:" + releaseMethod);
			}
		} catch (Exception e) {
			this.pubLog("发布异常", e);
			return;
		}
		this.log("release complete : " + DateUtil.formatBetween(SystemClock.now() - time, BetweenFormatter.Level.MILLISECOND));
		updateStatus(BuildStatus.PubSuccess);
	}

	/**
	 * 修改为发布中状态
	 */
	public void start() {
		updateStatus(BuildStatus.PubIng);
		this.start2();
	}

	/**
	 * 格式化命令模版
	 *
	 * @param commands 命令
	 */
	private void formatCommand(String[] commands) {
		//
		WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
		WorkspaceEnvVarModel workspaceEnvVarModel = new WorkspaceEnvVarModel();
		workspaceEnvVarModel.setWorkspaceId(this.buildExtraModule.getWorkspaceId());
		List<WorkspaceEnvVarModel> list = workspaceEnvVarService.listByBean(workspaceEnvVarModel);
		for (int i = 0; i < commands.length; i++) {
			commands[i] = this.formatCommandItem(commands[i], list);
		}
	}

	/**
	 * 格式化命令模版
	 *
	 * @param command 命令
	 * @param list    工作空间变量列表
	 * @return 格式化后
	 */
	private String formatCommandItem(String command, List<WorkspaceEnvVarModel> list) {
		String replace = StrUtil.replace(command, "#{BUILD_ID}", this.buildModelId);
		replace = StrUtil.replace(replace, "#{BUILD_NAME}", this.buildExtraModule.getName());
		replace = StrUtil.replace(replace, "#{BUILD_RESULT_FILE}", FileUtil.getAbsolutePath(this.resultFile));
		replace = StrUtil.replace(replace, "#{BUILD_NUMBER_ID}", this.buildId + StrUtil.EMPTY);

		if (list != null) {
			for (WorkspaceEnvVarModel envVarModel : list) {
				replace = StrUtil.replace(replace, StrUtil.format("#{{}}", envVarModel.getName()), envVarModel.getValue());
			}
		}
		return replace;
	}

	/**
	 * 本地命令执行
	 */
	private void localCommand() {
		// 执行命令
		String[] commands = StrUtil.splitToArray(this.buildExtraModule.getReleaseCommand(), StrUtil.LF);
		if (ArrayUtil.isEmpty(commands)) {
			this.log("没有需要执行的ssh命令");
			return;
		}
		String command = StrUtil.EMPTY;
		this.log(DateUtil.now() + " start exec");
		InputStream templateInputStream = null;
		try {
			templateInputStream = ResourceUtil.getStream("classpath:/bin/execTemplate." + CommandUtil.SUFFIX);
			if (templateInputStream == null) {
				this.log("系统中没有命令模版");
				return;
			}
			String sshExecTemplate = IoUtil.readUtf8(templateInputStream);
			StringBuilder stringBuilder = new StringBuilder(sshExecTemplate);
			// 替换变量
			this.formatCommand(commands);
			//
			stringBuilder.append(ArrayUtil.join(commands, StrUtil.LF));
			File tempPath = ConfigBean.getInstance().getTempPath();
			File commandFile = FileUtil.file(tempPath, "build", this.buildModelId + StrUtil.DOT + CommandUtil.SUFFIX);
			FileUtil.writeUtf8String(stringBuilder.toString(), commandFile);
			//
			command = SystemUtil.getOsInfo().isWindows() ? StrUtil.EMPTY : CommandUtil.SUFFIX;
			command += " " + FileUtil.getAbsolutePath(commandFile);
			String result = CommandUtil.execSystemCommand(command);
			this.log(result);
		} catch (Exception e) {
			this.pubLog("执行本地命令异常：" + command, e);
		} finally {
			IoUtil.close(templateInputStream);
		}
	}

	/**
	 * ssh 发布
	 */
	private void doSsh() {
		String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
		SshService sshService = SpringUtil.getBean(SshService.class);
		SshModel item = sshService.getByKey(releaseMethodDataId, false);
		if (item == null) {
			this.log("没有找到对应的ssh项：" + releaseMethodDataId);
			return;
		}
		Session session = SshService.getSessionByModel(item);
		try {
			try (Sftp sftp = new Sftp(session, item.getCharsetT())) {
				if (this.buildExtraModule.isClearOld() && StrUtil.isNotEmpty(this.buildExtraModule.getReleasePath())) {
					try {
						sftp.delDir(this.buildExtraModule.getReleasePath());
					} catch (Exception e) {
						this.pubLog("清除构建产物失败", e);
					}
				}
				String prefix = "";
				if (!StrUtil.startWith(this.buildExtraModule.getReleasePath(), StrUtil.SLASH)) {
					prefix = sftp.pwd();
				}
				String normalizePath = FileUtil.normalize(prefix + StrUtil.SLASH + this.buildExtraModule.getReleasePath());
				sftp.syncUpload(this.resultFile, normalizePath);
			} catch (Exception e) {
				this.pubLog("执行ssh发布异常", e);
			}
		} finally {
			JschUtil.close(session);
		}
		this.log("");
		// 执行命令
		String[] commands = StrUtil.splitToArray(this.buildExtraModule.getReleaseCommand(), StrUtil.LF);
		if (commands == null || commands.length <= 0) {
			this.log("没有需要执行的ssh命令");
			return;
		}
		// 替换变量
		this.formatCommand(commands);
		//
		this.log(DateUtil.now() + " start exec");
		try {
			String s = sshService.exec(item, commands);
			this.log(s);
		} catch (Exception e) {
			this.pubLog("执行异常", e);
		}
	}

	private void diffSyncProject() {
		File resultFile = this.resultFile;
		List<File> files = FileUtil.loopFiles(resultFile);
		this.log("还没有实现");
	}

	/**
	 * 发布项目
	 *
	 * @param afterOpt 后续操作
	 */
	private void doProject(AfterOpt afterOpt, boolean clearOld, boolean diffSync) {
		String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
		String[] strings = StrUtil.splitToArray(releaseMethodDataId, CharPool.COLON);
		if (ArrayUtil.length(strings) != 2) {
			throw new IllegalArgumentException(releaseMethodDataId + " error");
		}
		NodeService nodeService = SpringUtil.getBean(NodeService.class);
		NodeModel nodeModel = nodeService.getByKey(strings[0]);
		Objects.requireNonNull(nodeModel, "节点不存在");
		if (diffSync) {
			this.diffSyncProject();
			return;
		}
		File zipFile = BuildUtil.isDirPackage(this.resultFile);
		boolean unZip = true;
		if (zipFile == null) {
			zipFile = this.resultFile;
			unZip = false;
		}
		JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(zipFile,
				strings[1],
				unZip,
				afterOpt,
				nodeModel, this.userModel, clearOld);
		if (jsonMessage.getCode() == HttpStatus.HTTP_OK) {
			this.log("发布项目包成功：" + jsonMessage);
		} else {
			throw new JpomRuntimeException("发布项目包失败：" + jsonMessage);
		}
	}

	/**
	 * 分发包
	 */
	private void doOutGiving() {
		String releaseMethodDataId = this.buildExtraModule.getReleaseMethodDataId();
		File zipFile = BuildUtil.isDirPackage(this.resultFile);
		boolean unZip = true;
		if (zipFile == null) {
			zipFile = this.resultFile;
			unZip = false;
		}
		OutGivingRun.startRun(releaseMethodDataId, zipFile, userModel, unZip);
		this.log("开始执行分发包啦,请到分发中查看当前状态");
	}


	/**
	 * 发布异常日志
	 *
	 * @param title     描述
	 * @param throwable 异常
	 */
	private void pubLog(String title, Throwable throwable) {
		log(title, throwable, BuildStatus.PubError);
	}
}
