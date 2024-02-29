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

import cn.keepbx.jpom.model.BaseIdModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基础实体（带id）
 *
 * @author bwcx_jzy
 * @since 2019/3/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseModel extends BaseIdModel {

	private String name;

	@Override
	public String toString() {
		return super.toString();
	}
}
