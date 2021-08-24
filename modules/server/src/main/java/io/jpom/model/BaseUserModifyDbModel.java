package io.jpom.model;

/**
 * @author bwcx_jzy
 * @since 2021/8/24
 */
public abstract class BaseUserModifyDbModel extends BaseDbModel {
	/**
	 * 修改人
	 */
	private String modifyUser;

	public String getModifyUser() {
		return modifyUser;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}
}
