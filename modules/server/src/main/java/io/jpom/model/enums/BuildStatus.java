package io.jpom.model.enums;

import io.jpom.model.BaseEnum;

/**
 * @author bwcx_jzy
 * @since 2021/8/27
 */
public enum BuildStatus implements BaseEnum {
	/**
	 *
	 */
	No(0, "未构建"),

	Ing(1, "构建中"),

	Success(2, "构建成功"),

	Error(3, "构建失败"),

	PubIng(4, "发布中"),

	PubSuccess(5, "发布成功"),

	PubError(6, "发布失败"),

	Cancel(7, "取消构建"),
	;

	private final int code;
	private final String desc;

	BuildStatus(int code, String desc) {
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
