package io.jpom.model.data;

import io.jpom.model.BaseStrikeDbModel;
import io.jpom.service.h2db.TableName;

/**
 * 工作空间
 *
 * @author bwcx_jzy
 * @since 2021/12/3
 */
@TableName("WORKSPACE")
public class WorkspaceModel extends BaseStrikeDbModel {

	/**
	 * 名称
	 */
	private String name;

	/**
	 * 描述
	 */
	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
