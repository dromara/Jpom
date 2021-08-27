package io.jpom.model.enums;

import io.jpom.model.BaseEnum;

/**
 * @author bwcx_jzy
 * @since 2021/8/27
 */
public enum BuildReleaseMethod implements BaseEnum {
	/**
	 * 发布
	 */
	No(0, "不发布"),
	Outgiving(1, "节点分发"),
	Project(2, "项目"),
	Ssh(3, "SSH"),
	;
	private final int code;
	private final String desc;

	BuildReleaseMethod(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDesc() {
		return desc;
	}
}
