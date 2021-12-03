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
package io.jpom.controller.user.log;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.DbUserOperateLogService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * 用户操作日志
 *
 * @author jiangzeyin
 * @date 2019/4/19
 */
@Controller
@RequestMapping(value = "/user/log")
@Feature(cls = ClassFeature.USER)
public class UserOptLogController extends BaseServerController {

	@Resource
	private DbUserOperateLogService dbUserOperateLogService;

	/**
	 * 展示用户列表
	 *
	 * @param limit 大小
	 * @param page  page
	 * @return json
	 */
	@RequestMapping(value = "list_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.LOG)
	public String listData(
			@ValidatorConfig(value = {
					@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
			}, defaultVal = "10") int limit,
			@ValidatorConfig(value = {
					@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
			}, defaultVal = "1") int page) {

		Page pageObj = new Page(page, limit);
		Entity entity = Entity.create();
		this.doPage(pageObj, entity, "optTime");

		String selectNode = getParameter("selectNode");
		if (StrUtil.isNotEmpty(selectNode)) {
			entity.set("nodeId".toUpperCase(), selectNode);
		}

		String selectUser = getParameter("selectUser");
		if (StrUtil.isNotEmpty(selectUser)) {
			entity.set("userId".toUpperCase(), selectUser);
		}

		PageResultDto<UserOperateLogV1> pageResult = dbUserOperateLogService.listPage(entity, pageObj);
		return JsonMessage.getString(200, "获取成功", pageResult);
	}
}
