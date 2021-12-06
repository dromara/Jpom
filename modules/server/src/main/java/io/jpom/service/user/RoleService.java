///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 码之科技工作室
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package io.jpom.service.user;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.date.DateTime;
//import cn.jiangzeyin.common.spring.SpringUtil;
//import com.alibaba.fastjson.JSONArray;
//import io.jpom.JpomApplication;
//import io.jpom.common.BaseOperService;
//import io.jpom.model.data.RoleModel;
//import io.jpom.model.data.UserModel;
//import io.jpom.permission.BaseDynamicService;
//import io.jpom.permission.DynamicData;
//import io.jpom.plugin.ClassFeature;
//import io.jpom.plugin.MethodFeature;
//import io.jpom.system.ServerConfigBean;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//
///**
// * @author bwcx_jzy
// * @date 2019/8/15
// */
//@Service
//public class RoleService extends BaseOperService<RoleModel> {
//
//	public RoleService() {
//		super(ServerConfigBean.ROLE);
//	}
//
//	@Override
//	public void addItem(RoleModel roleModel) {
//		roleModel.setUpdateTime(DateTime.now().toString());
//		super.addItem(roleModel);
//	}
//
//	@Override
//	public void updateItem(RoleModel roleModel) {
//		roleModel.setUpdateTime(DateTime.now().toString());
//		super.updateItem(roleModel);
//	}
//
//	/**
//	 * 获取角色动态数据
//	 *
//	 * @param roleId       角色id
//	 * @param classFeature 功能
//	 * @param dataId       数据id
//	 * @return list
//	 */
//	public List<String> listDynamicData(String roleId, ClassFeature classFeature, String dataId) {
//		RoleModel item = getItem(roleId);
//		if (item == null) {
//			return null;
//		}
//		Set<String> treeData = item.getTreeData(classFeature, dataId);
//		return new ArrayList<>(treeData);
//	}
//
//	/**
//	 * 调用对应功能动态数据list
//	 *
//	 * @param roleId       角色id
//	 * @param classFeature 功能
//	 * @param dataId       数据id
//	 * @return array
//	 */
//	public JSONArray listDynamic(String roleId, ClassFeature classFeature, String dataId) {
//		DynamicData dynamicData = DynamicData.getDynamicData(classFeature);
//		Objects.requireNonNull(dynamicData, "没有配置对应动态数据");
//		Class<? extends BaseDynamicService> baseOperService = dynamicData.getBaseOperService();
//		BaseDynamicService bean = SpringUtil.getBean(baseOperService);
//		return bean.listDynamic(classFeature, roleId, dataId);
//	}
//
//
//	public boolean errorDynamicPermission(UserModel userModel, ClassFeature classFeature, String dataId) {
//		if (JpomApplication.SYSTEM_ID.equals(dataId)) {
//			// 系统构建id
//			return false;
//		}
//		DynamicData dynamicData1 = DynamicData.getDynamicData(classFeature);
//		if (dynamicData1 == null) {
//			// 如果不是没有动态权限  就默认通过
//			return false;
//		}
//		if (userModel.isSystemUser()) {
//			return false;
//		}
//		Set<String> roles = null;
//		if (roles == null || roles.isEmpty()) {
//			return true;
//		}
//		for (String role : roles) {
//			RoleModel item = getItem(role);
//			if (item == null) {
//				continue;
//			}
//			// 判断权限
//			if (item.contains(classFeature, dataId)) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	/**
//	 * 用户是否有添加权限
//	 *
//	 * @param userModel 用户
//	 * @return true 可以添加
//	 */
//	public boolean canAdd(UserModel userModel) {
//		if (userModel.isSystemUser()) {
//			return true;
//		}
//		Set<String> roles = null;
//		if (CollUtil.isEmpty(roles)) {
//			return false;
//		}
//		for (String role : roles) {
//			RoleModel item = getItem(role);
//			if (item == null) {
//				continue;
//			}
//			if (item.getCanAdd() != null && item.getCanAdd()) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	/**
//	 * 没有有对应功能方法的权限
//	 *
//	 * @param userModel     用户
//	 * @param classFeature  功能
//	 * @param methodFeature 方法
//	 * @return false 有权限  true 没有权限
//	 */
//	public boolean errorMethodPermission(UserModel userModel, ClassFeature classFeature, MethodFeature methodFeature) {
//		if (userModel.isSystemUser()) {
//			return false;
//		}
//		Set<String> roles = null;
//		if (roles == null || roles.isEmpty()) {
//			return true;
//		}
//		for (String role : roles) {
//			RoleModel item = getItem(role);
//			if (item == null) {
//				continue;
//			}
//			List<MethodFeature> methodFeatures = item.getMethodFeature(classFeature);
//			if (methodFeatures == null) {
//				continue;
//			}
//			if (methodFeatures.contains(methodFeature)) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	public Set<String> getDynamicList(UserModel userModel, ClassFeature classFeature, String parentId) {
//		Set<String> roles = null;
//		if (roles == null || roles.isEmpty()) {
//			return null;
//		}
//		Set<String> allData = new HashSet<>();
//		for (String role : roles) {
//			RoleModel item = getItem(role);
//			if (item == null) {
//				continue;
//			}
//			Map<ClassFeature, List<RoleModel.TreeLevel>> dynamicData = item.getDynamicData2();
//			if (dynamicData == null) {
//				continue;
//			}
//			Set<String> list = item.getTreeData(classFeature, parentId);
//			if (list != null) {
//				allData.addAll(list);
//			}
//		}
//		return allData;
//	}
//}
