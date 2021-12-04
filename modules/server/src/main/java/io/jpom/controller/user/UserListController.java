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
package io.jpom.controller.user;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.UserBindWorkspaceModel;
import io.jpom.model.data.UserModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.user.UserBindWorkspaceService;
import io.jpom.service.user.UserService;
import io.jpom.system.ServerExtConfigBean;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户列表
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
public class UserListController extends BaseServerController {

	private final UserService userService;
	private final UserBindWorkspaceService userBindWorkspaceService;

	public UserListController(UserService userService,
							  UserBindWorkspaceService userBindWorkspaceService) {
		this.userService = userService;
		this.userBindWorkspaceService = userBindWorkspaceService;
	}


	/**
	 * 查询所有用户
	 *
	 * @return json
	 */
	@RequestMapping(value = "getUserList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String getUserList() {
		PageResultDto<UserModel> userModelPageResultDto = userService.listPage(getRequest());
		return JsonMessage.getString(200, "", userModelPageResultDto);
	}

	/**
	 * 获取所有管理员信息
	 *
	 * @return json
	 * @author Hotstrip
	 * get all admin user list
	 */
	@RequestMapping(value = "get-admin-user-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LIST)
	public String getAdminUserList() {
		List<UserModel> list = userService.list(false);
		JSONArray jsonArray = new JSONArray();
		list.forEach(userModel -> {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("title", userModel.getName());
			jsonObject.put("value", userModel.getId());
			if (StrUtil.isAllEmpty(userModel.getEmail(), userModel.getDingDing(), userModel.getWorkWx())) {
				jsonObject.put("disabled", true);
			}
			jsonArray.add(jsonObject);
		});
		return JsonMessage.getString(200, "success", jsonArray);
	}

	/**
	 * 编辑用户
	 *
	 * @param type 操作类型
	 * @return String
	 */
	@PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String addUser(String type) {
		//
		boolean create = StrUtil.equals(type, "add");
		UserModel userModel = this.parseUser(create);

		if (create) {
			userService.insert(userModel);
		} else {
			userService.update(userModel);
		}
		//
		String workspace = getParameter("workspace");
		JSONArray jsonArray = JSONArray.parseArray(workspace);
		List<String> workspaceList = jsonArray.toJavaList(String.class);
		userBindWorkspaceService.updateUserWorkspace(userModel.getId(), workspaceList);
		return JsonMessage.getString(200, "操作成功");
	}

	private UserModel parseUser(boolean create) {
		String id = getParameter("id");
		Assert.hasText(id, "登录名不能为空");
		int length = id.length();
		Assert.state(length <= 20 && length >= UserModel.USER_NAME_MIN_LEN, "登录名不能为空,并且长度必须" + UserModel.USER_NAME_MIN_LEN + "-20");

		Assert.state(!StrUtil.equalsAnyIgnoreCase(id, UserModel.SYSTEM_OCCUPY_NAME, UserModel.SYSTEM_ADMIN), "当前登录名已经被系统占用");

		Validator.validateGeneral(id, "登录名不能包含特殊字符");
		UserModel userModel = new UserModel();
		UserModel optUser = getUser();
		if (create) {
			long size = userService.count();
			Assert.state(size <= ServerExtConfigBean.getInstance().userMaxCount, "当前用户个数超过系统上限");
			// 登录名重复
			boolean exists = userService.exists(new UserModel(id));
			Assert.state(!exists, "登录名已经存在");
			userModel.setParent(optUser.getId());
		}
		userModel.setId(id);
		//
		String name = getParameter("name");
		Assert.hasText(name, "请输入账户昵称");
		int len = name.length();
		Assert.state(len <= 10 && len >= 2, "昵称长度只能是2-10");

		userModel.setName(name);

		String password = getParameter("password");
		if (create || StrUtil.isNotEmpty(password)) {
			Assert.hasText(password, "密码不能为空");
			// 修改用户
			Assert.state(create || optUser.isSystemUser(), "只有系统管理员才能重置用户密码");
			userModel.setSalt(userService.generateSalt());
			userModel.setPassword(SecureUtil.sha1(password + userModel.getSalt()));
		}
		int systemUser = getParameterInt("systemUser", 0);
		userModel.setSystemUser(systemUser);
		return userModel;
	}

	/**
	 * 查询用户工作空间
	 *
	 * @return json
	 */
	@GetMapping(value = "workspace_list", produces = MediaType.APPLICATION_JSON_VALUE)
	public String workspaceList(@ValidatorItem String userId) {
		List<UserBindWorkspaceModel> workspaceModels = userBindWorkspaceService.listUserWorkspace(userId);
		return JsonMessage.getString(200, "", workspaceModels);
	}
}
