/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
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
package io.jpom.controller.script;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.PermissionInterceptor;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.UserModel;
import io.jpom.model.script.ScriptModel;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.service.script.ScriptServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author bwcx_jzy
 * @since 2022/1/19
 */
@RestController
@RequestMapping(value = "/script")
@Feature(cls = ClassFeature.SCRIPT)
public class ScriptController extends BaseServerController {

	private final ScriptServer scriptServer;

	public ScriptController(ScriptServer scriptServer) {
		this.scriptServer = scriptServer;
	}

	/**
	 * get script list
	 *
	 * @return json
	 */
	@RequestMapping(value = "list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String scriptList() {
		PageResultDto<ScriptModel> pageResultDto = scriptServer.listPage(getRequest());
		return JsonMessage.getString(200, "success", pageResultDto);
	}

	@RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String save(String id,
					   @ValidatorItem String context,
					   @ValidatorItem String name,
					   String autoExecCron,
					   String defArgs,
					   String description) {
		ScriptModel scriptModel = new ScriptModel();
		scriptModel.setId(id);
		scriptModel.setContext(context);
		scriptModel.setName(name);
		scriptModel.setDescription(description);
		scriptModel.setDefArgs(defArgs);

		Assert.hasText(scriptModel.getContext(), "内容为空");
		//
		if (StrUtil.isNotEmpty(autoExecCron)) {
			UserModel user = getUser();
			Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
			try {
				new CronPattern(autoExecCron);
			} catch (Exception e) {
				throw new IllegalArgumentException("定时执行表达式格式不正确");
			}
			scriptModel.setAutoExecCron(autoExecCron);
		} else {
			scriptModel.setAutoExecCron(StrUtil.EMPTY);
		}
		//
		if (StrUtil.isEmpty(id)) {
			scriptServer.insert(scriptModel);
			return JsonMessage.getString(200, "添加成功");
		}
		scriptServer.updateById(scriptModel, getRequest());
		return JsonMessage.getString(200, "修改成功");
	}

	@RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String del(String id) {
		ScriptModel server = scriptServer.getByKey(id);
		if (server != null) {
			File file = server.scriptPath();
			boolean del = FileUtil.del(file);
			Assert.state(del, "清理脚本文件失败");
			scriptServer.delByKey(id);
		}
		return JsonMessage.getString(200, "删除成功");
	}
}
