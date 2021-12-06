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
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import io.jpom.common.ServerOpenApi;
import io.jpom.common.interceptor.NotLogin;
import io.jpom.controller.build.BuildInfoTriggerController;
import io.jpom.model.data.BuildInfoModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.user.UserService;
import org.h2.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/9/4
 */
@RestController
@NotLogin
public class BuildTriggerApiController {

	private final BuildInfoService buildInfoService;

	private final UserService userService;

	public BuildTriggerApiController(BuildInfoService buildInfoService,
									 UserService userService) {
		this.buildInfoService = buildInfoService;
		this.userService = userService;
	}

	private UserModel getByUrlToken(String token) {
		String digestCountStr = StrUtil.sub(token, 0, BuildInfoTriggerController.BUILD_INFO_TRIGGER_TOKEN_FILL_LEN);
		String result = StrUtil.subSuf(token, BuildInfoTriggerController.BUILD_INFO_TRIGGER_TOKEN_FILL_LEN);
		int digestCount = Convert.toInt(digestCountStr, 1);

		String sql = "select HASH('SHA256', id,?) as token,id from user_info";
		List<Entity> query = userService.query(sql, digestCount);
		if (query == null) {
			return null;
		}
		String userId = query.stream()
				.filter(entity -> {
					Object token1 = entity.get("token");
					String sha256;
					if (token1 instanceof byte[]) {
						byte[] bytes = (byte[]) token1;
						sha256 = StringUtils.convertBytesToHex(bytes);
					} else {
						sha256 = ObjectUtil.toString(token1);
					}
					return StrUtil.equals(result, sha256);
				})
				.map(entity -> StrUtil.toString(entity.get("id")))
				.findFirst()
				.orElseGet(() -> "没有对应数据:-2");
		return userService.getByKey(userId);
	}


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
		UserModel userModel = this.getByUrlToken(token);
		//
		Assert.notNull(userModel, "没有对应数据:-1");

		Assert.state(StrUtil.equals(token, item.getTriggerToken()), "触发token错误,或者已经失效");

		return buildInfoService.start(item, userModel, Convert.toInt(delay, 0));
	}
}
