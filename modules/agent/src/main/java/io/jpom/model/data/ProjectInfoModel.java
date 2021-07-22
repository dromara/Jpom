package io.jpom.model.data;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.request.XssFilter;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.BaseJsonModel;
import io.jpom.model.BaseModel;
import io.jpom.model.RunMode;
import io.jpom.service.WhitelistDirectoryService;
import io.jpom.system.JpomRuntimeException;
import io.jpom.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 项目配置信息实体
 *
 * @author jiangzeyin
 */
public class ProjectInfoModel extends BaseModel {
	/**
	 * 分组
	 */
	private String group;
	private String mainClass;
	private String lib;
	/**
	 * 白名单目录
	 */
	private String whitelistDirectory;
	private String log;

	/**
	 * 日志目录
	 */
	private String logPath;
	/**
	 * jvm 参数
	 */
	private String jvm;
	/**
	 * java main 方法参数
	 */
	private String args;

	private List<JavaCopyItem> javaCopyItemList;
	/**
	 * WebHooks
	 */
	private String token;
	private boolean status;
	private String createTime;
	private String modifyTime;
	private String jdkId;
	/**
	 * lib 目录当前文件状态
	 */
	private String useLibDesc;
	/**
	 * 当前运行lib 状态
	 */
	private String runLibDesc;
	/**
	 * 最后修改人
	 */
	private String modifyUser;

	private RunMode runMode;
	/**
	 * 节点分发项目，不允许在项目管理中编辑
	 */
	private boolean outGivingProject;
	/**
	 * 实际运行的命令
	 */
	private String runCommand;

	/**
	 * -Djava.ext.dirs=lib -cp conf:run.jar
	 * 填写【lib:conf】
	 */
	private String javaExtDirsCp;

	public List<JavaCopyItem> getJavaCopyItemList() {
		return javaCopyItemList;
	}

	public void setJavaCopyItemList(List<JavaCopyItem> javaCopyItemList) {
		this.javaCopyItemList = javaCopyItemList;
	}

	public String getJavaExtDirsCp() {
		return StrUtil.emptyToDefault(javaExtDirsCp, StrUtil.EMPTY);
	}

	public void setJavaExtDirsCp(String javaExtDirsCp) {
		this.javaExtDirsCp = javaExtDirsCp;
	}

	public String getRunCommand() {
		return runCommand;
	}

	public void setRunCommand(String runCommand) {
		this.runCommand = runCommand;
	}

	public boolean isOutGivingProject() {
		return outGivingProject;
	}

	public void setOutGivingProject(boolean outGivingProject) {
		this.outGivingProject = outGivingProject;
	}

	public RunMode getRunMode() {
		if (runMode == null) {
			return RunMode.ClassPath;
		}
		return runMode;
	}

	public void setRunMode(RunMode runMode) {
		this.runMode = runMode;
	}

	public String getModifyUser() {
		if (StrUtil.isEmpty(modifyUser)) {
			return StrUtil.DASHED;
		}
		return modifyUser;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}

