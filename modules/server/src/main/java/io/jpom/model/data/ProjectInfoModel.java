package io.jpom.model.data;

import cn.hutool.crypto.SecureUtil;
import io.jpom.model.BaseNodeModel;
import io.jpom.service.h2db.TableName;
import org.springframework.util.Assert;

/**
 * @author bwcx_jzy
 * @since 2021/12/5
 */
@TableName("PROJECT_INFO")
public class ProjectInfoModel extends BaseNodeModel {

	private String projectId;


	private String name;

	private String mainClass;
	private String lib;
	/**
	 * 白名单目录
	 */
	private String whitelistDirectory;
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

	private String javaCopyItemList;
	/**
	 * WebHooks
	 */
	private String token;

	private String jdkId;

	private String runMode;
	/**
	 * 节点分发项目，不允许在项目管理中编辑
	 */
	private Integer outGivingProject;
	/**
	 * -Djava.ext.dirs=lib -cp conf:run.jar
	 * 填写【lib:conf】
	 */
	private String javaExtDirsCp;

	public String fullId() {
		String workspaceId = this.getWorkspaceId();

		String nodeId = this.getNodeId();

		String projectId = this.getProjectId();

		return ProjectInfoModel.fullId(workspaceId, nodeId, projectId);
	}

	public static String fullId(String workspaceId, String nodeId, String projectId) {

		Assert.hasText(workspaceId, "workspaceId");

		Assert.hasText(workspaceId, "nodeId");

		Assert.hasText(workspaceId, "projectId");
		return SecureUtil.sha1(workspaceId + nodeId + projectId);
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMainClass() {
		return mainClass;
	}

	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	public String getLib() {
		return lib;
	}

	public void setLib(String lib) {
		this.lib = lib;
	}

	public String getWhitelistDirectory() {
		return whitelistDirectory;
	}

	public void setWhitelistDirectory(String whitelistDirectory) {
		this.whitelistDirectory = whitelistDirectory;
	}


	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	public String getJvm() {
		return jvm;
	}

	public void setJvm(String jvm) {
		this.jvm = jvm;
	}

	public String getArgs() {
		return args;
	}

	public void setArgs(String args) {
		this.args = args;
	}

	public String getJavaCopyItemList() {
		return javaCopyItemList;
	}

	public void setJavaCopyItemList(String javaCopyItemList) {
		this.javaCopyItemList = javaCopyItemList;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getJdkId() {
		return jdkId;
	}

	public void setJdkId(String jdkId) {
		this.jdkId = jdkId;
	}

	public String getRunMode() {
		return runMode;
	}

	public void setRunMode(String runMode) {
		this.runMode = runMode;
	}

	public Integer getOutGivingProject() {
		return outGivingProject;
	}

	public void setOutGivingProject(Integer outGivingProject) {
		this.outGivingProject = outGivingProject;
	}

	public String getJavaExtDirsCp() {
		return javaExtDirsCp;
	}

	public void setJavaExtDirsCp(String javaExtDirsCp) {
		this.javaExtDirsCp = javaExtDirsCp;
	}
}
