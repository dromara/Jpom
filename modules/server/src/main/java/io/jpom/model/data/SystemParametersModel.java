package io.jpom.model.data;

import io.jpom.model.BaseStrikeDbModel;
import io.jpom.service.h2db.TableName;
import io.jpom.util.StringUtil;

/**
 * 系统参数
 *
 * @author bwcx_jzy
 * @since 2021/12/2
 */
@TableName("SYSTEM_PARAMETERS")
public class SystemParametersModel extends BaseStrikeDbModel {

	/**
	 * 参数值
	 */
	private String value;
	/**
	 * 参数描述
	 */
	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public <T> T jsonToBean(Class<T> cls) {
		return StringUtil.jsonConvert(this.getValue(), cls);
	}
}
