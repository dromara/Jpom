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
//package io.jpom.service.build;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.date.DateUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.jiangzeyin.common.JsonMessage;
//import com.alibaba.fastjson.JSONArray;
//import io.jpom.build.BuildManage;
//import io.jpom.common.BaseOperService;
//import io.jpom.model.BaseEnum;
//import io.jpom.model.data.BuildModel;
//import io.jpom.model.data.UserModel;
//import io.jpom.permission.BaseDynamicService;
//import io.jpom.plugin.ClassFeature;
//import io.jpom.service.dblog.BuildInfoService;
//import io.jpom.system.ServerConfigBean;
//import org.springframework.stereotype.Service;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Objects;
//import java.util.Set;
//
///**
// * 构建service
// * <p>
// * -- @author bwcx_jzy
// * -- @date 2019/7/16
// *
// * @author Hotstrip
// * @date 2021-08-10 更新构建信息到数据库，暂时废弃这个类
// * @see BuildInfoService
// **/
//@Deprecated
//@Service
//public class BuildService extends BaseOperService<BuildModel> implements BaseDynamicService {
//
//	public BuildService() {
//		super(ServerConfigBean.BUILD);
//	}
//
//	@Override
//	public void updateItem(BuildModel buildModel) {
//		buildModel.setModifyTime(DateUtil.now());
//		super.updateItem(buildModel);
//	}
//
//	public boolean checkOutGiving(String outGivingId) {
//		List<BuildModel> list = list();
//		if (list == null || list.isEmpty()) {
//			return false;
//		}
//		for (BuildModel buildModel : list) {
//			if (buildModel.getReleaseMethod() == BuildReleaseMethod.Outgiving.getCode() &&
//					outGivingId.equals(buildModel.getReleaseMethodDataId())) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	public boolean checkNode(String nodeId) {
//		List<BuildModel> list = list();
//		if (list == null || list.isEmpty()) {
//			return false;
//		}
//		for (BuildModel buildModel : list) {
//			if (buildModel.getReleaseMethod() == BuildReleaseMethod.Project.getCode()) {
//				String releaseMethodDataId = buildModel.getReleaseMethodDataId();
//				if (StrUtil.startWith(releaseMethodDataId, nodeId + ":")) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}
//
//	public boolean checkNodeProjectId(String nodeId, String projectId) {
//		List<BuildModel> list = list();
//		if (list == null || list.isEmpty()) {
//			return false;
//		}
//		for (BuildModel buildModel : list) {
//			if (buildModel.getReleaseMethod() == BuildReleaseMethod.Project.getCode()) {
//				String releaseMethodDataId = buildModel.getReleaseMethodDataId();
//				if (StrUtil.equals(releaseMethodDataId, nodeId + ":" + projectId)) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}
//
//	@Override
//	public <E> List<E> list(Class<E> cls) {
//		List<E> list = super.list(cls);
//		JSONArray jsonArray = ((JSONArray) JSONArray.toJSON(list));
//		jsonArray = filter(jsonArray, ClassFeature.BUILD);
//		if (jsonArray == null) {
//			return null;
//		}
//		return jsonArray.toJavaList(cls);
//	}
//
//	@Override
//	public JSONArray listToArray(String dataId) {
//		return (JSONArray) JSONArray.toJSON(this.list());
//	}
//
//	/**
//	 * 开始构建
//	 *
//	 * @param userModel 用户
//	 * @param id        id
//	 * @return json
//	 */
//	public String start(UserModel userModel, String id) {
//		BuildModel item = getItem(id);
//		if (item == null) {
//			return JsonMessage.getString(404, "没有对应数据");
//		}
//		String e = checkStatus(item.getStatus());
//		if (e != null) {
//			return e;
//		}
//		//
//		item.setBuildId(item.getBuildId() + 1);
//		String optUserName = userModel == null ? "openApi" : UserModel.getOptUserName(userModel);
//		item.setModifyUser(optUserName);
//		this.updateItem(item);
//		BuildManage.create(item, userModel);
//		return JsonMessage.getString(200, "开始构建中", item.getBuildId());
//	}
//
//	public String checkStatus(int status) {
//		BuildStatus nowStatus = BaseEnum.getEnum(BuildStatus.class, status);
//		Objects.requireNonNull(nowStatus);
//		if (BuildStatus.Ing == nowStatus ||
//				BuildStatus.PubIng == nowStatus) {
//			return JsonMessage.getString(501, "当前还在：" + nowStatus.getDesc());
//		}
//		return null;
//	}
//
//	/**
//	 * 构建所有分组
//	 */
//	public Set<String> listGroup() {
//		List<BuildModel> list = list();
//		Set<String> set = new HashSet<>();
//		if (CollUtil.isEmpty(list)) {
//			return set;
//		}
//		for (BuildModel buildModel : list) {
//			String group = buildModel.getGroup();
//			if (StrUtil.isNotEmpty(group)) {
//				set.add(group);
//			}
//		}
//		return set;
//	}
//}
