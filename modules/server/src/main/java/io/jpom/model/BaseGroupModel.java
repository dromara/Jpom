package io.jpom.model;

/**
 * @author bwcx_jzy
 * @since 2022/1/5
 */
public abstract class BaseGroupModel extends BaseWorkspaceModel {

	/**
	 * 节点分组
	 */
	private String group;

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
}
