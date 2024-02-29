/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2022/1/5
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseGroupModel extends BaseWorkspaceModel {

	/**
	 * 分组
	 */
	private String group;

	@Override
	public String toString() {
		return super.toString();
	}

}
