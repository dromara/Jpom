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
package io.jpom.controller.monitor;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.model.PageResultDto;
import io.jpom.model.data.MonitorUserOptModel;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import io.jpom.permission.MethodFeature;
import io.jpom.service.monitor.MonitorUserOptService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 监控用户操作
 *
 * @author bwcx_jzy
 * @since 2020/08/06
 */
@RestController
@RequestMapping(value = "/monitor_user_opt")
@Feature(cls = ClassFeature.OPT_MONITOR)
public class MonitorUserOptListController extends BaseServerController {

	private final MonitorUserOptService monitorUserOptService;

	public MonitorUserOptListController(MonitorUserOptService monitorUserOptService) {
		this.monitorUserOptService = monitorUserOptService;
	}


	@RequestMapping(value = "list_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getMonitorList() {
		PageResultDto<MonitorUserOptModel> pageResultDto = monitorUserOptService.listPage(getRequest());
		return JsonMessage.getString(200, "", pageResultDto);
	}

	/**
	 * 操作监控类型列表
	 *
	 * @return json
	 */
	@RequestMapping(value = "type_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.LIST)
	public String getOperateTypeList() {
		JSONObject jsonObject = new JSONObject();
		//
		List<JSONObject> classFeatureList = Arrays.stream(ClassFeature.values())
				.filter(classFeature -> classFeature != ClassFeature.NULL)
				.map(classFeature -> {
					JSONObject jsonObject1 = new JSONObject();
					jsonObject1.put("title", classFeature.getName());
					jsonObject1.put("value", classFeature.name());
					return jsonObject1;
				})
				.collect(Collectors.toList());
		jsonObject.put("classFeature", classFeatureList);
		//
		List<JSONObject> methodFeatureList = Arrays.stream(MethodFeature.values())
				.filter(methodFeature -> methodFeature != MethodFeature.NULL && methodFeature != MethodFeature.LIST)
				.map(classFeature -> {
					JSONObject jsonObject1 = new JSONObject();
					jsonObject1.put("title", classFeature.getName());
					jsonObject1.put("value", classFeature.name());
					return jsonObject1;
				})
				.collect(Collectors.toList());
		jsonObject.put("methodFeature", methodFeatureList);

		return JsonMessage.getString(200, "success", jsonObject);
	}

	/**
	 * 删除列表
	 *
	 * @param id id
	 * @return json
	 */
	@RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.DEL)
	public String deleteMonitor(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败") String id) {
		//
		monitorUserOptService.delByKey(id, getRequest());
		return JsonMessage.getString(200, "删除成功");
	}


	/**
	 * 增加或修改监控
	 *
	 * @param id         id
	 * @param name       name
	 * @param notifyUser user
	 * @return json
	 */
	@RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String updateMonitor(String id,
								@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空") String name,
								String notifyUser,
								String monitorUser,
								String monitorOpt,
								String monitorFeature) {

		String status = getParameter("status");

		JSONArray jsonArray = JSONArray.parseArray(notifyUser);
		List<String> notifyUsers = jsonArray.toJavaList(String.class)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		Assert.notEmpty(notifyUsers, "请选择报警联系人");


		JSONArray monitorUserArray = JSONArray.parseArray(monitorUser);
		List<String> monitorUserArrays = monitorUserArray.toJavaList(String.class)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		Assert.notEmpty(monitorUserArrays, "请选择监控人员");


		JSONArray monitorOptArray = JSONArray.parseArray(monitorOpt);
		List<MethodFeature> monitorOptArrays = monitorOptArray
				.stream()
				.map(o -> EnumUtil.fromString(MethodFeature.class, StrUtil.toString(o), null))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		Assert.notEmpty(monitorOptArrays, "请选择监控的操作");

		JSONArray monitorFeatureArray = JSONArray.parseArray(monitorFeature);
		List<ClassFeature> monitorFeatureArrays = monitorFeatureArray
				.stream()
				.map(o -> EnumUtil.fromString(ClassFeature.class, StrUtil.toString(o), null))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		Assert.notEmpty(monitorFeatureArrays, "请选择监控的功能");


		boolean start = "on".equalsIgnoreCase(status);
		MonitorUserOptModel monitorModel = monitorUserOptService.getByKey(id);
		if (monitorModel == null) {
			monitorModel = new MonitorUserOptModel();
		}
		monitorModel.monitorUser(monitorUserArrays);
		monitorModel.setStatus(start);
		monitorModel.monitorOpt(monitorOptArrays);
		monitorModel.monitorFeature(monitorFeatureArrays);
		monitorModel.notifyUser(notifyUsers);
		monitorModel.setName(name);

		if (StrUtil.isEmpty(id)) {
			//添加监控
			monitorUserOptService.insert(monitorModel);
			return JsonMessage.getString(200, "添加成功");
		}
		monitorUserOptService.update(monitorModel);
		return JsonMessage.getString(200, "修改成功");
	}

	/**
	 * 开启或关闭监控
	 *
	 * @param id     id
	 * @param status 状态
	 * @return json
	 */
	@RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@Feature(method = MethodFeature.EDIT)
	public String changeStatus(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "id不能为空") String id,
							   String status) {
		MonitorUserOptModel monitorModel = monitorUserOptService.getByKey(id);
		Assert.notNull(monitorModel, "不存在监控项啦");

		boolean bStatus = Convert.toBool(status, false);
		monitorModel.setStatus(bStatus);
		monitorUserOptService.update(monitorModel);
		return JsonMessage.getString(200, "修改成功");
	}


}
