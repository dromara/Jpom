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
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileCopier;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.BaseServerController;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.enums.BuildStatus;
import io.jpom.model.enums.GitProtocolEnum;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.plugin.DefaultPlugin;
import io.jpom.plugin.IPlugin;
import io.jpom.plugin.PluginFactory;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.system.ExtConfigBean;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import io.jpom.util.StringUtil;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * new build info manage runnable
 *
 * @author Hotstrip
 * @since 20210-08-23
 */
public class BuildInfoManage extends BaseBuild implements Runnable {
	/**
	 * 缓存构建中
	 */
	private static final Map<String, BuildInfoManage> BUILD_MANAGE_MAP = new ConcurrentHashMap<>();

	private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

	private final BuildInfoModel buildInfoModel;
	private final RepositoryModel repositoryModel;
	private final File gitFile;
	private Process process;
	private String logId;
	private final UserModel userModel;
	private final BuildExtraModule buildExtraModule;
	/**
	 * 延迟执行的时间（单位秒）
	 */
	private final Integer delay;
	/**
	 * 触发类型
	 */
	private final int triggerBuildType;

	private BuildInfoManage(final BuildInfoModel buildInfoModel,
							final RepositoryModel repositoryModel,
							final UserModel userModel,
							Integer delay,
							int triggerBuildType) {
		super(BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId()),
				buildInfoModel.getId());
		this.buildInfoModel = buildInfoModel;
		this.repositoryModel = repositoryModel;
		this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());
		this.userModel = userModel;
		this.triggerBuildType = triggerBuildType;
		this.delay = delay;
		// 解析 其他配置信息
		BuildExtraModule buildExtraModule = StringUtil.jsonConvert(this.buildInfoModel.getExtraData(), BuildExtraModule.class);
		Assert.notNull(buildExtraModule, "构建信息缺失");
		// update value
		buildExtraModule.updateValue(this.buildInfoModel);
		this.buildExtraModule = buildExtraModule;
	}

	/**
	 * 取消构建
	 *
	 * @param id id
	 * @return bool
	 */
	public static boolean cancel(String id) {
		BuildInfoManage buildInfoManage = BUILD_MANAGE_MAP.get(id);
		if (buildInfoManage == null) {
			return false;
		}
		if (buildInfoManage.process != null) {
			try {
				buildInfoManage.process.destroy();
			} catch (Exception ignored) {
			}
		}
		buildInfoManage.updateStatus(BuildStatus.Cancel);
		BUILD_MANAGE_MAP.remove(id);
		return true;
	}

	/**
	 * 创建构建
	 *
	 * @param buildInfoModel   构建项
	 * @param userModel        操作人
	 * @param repositoryModel  仓库信息
	 * @param delay            延迟执行的时间 单位秒
	 * @param triggerBuildType 触发构建类型
	 * @return this
	 */
	public static BuildInfoManage create(final BuildInfoModel buildInfoModel,
										 final RepositoryModel repositoryModel,
										 final UserModel userModel,
										 Integer delay,
										 int triggerBuildType) {
		if (BUILD_MANAGE_MAP.containsKey(buildInfoModel.getId())) {
			throw new JpomRuntimeException("当前构建还在进行中");
		}
		BuildInfoManage manage = new BuildInfoManage(buildInfoModel, repositoryModel, userModel, delay, triggerBuildType);
		BUILD_MANAGE_MAP.put(buildInfoModel.getId(), manage);
		//
		ThreadUtil.execute(manage);
		return manage;
	}

	@Override
	protected boolean updateStatus(BuildStatus status) {
		try {
			//super.updateStatus(status);
			BuildInfoService buildService = SpringUtil.getBean(BuildInfoService.class);
			BuildInfoModel item = buildService.getByKey(this.buildModelId);
			item.setStatus(status.getCode());
			buildService.update(item);
			//
			if (status == BuildStatus.Ing) {
				this.insertLog();
			} else {
				DbBuildHistoryLogService dbBuildHistoryLogService = SpringUtil.getBean(DbBuildHistoryLogService.class);
				dbBuildHistoryLogService.updateLog(this.logId, status);
			}
			return true;
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("构建状态变更失败", e);
			return false;
		}
	}


	/**
	 * 插入记录
	 */
	private void insertLog() {
		this.logId = IdUtil.fastSimpleUUID();
		BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
		// 更新其他配置字段
		buildExtraModule.fillLogValue(buildHistoryLog);
//		buildHistoryLog.setResultDirFile(buildExtraModule.getResultDirFile());
//		buildHistoryLog.setReleaseMethod(buildExtraModule.getReleaseMethod());
//		buildHistoryLog.setReleaseMethodDataId(buildExtraModule.getReleaseMethodDataId());
//		buildHistoryLog.setAfterOpt(buildExtraModule.getAfterOpt());
//		buildHistoryLog.setReleasePath(buildExtraModule.getReleasePath());
//		buildHistoryLog.setReleaseCommand(buildExtraModule.getReleaseCommand());
//		buildHistoryLog.setClearOld(buildExtraModule.isClearOld());

		buildHistoryLog.setTriggerBuildType(triggerBuildType);
		buildHistoryLog.setBuildNumberId(buildInfoModel.getBuildId());
		buildHistoryLog.setBuildName(buildInfoModel.getName());
		buildHistoryLog.setBuildDataId(buildInfoModel.getId());

		//
		buildHistoryLog.setWorkspaceId(this.buildInfoModel.getWorkspaceId());
		buildHistoryLog.setId(this.logId);
		buildHistoryLog.setStatus(BuildStatus.Ing.getCode());
		buildHistoryLog.setStartTime(SystemClock.now());
		DbBuildHistoryLogService dbBuildHistoryLogService = SpringUtil.getBean(DbBuildHistoryLogService.class);
		dbBuildHistoryLogService.insert(buildHistoryLog);
	}

	/**
	 * 打包构建产物
	 */
	private boolean packageFile() {
		ThreadUtil.sleep(2, TimeUnit.SECONDS);
		String resultDirFile = buildInfoModel.getResultDirFile();
		File rootFile = this.gitFile;
		boolean updateDirFile = false;
		if (ANT_PATH_MATCHER.isPattern(resultDirFile)) {
			// 通配模式
			String matchStr = FileUtil.normalize(StrUtil.SLASH + resultDirFile);
			List<String> paths = new ArrayList<>();
			//
			FileUtil.walkFiles(this.gitFile.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					return this.test(file);
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes exc) throws IOException {
					return this.test(dir);
				}

				private FileVisitResult test(Path path) {
					String subPath = FileUtil.subPath(FileUtil.getAbsolutePath(rootFile), path.toFile());
					subPath = FileUtil.normalize(StrUtil.SLASH + subPath);
					if (ANT_PATH_MATCHER.match(matchStr, subPath)) {
						paths.add(subPath);
						return FileVisitResult.TERMINATE;
					}
					return FileVisitResult.CONTINUE;
				}
			});
			String first = CollUtil.getFirst(paths);
			if (StrUtil.isEmpty(first)) {
				this.log(resultDirFile + " 没有匹配到任何文件");
				return false;
			}
			// 切换到匹配到到文件
			this.log(StrUtil.format("match {} {}", resultDirFile, first));
			resultDirFile = first;
			updateDirFile = true;
		}
		File file = FileUtil.file(this.gitFile, resultDirFile);
		if (!file.exists()) {
			this.log(resultDirFile + "不存在，处理构建产物失败");
			return false;
		}
		File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFile);
		FileCopier.create(file, toFile)
				.setCopyContentIfDir(true)
				.setOverride(true)
				.setCopyAttributes(true)
				.setCopyFilter(file1 -> !file1.isHidden())
				.copy();
		this.log(StrUtil.format("mv {} {}", resultDirFile, buildInfoModel.getBuildId()));
		// 修改构建产物目录
		if (updateDirFile) {
			DbBuildHistoryLogService dbBuildHistoryLogService = SpringUtil.getBean(DbBuildHistoryLogService.class);
			dbBuildHistoryLogService.updateResultDirFile(this.logId, resultDirFile);
			//
			this.buildInfoModel.setResultDirFile(resultDirFile);
			this.buildExtraModule.setResultDirFile(resultDirFile);
		}
		return true;
	}

	/**
	 * 准备构建
	 *
	 * @return false 执行异常需要结束
	 */
	private boolean startReady() {
		if (!updateStatus(BuildStatus.Ing)) {
			BuildInfoManage.this.log("初始化构建记录失败,异常结束");
			return false;
		}
		this.log("#" + this.buildInfoModel.getBuildId() + " start build in file : " + FileUtil.getAbsolutePath(this.gitFile));
		if (delay != null && delay > 0) {
			// 延迟执行
			this.log("Execution delayed by " + delay + " seconds");
			ThreadUtil.sleep(delay, TimeUnit.SECONDS);
		}
		return true;
	}

	/**
	 * 拉取代码
	 *
	 * @return false 执行异常需要结束
	 */
	private boolean pull() {
		try {
			String msg = "error";
			Integer repoTypeCode = repositoryModel.getRepoType();
			RepositoryModel.RepoType repoType = EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repoTypeCode);
			Integer protocolCode = repositoryModel.getProtocol();
			GitProtocolEnum protocol = EnumUtil.likeValueOf(GitProtocolEnum.class, protocolCode);
			if (repoType == RepositoryModel.RepoType.Git) {
				// git with password
				Tuple tuple = GitUtil.getBranchAndTagListChek(repositoryModel);
				String branchName = buildInfoModel.getBranchName();
				// 模糊匹配分支
				String newBranchName = GitUtil.fuzzyMatch(tuple.get(0), branchName);
				if (StrUtil.isEmpty(newBranchName)) {
					BuildInfoManage.this.log(branchName + " Did not match the corresponding branch");
					BuildInfoManage.this.updateStatus(BuildStatus.Error);
					return false;
				}
				// 模糊匹配 标签
				String branchTagName = buildInfoModel.getBranchTagName();
				if (StrUtil.isNotEmpty(branchTagName)) {
					String newBranchTagName = GitUtil.fuzzyMatch(tuple.get(1), branchTagName);
					if (StrUtil.isEmpty(newBranchTagName)) {
						BuildInfoManage.this.log(branchTagName + " Did not match the corresponding tag");
						BuildInfoManage.this.updateStatus(BuildStatus.Error);
						return false;
					}
					// 标签拉取模式
					BuildInfoManage.this.log("repository [" + branchName + "] [" + branchTagName + "] clone pull from " + newBranchName + "  " + newBranchTagName);
					msg = GitUtil.checkoutPullTag(repositoryModel, gitFile, newBranchName, newBranchTagName, BuildInfoManage.this.getPrintWriter());
				} else {
					// 分支模式
					BuildInfoManage.this.log("repository [" + branchName + "] clone pull from " + newBranchName);
					msg = GitUtil.checkoutPull(repositoryModel, gitFile, newBranchName, BuildInfoManage.this.getPrintWriter());
				}
			} else if (repoType == RepositoryModel.RepoType.Svn) {
				// svn
				Map<String, Object> map = new HashMap<>(10);
				map.put("url", repositoryModel.getGitUrl());
				map.put("userName", repositoryModel.getUserName());
				map.put("password", repositoryModel.getPassword());
				map.put("protocol", protocol.name());
				File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel);
				map.put("rsaFile", rsaFile);
				IPlugin plugin = PluginFactory.getPlugin(DefaultPlugin.SvnClone);
				Object result = plugin.execute(gitFile, map);
				//msg = SvnKitUtil.checkOut(repositoryModel, gitFile);
				msg = StrUtil.toString(result);
			}
			BuildInfoManage.this.log(msg);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return true;
	}

	/**
	 * 执行构建命令
	 *
	 * @return false 执行异常需要结束
	 */
	private boolean executeCommand() {
		String[] commands = CharSequenceUtil.splitToArray(buildInfoModel.getScript(), StrUtil.LF);
		if (commands == null || commands.length <= 0) {
			this.log("没有需要执行的命令");
			this.updateStatus(BuildStatus.Error);
			return false;
		}
		for (String item : commands) {
			try {
				boolean s = runCommand(item);
				if (!s) {
					this.log("命令执行存在error");
				}
			} catch (Exception e) {
				this.log(item + " 执行异常", e);
				return false;
			}
		}
		return true;
	}

	/**
	 * 打包发布
	 *
	 * @return false 执行需要结束
	 */
	private boolean packageRelease() {
		boolean status = packageFile();
		if (status && buildInfoModel.getReleaseMethod() != BuildReleaseMethod.No.getCode()) {
			// 发布文件
			new ReleaseManage(buildExtraModule, this.userModel, this, buildInfoModel.getBuildId()).start();
		} else {
			//
			updateStatus(BuildStatus.Success);
		}
		return true;
	}

	@Override
	public void run() {
		// 初始化构建流程 准备->拉取代码->执行构建命令->打包发布
		Map<String, Supplier<Boolean>> suppliers = new LinkedHashMap<>(10);
		suppliers.put("startReady", BuildInfoManage.this::startReady);
		suppliers.put("pull", BuildInfoManage.this::pull);
		suppliers.put("executeCommand", BuildInfoManage.this::executeCommand);
		suppliers.put("release", BuildInfoManage.this::packageRelease);
		// 依次执行流程，发生异常结束整个流程
		String processName = StrUtil.EMPTY;
		if (this.triggerBuildType == 2) {
			// 系统触发构建
			BaseServerController.resetInfo(UserModel.EMPTY);
		} else {
			BaseServerController.resetInfo(this.userModel);
		}
		try {
			for (Map.Entry<String, Supplier<Boolean>> stringSupplierEntry : suppliers.entrySet()) {
				processName = stringSupplierEntry.getKey();
				Supplier<Boolean> value = stringSupplierEntry.getValue();
				//
				this.asyncWebHooks(processName);
				Boolean aBoolean = value.get();
				if (!aBoolean) {
					// 有条件结束构建流程
					this.asyncWebHooks("stop", "process", processName);
					break;
				}
			}
			this.asyncWebHooks("success");
		} catch (RuntimeException runtimeException) {
			Throwable cause = runtimeException.getCause();
			this.log("构建失败:" + processName, cause == null ? runtimeException : cause);
			this.asyncWebHooks(processName, "error", runtimeException.getMessage());
		} catch (Exception e) {
			this.log("构建失败:" + processName, e);
			this.asyncWebHooks(processName, "error", e.getMessage());
		} finally {
			BUILD_MANAGE_MAP.remove(buildInfoModel.getId());
			BaseServerController.remove();
			this.asyncWebHooks("done");
		}
	}

	private void log(String title, Throwable throwable) {
		log(title, throwable, BuildStatus.Error);
	}

	/**
	 * 执行命令
	 *
	 * @param command 命令
	 * @return 是否存在错误
	 * @throws IOException IO
	 */
	private boolean runCommand(String command) throws IOException, InterruptedException {
		this.log(command);
		//
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(this.gitFile);
		List<String> commands = CommandUtil.getCommand();
		commands.add(command);
		processBuilder.command(commands);
		final boolean[] status = new boolean[1];
		processBuilder.redirectErrorStream(true);
		process = processBuilder.start();
		//
		InputStream inputStream = process.getInputStream();
		IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) line -> {
			log(line);
			status[0] = true;
		});
		int waitFor = process.waitFor();
		log("process result " + waitFor);
		return status[0];
	}

	/**
	 * 执行 webhooks 通知
	 *
	 * @param type  类型
	 * @param other 其他参数
	 */
	private void asyncWebHooks(String type, Object... other) {
		String webhook = this.buildInfoModel.getWebhook();
		IPlugin plugin = PluginFactory.getPlugin(DefaultPlugin.WebHook);
		Map<String, Object> map = new HashMap<>(10);
		long triggerTime = SystemClock.now();
		map.put("buildId", this.buildInfoModel.getId());
		map.put("buildName", this.buildInfoModel.getName());
		map.put("type", type);
		map.put("triggerBuildType", this.triggerBuildType);
		map.put("triggerTime", triggerTime);
		//
		for (int i = 0; i < other.length; i += 2) {
			map.put(other[i].toString(), other[i + 1]);
		}
		ThreadUtil.execute(() -> {
			try {
				plugin.execute(webhook, map);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("WebHooks 调用错误", e);
			}
		});

	}
}
