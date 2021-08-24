package io.jpom.build;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.LineHandler;
import cn.hutool.core.io.file.FileCopier;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.JpomApplication;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.BuildModel;
import io.jpom.model.data.RepositoryModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.BuildHistoryLog;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import io.jpom.util.StringUtil;
import io.jpom.util.SvnKitUtil;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.util.AntPathMatcher;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private final String optUserName;
	private final UserModel userModel;
	private final BaseBuildModule baseBuildModule;

	private BuildInfoManage(final BuildInfoModel buildInfoModel, final RepositoryModel repositoryModel, final UserModel userModel) {
		super(BuildUtil.getLogFile(buildInfoModel.getId(), buildInfoModel.getBuildId()),
				buildInfoModel.getId());
		this.buildInfoModel = buildInfoModel;
		this.repositoryModel = repositoryModel;
		this.gitFile = BuildUtil.getSourceById(buildInfoModel.getId());
		this.optUserName = UserModel.getOptUserName(userModel);
		this.userModel = userModel;
		// 解析 其他配置信息
		BaseBuildModule baseBuildModule = StringUtil.jsonConvert(this.buildInfoModel.getExtraData(), BaseBuildModule.class);
		Assert.notNull(baseBuildModule);
		// update value
		baseBuildModule.updateValue(this.buildInfoModel);
		this.baseBuildModule = baseBuildModule;
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
		buildInfoManage.updateStatus(BuildModel.Status.Cancel);
		BUILD_MANAGE_MAP.remove(id);
		return true;
	}

	/**
	 * 创建构建
	 *
	 * @param buildInfoModel 构建项
	 * @param userModel      操作人
	 * @return this
	 */
	public static BuildInfoManage create(BuildInfoModel buildInfoModel, RepositoryModel repositoryModel, UserModel userModel) {
		if (BUILD_MANAGE_MAP.containsKey(buildInfoModel.getId())) {
			throw new JpomRuntimeException("当前构建还在进行中");
		}
		BuildInfoManage manage = new BuildInfoManage(buildInfoModel, repositoryModel, userModel);
		BUILD_MANAGE_MAP.put(buildInfoModel.getId(), manage);
		//
		ThreadUtil.execute(manage);
		return manage;
	}

	@Override
	protected boolean updateStatus(BuildModel.Status status) {
		try {
			//super.updateStatus(status);
			BuildInfoService buildService = SpringUtil.getBean(BuildInfoService.class);
			BuildInfoModel item = buildService.getByKey(this.buildModelId);
			item.setStatus(status.getCode());
			buildService.update(item);
			//
			if (status == BuildModel.Status.Ing) {
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
		buildHistoryLog.setResultDirFile(baseBuildModule.getResultDirFile());
		buildHistoryLog.setReleaseMethod(baseBuildModule.getReleaseMethod());
		buildHistoryLog.setReleaseMethodDataId(baseBuildModule.getReleaseMethodDataId());
		buildHistoryLog.setAfterOpt(baseBuildModule.getAfterOpt());
		buildHistoryLog.setReleaseCommand(baseBuildModule.getReleaseCommand());
		BeanUtil.copyProperties(this.buildInfoModel, buildHistoryLog);

		buildHistoryLog.setId(this.logId);
		buildHistoryLog.setBuildDataId(buildInfoModel.getId());
		buildHistoryLog.setStatus(BuildModel.Status.Ing.getCode());
		buildHistoryLog.setStartTime(System.currentTimeMillis());
		buildHistoryLog.setBuildNumberId(buildInfoModel.getBuildId());
		buildHistoryLog.setBuildUser(optUserName);

		DbBuildHistoryLogService dbBuildHistoryLogService = SpringUtil.getBean(DbBuildHistoryLogService.class);
		dbBuildHistoryLogService.insert(buildHistoryLog);
	}

	/**
	 * 打包构建产物
	 */
	private boolean packageFile() throws InterruptedException {
		Thread.sleep(2000);
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
		}
		return true;
	}

	@Override
	public void run() {
		try {
			if (!updateStatus(BuildModel.Status.Ing)) {
				this.log("初始化构建记录失败,异常结束");
				return;
			}
			try {
				this.log("start build in file : " + FileUtil.getAbsolutePath(this.gitFile));
				//
				String branchName = buildInfoModel.getBranchName();
				this.log("repository clone pull from " + branchName);
				String msg = "error";
				if (repositoryModel.getRepoType() == BuildModel.RepoType.Git.getCode()) {
					// git
					UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(repositoryModel.getUserName(), repositoryModel.getPassword());
					msg = GitUtil.checkoutPull(repositoryModel.getGitUrl(), gitFile, branchName, credentialsProvider, this.getPrintWriter());
				} else if (repositoryModel.getRepoType() == BuildModel.RepoType.Svn.getCode()) {
					// svn
					msg = SvnKitUtil.checkOut(repositoryModel.getGitUrl(), repositoryModel.getUserName(), repositoryModel.getPassword(), gitFile);
				}
				this.log(msg);
			} catch (Exception e) {
				this.log("拉取代码失败", e);
				return;
			}
			String[] commands = CharSequenceUtil.splitToArray(buildInfoModel.getScript(), StrUtil.LF);
			if (commands == null || commands.length <= 0) {
				this.log("没有需要执行的命令");
				this.updateStatus(BuildModel.Status.Error);
				return;
			}
			for (String item : commands) {
				try {
					boolean s = runCommand(item);
					if (!s) {
						this.log("命令执行存在error");
					}
				} catch (IOException e) {
					this.log(item + " 执行异常", e);
					return;
				}
			}
			boolean status = packageFile();
			if (status && buildInfoModel.getReleaseMethod() != BuildModel.ReleaseMethod.No.getCode()) {
				// 发布文件
				new ReleaseManage(baseBuildModule, this.userModel, this, buildInfoModel.getBuildId()).start();
			} else {
				//
				updateStatus(BuildModel.Status.Success);
			}
		} catch (Exception e) {
			this.log("构建失败", e);
		} finally {
			BUILD_MANAGE_MAP.remove(buildInfoModel.getId());
		}
	}

	private void log(String title, Throwable throwable) {
		log(title, throwable, BuildModel.Status.Error);
	}

	/**
	 * 执行命令
	 *
	 * @param command 命令
	 * @return 是否存在错误
	 * @throws IOException IO
	 */
	private boolean runCommand(String command) throws IOException {
		this.log(command);
		//
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.directory(this.gitFile);
		List<String> commands = CommandUtil.getCommand();
		commands.add(command);
		processBuilder.command(commands);
		final boolean[] status = new boolean[1];
		process = processBuilder.start();
		//
		InputStream inputStream = process.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, JpomApplication.getCharset());
		BufferedReader results = new BufferedReader(inputStreamReader);
		IoUtil.readLines(results, (LineHandler) line -> {
			log(line);
			status[0] = true;
		});
		//
		InputStream errorInputStream = process.getErrorStream();
		InputStreamReader errorInputStreamReader = new InputStreamReader(errorInputStream, JpomApplication.getCharset());
		BufferedReader errorResults = new BufferedReader(errorInputStreamReader);
		IoUtil.readLines(errorResults, (LineHandler) line -> {
			log(line);
			status[0] = false;
		});
		return status[0];
	}
}
