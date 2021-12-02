/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