	/**
	 * 项目是否正在运行
	 *
	 * @return true 正在运行
	 */
	public boolean tryGetStatus() {
		try {
			status = AbstractProjectCommander.getInstance().isRun(getId());
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("检查项目状态错误", e);
			status = false;
		}
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getRunLibDesc() {
		return runLibDesc;
	}

	public void setRunLibDesc(String runLibDesc) {
		this.runLibDesc = runLibDesc;
	}

	public String getUseLibDesc() {
		return useLibDesc;
	}

	public void setUseLibDesc(String useLibDesc) {
		this.useLibDesc = useLibDesc;
	}

	public String getModifyTime() {
		return modifyTime;
	}

	/**
	 * 修改时间
	 *
	 * @param modifyTime time
	 */
	public void setModifyTime(String modifyTime) {
		this.modifyTime = modifyTime;
	}

	public String getJvm() {
		String s = StrUtil.emptyToDefault(jvm, StrUtil.EMPTY);
		if (XssFilter.isXSS()) {
			s = HtmlUtil.unescape(s);
		}
		return s;
	}

	public void setJvm(String jvm) {
		if (XssFilter.isXSS()) {
			this.jvm = HtmlUtil.unescape(jvm);
		} else {
			this.jvm = jvm;
		}
	}

	public String getGroup() {
		if (StrUtil.isEmpty(group)) {
			return "默认";
		}
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getMainClass() {
		return StrUtil.emptyToDefault(mainClass, StrUtil.EMPTY);
	}

	private void repairWhitelist() {
		if (StrUtil.isEmpty(whitelistDirectory) && StrUtil.isEmpty(lib)) {
			throw new JpomRuntimeException("当前项目lib数据异常");
		}
		if (StrUtil.isNotEmpty(whitelistDirectory)) {
			return;
		}
		WhitelistDirectoryService whitelistDirectoryService = SpringUtil.getBean(WhitelistDirectoryService.class);
		List<String> project = whitelistDirectoryService.getWhitelist().getProject();
		for (String path : project) {
			if (lib.startsWith(path)) {
				String itemWhitelistDirectory = lib.substring(0, path.length());
				lib = lib.substring(path.length());
				setWhitelistDirectory(itemWhitelistDirectory);
				setLib(lib);
			}
		}
	}

	public String getWhitelistDirectory() {
		this.repairWhitelist();
		if (StrUtil.isEmpty(whitelistDirectory)) {
			throw new JpomRuntimeException("修护白名单数据异常");
		}
		return whitelistDirectory;
	}

	public void setWhitelistDirectory(String whitelistDirectory) {
		this.whitelistDirectory = whitelistDirectory;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getLib() {
		this.repairWhitelist();
		return lib;
	}

	public String allLib() {
		return FileUtil.file(getWhitelistDirectory(), getLib()).getAbsolutePath();
	}

	/**
	 * 获取项目文件中的所有jar 文件
	 *
	 * @param projectInfoModel 项目
	 * @return list
	 */
	public static List<File> listJars(ProjectInfoModel projectInfoModel) {
		File fileLib = new File(projectInfoModel.allLib());
		File[] files = fileLib.listFiles();
		List<File> files1 = new ArrayList<>();
		if (files != null) {
			for (File file : files) {
				if (!file.isFile()) {
					continue;
				}
				if (projectInfoModel.getRunMode() == RunMode.ClassPath || projectInfoModel.getRunMode() == RunMode.Jar) {
					if (!StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true)) {
						continue;
					}
				} else if (projectInfoModel.getRunMode() == RunMode.JarWar) {
					if (!StrUtil.endWith(file.getName(), "war", true)) {
						continue;
					}
				}
				files1.add(file);
			}
		}
		return files1;
	}

	/**
	 * 拼接java 执行的jar路径
	 *
	 * @param projectInfoModel 项目
	 * @return classpath 或者 jar
	 */
	public static String getClassPathLib(ProjectInfoModel projectInfoModel) {
		List<File> files = listJars(projectInfoModel);
		if (files.size() <= 0) {
			return "";
		}
		// 获取lib下面的所有jar包
		StringBuilder classPath = new StringBuilder();
		RunMode runMode = projectInfoModel.getRunMode();
		int len = files.size();
		if (runMode == RunMode.ClassPath) {
			classPath.append("-classpath ");
		} else if (runMode == RunMode.Jar || runMode == RunMode.JarWar) {
			classPath.append("-jar ");
			// 只取一个jar文件
			len = 1;
		} else if (runMode == RunMode.JavaExtDirsCp) {
			classPath.append("-Djava.ext.dirs=");
			String javaExtDirsCp = projectInfoModel.getJavaExtDirsCp();
			String[] split = StrUtil.splitToArray(javaExtDirsCp, StrUtil.COLON);
			if (ArrayUtil.isEmpty(split)) {
				classPath.append(". -cp ");
			} else {
				classPath.append(split[0]).append(" -cp ");
				if (split.length > 1) {
					classPath.append(split[1]).append(FileUtils.getJarSeparator());
				}
			}
		} else {
			return StrUtil.EMPTY;
		}
		for (int i = 0; i < len; i++) {
			File file = files.get(i);
			classPath.append(file.getAbsolutePath());
			if (i != len - 1) {
				classPath.append(FileUtils.getJarSeparator());
			}
		}
		return classPath.toString();
	}

	public void setLib(String lib) {
		this.lib = lib;
	}


	public String getLogPath() {
		return StrUtil.emptyToDefault(this.logPath, StrUtil.EMPTY);
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public String getLog() {
		if (StrUtil.isEmpty(this.getId())) {
			return StrUtil.EMPTY;
		}
		if (StrUtil.isNotEmpty(this.getLogPath())) {
			return FileUtil.normalize(String.format("%s/%s/%s.log", this.getLogPath(), this.getId(), this.getId()));
		}
		if (StrUtil.isEmpty(this.log)) {
			String log = new File(this.allLib()).getParent();
			this.log = FileUtil.normalize(String.format("%s/%s.log", log, this.getId()));
		}
		return StrUtil.emptyToDefault(this.log, StrUtil.EMPTY);
	}

	/**
	 * 副本的控制台日志文件
	 *
	 * @param javaCopyItem 副本信息
	 * @return file
	 */
	public File getLog(JavaCopyItem javaCopyItem) {
		File file = FileUtil.file(getLog());
		return FileUtil.file(file.getParentFile(), getId() + "_" + javaCopyItem.getId() + ".log");
	}

	public String getAbsoluteLog(JavaCopyItem javaCopyItem) {
		File file = javaCopyItem == null ? new File(getLog()) : getLog(javaCopyItem);
		return file.getAbsolutePath();
	}

	public File getLogBack() {
		return new File(getLog() + "_back");
	}

	public File getLogBack(JavaCopyItem javaCopyItem) {
		return new File(getLog(javaCopyItem) + "_back");
	}

	public void setLog(String log) {
		this.log = log;
	}

	/**
	 * 默认
	 *
	 * @return url token
	 */
	public String getToken() {
		// 兼容旧数据
		if ("no".equalsIgnoreCase(this.token)) {
			return "";
		}
		return StrUtil.emptyToDefault(token, StrUtil.EMPTY);
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public String getArgs() {
		String s = StrUtil.emptyToDefault(args, StrUtil.EMPTY);
		if (XssFilter.isXSS()) {
			s = HtmlUtil.unescape(s);
		}
		return s;
	}

	public void setArgs(String args) {
		if (XssFilter.isXSS()) {
			this.args = HtmlUtil.unescape(args);
		} else {
			this.args = args;
		}
	}

	public String getJdkId() {
		return jdkId;
	}

	public void setJdkId(String jdkId) {
		this.jdkId = jdkId;
	}


	public JavaCopyItem findCopyItem(String copyId) {
		if (StrUtil.isEmpty(copyId)) {
			return null;
		}
		List<JavaCopyItem> javaCopyItemList = getJavaCopyItemList();
		if (CollUtil.isEmpty(javaCopyItemList)) {
			return null;
		}
		Optional<JavaCopyItem> first = javaCopyItemList.stream().filter(javaCopyItem -> StrUtil.equals(javaCopyItem.getId(), copyId)).findFirst();
		return first.orElse(null);
	}

	public boolean removeCopyItem(String copyId) {
		if (StrUtil.isEmpty(copyId)) {
			return true;
		}
		if (CollUtil.isEmpty(javaCopyItemList)) {
			return true;
		}
		int size = javaCopyItemList.size();
		List<JavaCopyItem> collect = javaCopyItemList.stream().filter(javaCopyItem -> !StrUtil.equals(javaCopyItem.getId(), copyId)).collect(Collectors.toList());
		if (size - 1 == collect.size()) {
			this.javaCopyItemList = collect;
			return true;
		} else {
			return false;
		}
	}

	public static class JavaCopyItem extends BaseJsonModel {
		/**
		 * 父级项目id
		 */
		private String parendId;
		/**
		 * id
		 */
		private String id;

		/**
		 * jvm 参数
		 */
		private String jvm;
		/**
		 * java main 方法参数
		 */
		private String args;

		private String modifyTime;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTagId() {
			return getTagId(parendId, id);
		}

		/**
		 * 创建进程标记
		 *
		 * @param id
		 * @param copyId
		 * @return
		 */
		public static String getTagId(String id, String copyId) {
			if (StrUtil.isEmpty(copyId)) {
				return id;
			}
			return StrUtil.format("{}:{}", id, copyId);
		}

		public String getModifyTime() {
			return modifyTime;
		}

		public void setModifyTime(String modifyTime) {
			this.modifyTime = modifyTime;
		}

		/**
		 * 项目是否正在运行
		 *
		 * @return true 正在运行
		 */
		public boolean tryGetStatus() {
			try {
				return AbstractProjectCommander.getInstance().isRun(getTagId());
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("检查项目状态错误", e);
				return false;
			}
		}

		public String getParendId() {
			return parendId;
		}

		public void setParendId(String parendId) {
			this.parendId = parendId;
		}

		public String getJvm() {
			if (XssFilter.isXSS()) {
				return HtmlUtil.unescape(jvm);
			}
			return jvm;
		}

		public void setJvm(String jvm) {
			if (XssFilter.isXSS()) {
				this.jvm = HtmlUtil.unescape(jvm);
			} else {
				this.jvm = jvm;
			}
		}

		public String getArgs() {
			if (XssFilter.isXSS()) {
				return HtmlUtil.unescape(args);
			}
			return args;
		}

		public void setArgs(String args) {
			if (XssFilter.isXSS()) {
				this.args = HtmlUtil.unescape(args);
			} else {
				this.args = args;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			JavaCopyItem that = (JavaCopyItem) o;
			return Objects.equals(parendId, that.parendId) &&
					Objects.equals(id, that.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(parendId, id, jvm, args, modifyTime);
		}
	}
}
