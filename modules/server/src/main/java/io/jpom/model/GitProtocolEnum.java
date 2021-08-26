package io.jpom.model;

/**
 * Git protocol
 * @author Hotstrip
 * @since 2021-08-26
 */
public enum  GitProtocolEnum {
	HTTP(0, "HTTP(s)"),
	SSH(1, "SSH"),
	;

	GitProtocolEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	int code;
	String desc;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
}
