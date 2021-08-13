package io.jpom.model;

import cn.hutool.core.date.SystemClock;

/**
 * 数据基础实体
 *
 * @author jzy
 * @since 2021-08-13
 */
public abstract class BaseDbModel extends BaseJsonModel {

	/**
	 * 主键
	 */
	private String id;

	/**
	 * 数据创建时间
	 *
	 * @see SystemClock#now()
	 */
	private Long createTimeMillis;

	/**
	 * 数据修改时间
	 */
	private Long modifyTimeMillis;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Long getCreateTimeMillis() {
		return createTimeMillis;
	}

	public void setCreateTimeMillis(Long createTimeMillis) {
		this.createTimeMillis = createTimeMillis;
	}

	public Long getModifyTimeMillis() {
		return modifyTimeMillis;
	}

	public void setModifyTimeMillis(Long modifyTimeMillis) {
		this.modifyTimeMillis = modifyTimeMillis;
	}
}
