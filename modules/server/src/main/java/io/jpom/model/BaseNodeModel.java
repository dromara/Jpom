package io.jpom.model;

/**
 * 节点 数据
 *
 * @author bwcx_jzy
 * @since 2021/12/05
 */
public abstract class BaseNodeModel extends BaseWorkspaceModel {

	/**
	 * 节点Id
	 *
	 * @see io.jpom.model.data.NodeModel
	 */
	private String nodeId;

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
}
