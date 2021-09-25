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
package io.jpom.controller.openapi.build;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author bwcx_jzy
 * @date 2019/9/4
 */
@RestController
@NotLogin
public class BuildTriggerApiController {
//
//	@Resource
//	private BuildService buildService;

	@Resource
	private BuildInfoService buildInfoService;

	@Resource
	private UserService userService;
//
//	@RequestMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD, produces = MediaType.APPLICATION_JSON_VALUE)
//	public String trigger(@PathVariable String id, @PathVariable String token) {
//		BuildModel item = buildService.getItem(id);
//		if (item == null) {
//			return JsonMessage.getString(404, "没有对应数据");
//		}
//		List<UserModel> list = userService.list(false);
//		Optional<UserModel> first = list.stream().filter(UserModel::isSystemUser).findFirst();
//		if (!first.isPresent()) {
//			return JsonMessage.getString(404, "没有对应数据");
//		}
//		UserModel userModel = first.get();
//		if (!StrUtil.equals(token, item.getTriggerToken())) {
//			return JsonMessage.getString(404, "触发token错误");
//		}
//		return buildService.start(userModel, id);
//	}


	/**
	 * 构建触发器
	 *
	 * @param id    构建ID
	 * @param token 构建的token
	 * @param delay 延迟时间（单位秒）
	 * @return json
	 */
	@RequestMapping(value = ServerOpenApi.BUILD_TRIGGER_BUILD2, produces = MediaType.APPLICATION_JSON_VALUE)
	public String trigger2(@PathVariable String id, @PathVariable String token, String delay) {
		BuildInfoModel item = buildInfoService.getByKey(id);
		Assert.notNull(item, "没有对应数据");
		List<UserModel> list = userService.list(false);
		//
		Optional<UserModel> first = list.stream().filter(UserModel::isSystemUser).findFirst();
		Assert.state(first.isPresent(), "没有对应数据:-1");

		Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误");

		return buildInfoService.start(item, first.get(), Convert.toInt(delay, 0));
	}
}
