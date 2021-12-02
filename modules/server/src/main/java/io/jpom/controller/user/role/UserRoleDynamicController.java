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
package io.jpom.controller.user.role;

import cn.hutool.core.collection.CollUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.data.RoleModel;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.permission.BaseDynamicService;
import io.jpom.permission.DynamicData;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.user.RoleService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author bwcx_jzy
 * @date 2019/8/15
 */
@Controller
@RequestMapping(value = "/user/role")
@Feature(cls = ClassFeature.USER_ROLE)
public class UserRoleDynamicController extends BaseServerController {

	@Resource
	private RoleService roleService;

//    @RequestMapping(value = "dynamicData.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String list() {
//        Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
//        setAttribute("dynamicDataMap", dynamicDataMap);
//        return "user/role/dynamicData";
//    }

	/**
	 * @return
	 * @author Hotstrip
	 * load role dynamic data
	 * 加载角色的动态资源数据
	 */
	@RequestMapping(value = "dynamic-list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String roleDynamicData() {
		Map<ClassFeature, DynamicData> dynamicDataMap = DynamicData.getDynamicDataMap();
		List<Map<String, String>> list = new ArrayList<>();
		dynamicDataMap.keySet().forEach(key -> {
			HashMap<String, String> valueMap = new HashMap<>();
			valueMap.put("id", key.name());
			valueMap.put("name", key.getName());
			if (key.getParent() != null) {
				valueMap.put("parent", key.getParent().name());
			}
			list.add(valueMap);
		});
		return JsonMessage.getString(200, "success", list);
	}

	@RequestMapping(value = "getDynamic.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	public String getDynamic(String id, String dynamic) {
		ClassFeature classFeature = ClassFeature.valueOf(dynamic);
		JSONArray jsonArray = roleService.listDynamic(id, classFeature, null);
		return JsonMessage.getString(200, "", jsonArray);
	}

	@RequestMapping(value = "saveDynamic.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Feature(method = MethodFeature.EDIT)
	@OptLog(value = UserOperateLogV1.OptType.EditRole)
	public String saveDynamic(String id, String dynamic) {
		RoleModel item = roleService.getItem(id);
		if (item == null) {
			return JsonMessage.getString(404, "角色信息错误");
		}
		//
		JSONObject jsonObject = JSONObject.parseObject(dynamic);
		Map<ClassFeature, List<RoleModel.TreeLevel>> dynamicData1 = new HashMap<>(jsonObject.keySet().size());
		//
		List<ClassFeature> root = DynamicData.getRoot();
		for (ClassFeature classFeature : root) {
			JSONArray value = jsonObject.getJSONArray(classFeature.name());
			if (value == null || value.isEmpty()) {
				continue;
			}
			DynamicData dynamicData = DynamicData.getDynamicData(classFeature);
			if (dynamicData == null) {
				return JsonMessage.getString(404, classFeature.getName() + "没有配置对应动态数据");
			}
			Class<? extends BaseDynamicService> baseOperService = dynamicData.getBaseOperService();
			BaseDynamicService bean = SpringUtil.getBean(baseOperService);
			List<RoleModel.TreeLevel> list = bean.parserValue(classFeature, value);
			if (CollUtil.isEmpty(list)) {
				continue;
			}
			if (classFeature.getParent() != null) {
				list = list.stream().filter(treeLevel -> CollUtil.isNotEmpty(treeLevel.getChildren())).collect(Collectors.toList());
			}
			dynamicData1.put(classFeature, list);
		}
		item.setDynamicData2(dynamicData1);
		roleService.updateItem(item);
		return JsonMessage.getString(200, "保存成功");
	}
}
