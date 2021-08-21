package io.jpom.model.data;

import io.jpom.model.BaseDbModel;
import io.jpom.service.h2db.TableName;

/**
 * @author Hotstrip
 * new BuildModel class, for replace old BuildModel
 * @see BuildModel
 */
@TableName("BUILD_INFO")
public class BuildInfoModel extends BaseDbModel {

	/**
	 * 仓库 ID
	 */
	private String repositoryId;
	/**
	 * 名称
	 */
	private String name;
	/**
	 * 构建 ID
	 */
	private Integer buildId;
	/**
	 * 分组名称
	 */
	private String group;
	/**
	 * 分支
	 */
	private String branchName;
	/**
	 * 构建命令
	 */
	private String script;
	/**
	 * 构建产物目录
	 */
	private String resultDirFile;
	/**
	 * 发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}
	 */
	private Integer releaseMethod;
	/**
	 * 修改人
	 */
	private String modifyUser;
	/**
	 * 状态
	 */
	private Integer status;
	/**
	 * 额外信息，JSON 字符串格式
	 */
	private String extraData;

	public String getRepositoryId() {
		return repositoryId;
	}

	public void setRepositoryId(String repositoryId) {
		this.repositoryId = repositoryId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getBuildId() {
		return buildId;
	}

	public void setBuildId(Integer buildId) {
		this.buildId = buildId;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getBranchName() {
		return branchName;
	}

	public void setBranchName(String branchName) {
		this.branchName = branchName;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public String getResultDirFile() {
		return resultDirFile;
	}

	public void setResultDirFile(String resultDirFile) {
		this.resultDirFile = resultDirFile;
	}

	public Integer getReleaseMethod() {
		return releaseMethod;
	}

	public void setReleaseMethod(Integer releaseMethod) {
		this.releaseMethod = releaseMethod;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}

	public String getModifyUser() {
		return modifyUser;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getExtraData() {
		return extraData;
	}

	public void setExtraData(String extraData) {
		this.extraData = extraData;
	}

}
