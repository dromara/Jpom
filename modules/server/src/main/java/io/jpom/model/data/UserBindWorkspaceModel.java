package io.jpom.model.data;

import cn.hutool.crypto.SecureUtil;
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

	/**
	 * 生产绑定关系表 主键 ID
	 *
	 * @param userId      用户ID
	 * @param workspaceId 工作空间ID
	 * @return id
	 */
	public static String getId(String userId, String workspaceId) {
		return SecureUtil.sha1(userId + workspaceId);
	}
}
