package io.jpom.model.data;

import io.jpom.model.BaseDbModel;
import io.jpom.service.h2db.TableName;

/**
 * @author bwcx_jzy
 * @since 2021/12/4
 */
@TableName("USER_BIND_WORKSPACE")
public class UserBindWorkspaceModel extends BaseDbModel {

	private String userId;

	private String workspaceId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}
}
