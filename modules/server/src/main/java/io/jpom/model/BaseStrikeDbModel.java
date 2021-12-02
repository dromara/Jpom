package io.jpom.model;

/**
 * 带逻辑删除 数据表实体
 *
 * @author bwcx_jzy
 * @since 2021/12/2
 */
public abstract class BaseStrikeDbModel extends BaseUserModifyDbModel {

	/**
	 * 逻辑删除  1 删除  0 未删除
	 */
	private Integer strike;

	public Integer getStrike() {
		return strike;
	}

	public void setStrike(Integer strike) {
		this.strike = strike;
	}
}
