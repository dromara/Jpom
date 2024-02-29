/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.enums;

import org.dromara.jpom.model.BaseEnum;

/**
 * backup type
 *
 * @author Hotstrip
 * @since 2021-11-24
 */
public enum BackupTypeEnum implements BaseEnum {
	/**
	 * 备份类型{0: 全量, 1: 部分}
	 */
	ALL(0, "全量备份"),
	PART(1, "部分备份"),
	IMPORT(2, "导入备份"),
	AUTO(3, "自动备份"),
	;

	BackupTypeEnum(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	final int code;
	final String desc;

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDesc() {
		return desc;
	}

}
