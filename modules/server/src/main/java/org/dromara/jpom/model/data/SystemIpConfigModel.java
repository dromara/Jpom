/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.data;

import cn.keepbx.jpom.model.BaseJsonModel;

/**
 * @author bwcx_jzy
 * @since 2021/4/18
 */
public class SystemIpConfigModel extends BaseJsonModel {

	public static final String ID = "IP_CONFIG";

	/**
	 * ip 授权  允许访问
	 */
	private String allowed;

	/**
	 * 禁止
	 */
	private String prohibited;

	public String getAllowed() {
		return allowed;
	}

	public void setAllowed(String allowed) {
		this.allowed = allowed;
	}

	public String getProhibited() {
		return prohibited;
	}

	public void setProhibited(String prohibited) {
		this.prohibited = prohibited;
	}
}
