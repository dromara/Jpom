/*
 * Copyright (c) 2019 Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.model.dto;

import lombok.Data;
import org.dromara.jpom.controller.user.UserWorkspaceModel;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2020/11/2
 */
@Data
public class UserLoginDto {

	private String token;

	private String longTermToken;

	private List<UserWorkspaceModel> bindWorkspaceModels;


	public UserLoginDto() {
	}

	public UserLoginDto(String token, String jwtId) {
		this.setLongTermToken(jwtId);
		this.setToken(token);
	}
}
