/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import cn.hutool.core.date.SystemClock;
import cn.keepbx.jpom.model.BaseIdModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据基础实体
 *
 * @author jzy
 * @since 2021-08-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseDbModel extends BaseIdModel {

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

	@Override
	public String toString() {
		return super.toString();
	}
}
