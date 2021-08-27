package io.jpom.model.enums;

import io.jpom.model.BaseEnum;

/**
 * Git protocol
 *
 * @author Hotstrip
 * @since 2021-08-26
 */
public enum GitProtocolEnum implements BaseEnum {
	/**
	 * http
	 */
	HTTP(0, "HTTP(s)"),
	SSH(1, "SSH"),
	;

	GitProtocolEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	int code;
	String desc;

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDesc() {
		return desc;
	}

}
