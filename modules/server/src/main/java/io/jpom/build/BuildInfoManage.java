///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 Code Technology Studio
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package io.jpom.build;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.date.SystemClock;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.io.IoUtil;
//import cn.hutool.core.io.LineHandler;
//import cn.hutool.core.io.file.FileCopier;
//import cn.hutool.core.lang.Tuple;
//import cn.hutool.core.text.CharSequenceUtil;
//import cn.hutool.core.thread.ThreadUtil;
//import cn.hutool.core.util.EnumUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.jiangzeyin.common.DefaultSystemLog;
//import io.jpom.common.BaseServerController;
//import io.jpom.model.data.BuildInfoModel;
//import io.jpom.model.data.RepositoryModel;
//import io.jpom.model.data.UserModel;
//import io.jpom.model.enums.BuildReleaseMethod;
//import io.jpom.model.enums.BuildStatus;
//import io.jpom.model.enums.GitProtocolEnum;
//import io.jpom.plugin.DefaultPlugin;
//import io.jpom.plugin.IPlugin;
//import io.jpom.plugin.PluginFactory;
//import io.jpom.service.dblog.DbBuildHistoryLogService;
//import io.jpom.system.ExtConfigBean;
//import io.jpom.util.CommandUtil;
//import io.jpom.util.GitUtil;
//import lombok.Builder;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.FileVisitResult;
//import java.nio.file.Path;
//import java.nio.file.SimpleFileVisitor;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Supplier;
//
///**
// * new build info manage runnable
// *
// * @author Hotstrip
// * @since 20210-08-23
// */
//@Builder
//public class BuildInfoManage implements Runnable {
//
//	private final TaskData taskData;
//	private final BuildExtraModule buildExtraModule;
//	private final String logId;
//	private final BuildExecuteService buildExecuteService;
//	private final DbBuildHistoryLogService dbBuildHistoryLogService;
//	//
//	private Process process;
//	private LogRecorder logRecorder;
//	private File gitFile;
//
//		private BuildInfoManage(final TaskData taskData) {
////			this.taskData = taskData;
////			BuildInfoModel buildInfoModel = taskData.buildInfoModel;
////			File logFile = BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
////			this.logRecorder = LogRecorder.builder().file(logFile).build();
////			this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());
////
////			// 解析 其他配置信息
////			BuildExtraModule buildExtraModule = StringUtil.jsonConvert(buildInfoModel.getExtraData(), BuildExtraModule.class);
////			Assert.notNull(buildExtraModule, "构建信息缺失");
////			// update value
//
////			this.buildExtraModule = buildExtraModule;
////		}
//
//
//	//		@Override
////		private void updateStatus(BuildStatus status) {
////
////		}
//
//	/**
//	 * 打包构建产物
//	 */
//	private boolean packageFile() {
//		ThreadUtil.sleep(2, TimeUnit.SECONDS);
//		BuildInfoModel buildInfoModel = taskData.buildInfoModel;
//		String resultDirFile = buildInfoModel.getResultDirFile();
//		File rootFile = this.gitFile;
//		boolean updateDirFile = false;
//		if (ANT_PATH_MATCHER.isPattern(resultDirFile)) {
//			// 通配模式
//			String matchStr = FileUtil.normalize(StrUtil.SLASH + resultDirFile);
//			List<String> paths = new ArrayList<>();
//			//
//			FileUtil.walkFiles(this.gitFile.toPath(), new SimpleFileVisitor<Path>() {
//				@Override
//				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//					return this.test(file);
//				}
//
//				@Override
//				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes exc) throws IOException {
//					return this.test(dir);
//				}
//
//				private FileVisitResult test(Path path) {
//					String subPath = FileUtil.subPath(FileUtil.getAbsolutePath(rootFile), path.toFile());
//					subPath = FileUtil.normalize(StrUtil.SLASH + subPath);
//					if (ANT_PATH_MATCHER.match(matchStr, subPath)) {
//						paths.add(subPath);
//						return FileVisitResult.TERMINATE;
//					}
//					return FileVisitResult.CONTINUE;
//				}
//			});
//			String first = CollUtil.getFirst(paths);
//			if (StrUtil.isEmpty(first)) {
//				logRecorder.log(resultDirFile + " 没有匹配到任何文件");
//				return false;
//			}
//			// 切换到匹配到到文件
//			logRecorder.log(StrUtil.format("match {} {}", resultDirFile, first));
//			resultDirFile = first;
//			updateDirFile = true;
//		}
//		File file = FileUtil.file(this.gitFile, resultDirFile);
//		if (!file.exists()) {
//			logRecorder.log(resultDirFile + "不存在，处理构建产物失败");
//			return false;
//		}
//		File toFile = BuildUtil.getHistoryPackageFile(buildInfoModel.getId(), buildInfoModel.getBuildId(), resultDirFile);
//		FileCopier.create(file, toFile)
//				.setCopyContentIfDir(true)
//				.setOverride(true)
//				.setCopyAttributes(true)
//				.setCopyFilter(file1 -> !file1.isHidden())
//				.copy();
//		logRecorder.log(StrUtil.format("mv {} {}", resultDirFile, buildInfoModel.getBuildId()));
//		// 修改构建产物目录
//		if (updateDirFile) {
//			dbBuildHistoryLogService.updateResultDirFile(this.logId, resultDirFile);
//			//
//			buildInfoModel.setResultDirFile(resultDirFile);
//			this.buildExtraModule.setResultDirFile(resultDirFile);
//		}
//		return true;
//	}
//
//	/**
//	 * 准备构建
//	 *
//	 * @return false 执行异常需要结束
//	 */
//	private boolean startReady() {
//		BuildInfoModel buildInfoModel = taskData.buildInfoModel;
//		File logFile = BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId());
//		this.logRecorder = LogRecorder.builder().file(logFile).build();
//		this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());
//
//		Integer delay = taskData.delay;
//		logRecorder.log("#" + buildInfoModel.getBuildId() + " start build in file : " + FileUtil.getAbsolutePath(this.gitFile));
//		if (delay != null && delay > 0) {
//			// 延迟执行
//			logRecorder.log("Execution delayed by " + delay + " seconds");
//			ThreadUtil.sleep(delay, TimeUnit.SECONDS);
//		}
//		return true;
//	}
//
//	/**
//	 * 拉取代码
//	 *
//	 * @return false 执行异常需要结束
//	 */
//	private boolean pull() {
//		RepositoryModel repositoryModel = taskData.repositoryModel;
//		BuildInfoModel buildInfoModel = taskData.buildInfoModel;
//		try {
//			String msg = "error";
//			Integer repoTypeCode = repositoryModel.getRepoType();
//			RepositoryModel.RepoType repoType = EnumUtil.likeValueOf(RepositoryModel.RepoType.class, repoTypeCode);
//			Integer protocolCode = repositoryModel.getProtocol();
//			GitProtocolEnum protocol = EnumUtil.likeValueOf(GitProtocolEnum.class, protocolCode);
//			if (repoType == RepositoryModel.RepoType.Git) {
//				// git with password
//				Tuple tuple = GitUtil.getBranchAndTagListChek(repositoryModel);
//				String branchName = buildInfoModel.getBranchName();
//				// 模糊匹配分支
//				String newBranchName = GitUtil.fuzzyMatch(tuple.get(0), branchName);
//				if (StrUtil.isEmpty(newBranchName)) {
//					logRecorder.log(branchName + " Did not match the corresponding branch");
//					buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, BuildStatus.Error);
//					return false;
//				}
//				// 模糊匹配 标签
//				String branchTagName = buildInfoModel.getBranchTagName();
//				if (StrUtil.isNotEmpty(branchTagName)) {
//					String newBranchTagName = GitUtil.fuzzyMatch(tuple.get(1), branchTagName);
//					if (StrUtil.isEmpty(newBranchTagName)) {
//						logRecorder.log(branchTagName + " Did not match the corresponding tag");
//						buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, BuildStatus.Error);
//						return false;
//					}
//					// 标签拉取模式
//					logRecorder.log("repository [" + branchName + "] [" + branchTagName + "] clone pull from " + newBranchName + "  " + newBranchTagName);
//					msg = GitUtil.checkoutPullTag(repositoryModel, gitFile, newBranchName, newBranchTagName, logRecorder.getPrintWriter());
//				} else {
//					// 分支模式
//					logRecorder.log("repository [" + branchName + "] clone pull from " + newBranchName);
//					msg = GitUtil.checkoutPull(repositoryModel, gitFile, newBranchName, logRecorder.getPrintWriter());
//				}
//			} else if (repoType == RepositoryModel.RepoType.Svn) {
//				// svn
//				Map<String, Object> map = new HashMap<>(10);
//				map.put("url", repositoryModel.getGitUrl());
//				map.put("userName", repositoryModel.getUserName());
//				map.put("password", repositoryModel.getPassword());
//				map.put("protocol", protocol.name());
//				File rsaFile = BuildUtil.getRepositoryRsaFile(repositoryModel);
//				map.put("rsaFile", rsaFile);
//				IPlugin plugin = PluginFactory.getPlugin(DefaultPlugin.SvnClone);
//				Object result = plugin.execute(gitFile, map);
//				//msg = SvnKitUtil.checkOut(repositoryModel, gitFile);
//				msg = StrUtil.toString(result);
//			}
//			logRecorder.log(msg);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		return true;
//	}
//
//	/**
//	 * 执行构建命令
//	 *
//	 * @return false 执行异常需要结束
//	 */
//	private boolean executeCommand() {
//		BuildInfoModel buildInfoModel = taskData.buildInfoModel;
//		Integer buildMode = buildInfoModel.getBuildMode();
//		if (buildMode != null && buildMode == 1) {
//			// 容器构建
//
//		}
//		String[] commands = CharSequenceUtil.splitToArray(buildInfoModel.getScript(), StrUtil.LF);
//		if (commands == null || commands.length <= 0) {
//			logRecorder.log("没有需要执行的命令");
//			this.buildExecuteService.updateStatus(buildInfoModel.getId(), this.logId, BuildStatus.Error);
//			return false;
//		}
//		for (String item : commands) {
//			try {
//				boolean s = runCommand(item);
//				if (!s) {
//					logRecorder.log("命令执行存在error");
//				}
//			} catch (Exception e) {
//				logRecorder.log(item + " 执行异常", e);
//				return false;
//			}
//		}
//		return true;
//	}
//
//	/**
//	 * 打包发布
//	 *
//	 * @return false 执行需要结束
//	 */
//	private boolean packageRelease() {
//		BuildInfoModel buildInfoModel = taskData.buildInfoModel;
//		UserModel userModel = taskData.userModel;
//		boolean status = packageFile();
//		if (status && buildInfoModel.getReleaseMethod() != BuildReleaseMethod.No.getCode()) {
//			// 发布文件
//			ReleaseManage releaseManage = ReleaseManage.builder()
//					.buildId(buildInfoModel.getBuildId())
//					.buildExtraModule(buildExtraModule)
//					.userModel(userModel)
//					.logId(logId)
//					.buildExecuteService(buildExecuteService)
//					.logRecorder(logRecorder).build();
//			releaseManage.start();
//		} else {
//			//
//			buildExecuteService.updateStatus(buildInfoModel.getId(), logId, BuildStatus.Success);
//		}
//		return true;
//	}
//
//	@Override
//	public void run() {
//		// 初始化构建流程 准备->拉取代码->执行构建命令->打包发布
//		Map<String, Supplier<Boolean>> suppliers = new LinkedHashMap<>(10);
//		suppliers.put("startReady", BuildInfoManage.this::startReady);
//		suppliers.put("pull", BuildInfoManage.this::pull);
//		suppliers.put("executeCommand", BuildInfoManage.this::executeCommand);
//		suppliers.put("release", BuildInfoManage.this::packageRelease);
//		// 依次执行流程，发生异常结束整个流程
//		String processName = StrUtil.EMPTY;
//		if (taskData.triggerBuildType == 2) {
//			// 系统触发构建
//			BaseServerController.resetInfo(UserModel.EMPTY);
//		} else {
//			BaseServerController.resetInfo(taskData.userModel);
//		}
//		try {
//			for (Map.Entry<String, Supplier<Boolean>> stringSupplierEntry : suppliers.entrySet()) {
//				processName = stringSupplierEntry.getKey();
//				Supplier<Boolean> value = stringSupplierEntry.getValue();
//				//
//				this.asyncWebHooks(processName);
//				Boolean aBoolean = value.get();
//				if (!aBoolean) {
//					// 有条件结束构建流程
//					this.asyncWebHooks("stop", "process", processName);
//					break;
//				}
//			}
//			this.asyncWebHooks("success");
//		} catch (RuntimeException runtimeException) {
//			Throwable cause = runtimeException.getCause();
//			logRecorder.log("构建失败:" + processName, cause == null ? runtimeException : cause);
//			this.asyncWebHooks(processName, "error", runtimeException.getMessage());
//		} catch (Exception e) {
//			logRecorder.log("构建失败:" + processName, e);
//			this.asyncWebHooks(processName, "error", e.getMessage());
//		} finally {
//			BUILD_MANAGE_MAP.remove(taskData.buildInfoModel.getId());
//			BaseServerController.removeAll();
//			this.asyncWebHooks("done");
//		}
//	}
//
////		private void log(String title, Throwable throwable) {
////			log(title, throwable, BuildStatus.Error);
////		}
//
//	/**
//	 * 执行命令
//	 *
//	 * @param command 命令
//	 * @return 是否存在错误
//	 * @throws IOException IO
//	 */
//	private boolean runCommand(String command) throws IOException, InterruptedException {
//		logRecorder.log(command);
//		//
//		ProcessBuilder processBuilder = new ProcessBuilder();
//		processBuilder.directory(this.gitFile);
//		List<String> commands = CommandUtil.getCommand();
//		commands.add(command);
//		processBuilder.command(commands);
//		final boolean[] status = new boolean[1];
//		processBuilder.redirectErrorStream(true);
//		process = processBuilder.start();
//		//
//		InputStream inputStream = process.getInputStream();
//		IoUtil.readLines(inputStream, ExtConfigBean.getInstance().getConsoleLogCharset(), (LineHandler) line -> {
//			logRecorder.log(line);
//			status[0] = true;
//		});
//		int waitFor = process.waitFor();
//		logRecorder.log("process result " + waitFor);
//		return status[0];
//	}
//
//	/**
//	 * 执行 webhooks 通知
//	 *
//	 * @param type  类型
//	 * @param other 其他参数
//	 */
//	private void asyncWebHooks(String type, Object... other) {
//		BuildInfoModel buildInfoModel = taskData.buildInfoModel;
//		String webhook = buildInfoModel.getWebhook();
//		IPlugin plugin = PluginFactory.getPlugin(DefaultPlugin.WebHook);
//		Map<String, Object> map = new HashMap<>(10);
//		long triggerTime = SystemClock.now();
//		map.put("buildId", buildInfoModel.getId());
//		map.put("buildName", buildInfoModel.getName());
//		map.put("type", type);
//		map.put("triggerBuildType", taskData.triggerBuildType);
//		map.put("triggerTime", triggerTime);
//		//
//		for (int i = 0; i < other.length; i += 2) {
//			map.put(other[i].toString(), other[i + 1]);
//		}
//		ThreadUtil.execute(() -> {
//			try {
//				plugin.execute(webhook, map);
//			} catch (Exception e) {
//				DefaultSystemLog.getLog().error("WebHooks 调用错误", e);
//			}
//		});
//
//	}
//}
