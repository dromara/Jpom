package io.jpom.model;

import io.jpom.model.data.WorkspaceModel;

/**
 * 工作空间 数据
 *
 * @author bwcx_jzy
 * @since 2021/12/04
 */
public abstract class BaseWorkspaceModel extends BaseStrikeDbModel {

	/**
	 * 工作空间ID
	 *
	 * @see io.jpom.model.data.WorkspaceModel
	 * @see WorkspaceModel#REQ_HEADER
	 */
	private String workspaceId;

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}
}
