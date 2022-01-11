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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.BetweenFormatter;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import cn.hutool.http.HttpStatus;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.Session;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.SshModel;
import io.jpom.model.data.UserModel;
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
import io.jpom.util.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
		for (int i = 0; i < commands.length; i++) {
			commands[i] = this.formatCommandItem(commands[i]);
		}
		//
		WorkspaceEnvVarService workspaceEnvVarService = SpringUtil.getBean(WorkspaceEnvVarService.class);
		workspaceEnvVarService.formatCommand(this.buildExtraModule.getWorkspaceId(), commands);

	}

	/**
	 * 格式化命令模版
	 *
	 * @param command 命令
	 * @return 格式化后
	 */
	private String formatCommandItem(String command) {
		String replace = StrUtil.replace(command, "#{BUILD_ID}", this.buildModelId);
		replace = StrUtil.replace(replace, "#{BUILD_NAME}", this.buildExtraModule.getName());
		replace = StrUtil.replace(replace, "#{BUILD_RESULT_FILE}", FileUtil.getAbsolutePath(this.resultFile));
		replace = StrUtil.replace(replace, "#{BUILD_NUMBER_ID}", this.buildId + StrUtil.EMPTY);
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
						if (!StrUtil.startWithIgnoreCase(e.getMessage(), "No such file")) {
							this.pubLog("清除构建产物失败", e);
						}
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

	/**
	 * 差异上传发布
	 *
	 * @param nodeModel 节点
	 * @param projectId 项目ID
	 * @param afterOpt  发布后的操作
	 */
	private void diffSyncProject(NodeModel nodeModel, String projectId, AfterOpt afterOpt, boolean clearOld) {
		File resultFile = this.resultFile;
		String resultFileParent = resultFile.isFile() ?
				FileUtil.getAbsolutePath(resultFile.getParent()) : FileUtil.getAbsolutePath(this.resultFile);
		//
		List<File> files = FileUtil.loopFiles(resultFile);
		List<JSONObject> collect = files.stream().map(file -> {
			//
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", StringUtil.delStartPath(file, resultFileParent, true));
			jsonObject.put("sha1", SecureUtil.sha1(file));
			return jsonObject;
		}).collect(Collectors.toList());
		//
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", projectId);
		jsonObject.put("data", collect);
		JsonMessage<JSONObject> requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_DIFF_FILE, this.userModel, jsonObject);
		if (requestBody.getCode() != HttpStatus.HTTP_OK) {
			throw new JpomRuntimeException("对比项目文件失败：" + requestBody);
		}
		JSONObject data = requestBody.getData();
		JSONArray diff = data.getJSONArray("diff");
		JSONArray del = data.getJSONArray("del");
		int delSize = CollUtil.size(del);
		int diffSize = CollUtil.size(diff);
		if (clearOld) {
			this.log(StrUtil.format("对比文件结果,产物文件 {} 个、需要上传 {} 个、需要删除 {} 个", CollUtil.size(collect), CollUtil.size(diff), delSize));
		} else {
			this.log(StrUtil.format("对比文件结果,产物文件 {} 个、需要上传 {} 个", CollUtil.size(collect), CollUtil.size(diff)));
		}
		// 清空发布才先执行删除
		if (delSize > 0 && clearOld) {
			jsonObject.put("data", del);
			requestBody = NodeForward.requestBody(nodeModel, NodeUrl.MANAGE_FILE_BATCH_DELETE, this.userModel, jsonObject);
			if (requestBody.getCode() != HttpStatus.HTTP_OK) {
				throw new JpomRuntimeException("删除项目文件失败：" + requestBody);
			}
		}
		for (int i = 0; i < diffSize; i++) {
			boolean last = (i == diffSize - 1);
			JSONObject diffData = (JSONObject) diff.get(i);
			String name = diffData.getString("name");
			File file = FileUtil.file(resultFileParent, name);
			//
			String startPath = StringUtil.delStartPath(file, resultFileParent, false);
			//
			JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(file, startPath,
					projectId, false, last ? afterOpt : AfterOpt.No, nodeModel, this.userModel, false);
			if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
				throw new JpomRuntimeException("同步项目文件失败：" + jsonMessage);
			}
			if (last) {
				// 最后一个
				this.log("发布项目包成功：" + jsonMessage);
			}
		}
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
		String projectId = strings[1];
		if (diffSync) {
			this.diffSyncProject(nodeModel, projectId, afterOpt, clearOld);
			return;
		}
		File zipFile = BuildUtil.isDirPackage(this.resultFile);
		boolean unZip = true;
		if (zipFile == null) {
			zipFile = this.resultFile;
			unZip = false;
		}
		JsonMessage<String> jsonMessage = OutGivingRun.fileUpload(zipFile, null,
				projectId,
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
