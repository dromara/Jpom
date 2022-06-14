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
package io.jpom.controller.manage;

import cn.hutool.core.io.FileUtil;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseAgentController;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.RunMode;
import io.jpom.model.data.NodeProjectInfoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

/**
 * 管理的信息获取接口
 *
 * @author jiangzeyin
 * @since 2019/4/16
 */
@RestController
@RequestMapping(value = "/manage/")
@Slf4j
public class ProjectListController extends BaseAgentController {

	/**
	 * 获取项目的信息
	 *
	 * @param id id
	 * @return item
	 * @see NodeProjectInfoModel
	 */
	@RequestMapping(value = "getProjectItem", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getProjectItem(String id) {
		NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(id);
		if (nodeProjectInfoModel != null) {
			RunMode runMode = nodeProjectInfoModel.getRunMode();
			if (runMode != RunMode.Dsl && runMode != RunMode.File) {
				// 返回实际执行的命令
				String command = AbstractProjectCommander.getInstance().buildJavaCommand(nodeProjectInfoModel, null);
				nodeProjectInfoModel.setRunCommand(command);
			}
			//
			List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = nodeProjectInfoModel.getJavaCopyItemList();
			if (javaCopyItemList != null) {
				for (NodeProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
					File logBack = nodeProjectInfoModel.getLogBack(javaCopyItem);
					File log = nodeProjectInfoModel.getLog(javaCopyItem);
					javaCopyItem.setLogBack(FileUtil.getAbsolutePath(logBack));
					javaCopyItem.setLog(FileUtil.getAbsolutePath(log));
				}
			}
		}


		return JsonMessage.getString(200, "", nodeProjectInfoModel);
	}

	/**
	 * 程序项目信息
	 *
	 * @return json
	 */
	@RequestMapping(value = "getProjectInfo", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getProjectInfo() {
		try {
			// 查询数据
			List<NodeProjectInfoModel> nodeProjectInfoModels = projectInfoService.list();
			return JsonMessage.getString(200, "查询成功！", nodeProjectInfoModels);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return JsonMessage.getString(500, "查询异常：" + e.getMessage());
		}
	}

	/**
	 * 展示项目页面
	 */
	@RequestMapping(value = "project_copy_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String projectCopyList(String id) {
		NodeProjectInfoModel nodeProjectInfoModel = projectInfoService.getItem(id);
		Assert.notNull(nodeProjectInfoModel, "没有对应项目");

		List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = nodeProjectInfoModel.getJavaCopyItemList();
		Assert.notEmpty(javaCopyItemList, "对应项目没有副本集");
		//		JSONArray array = new JSONArray();
		//		for (NodeProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
		//			JSONObject object = javaCopyItem.toJson();
		//			boolean run = AbstractProjectCommander.getInstance().isRun(nodeProjectInfoModel, javaCopyItem);
		//			object.put("status", run);
		//			array.add(object);
		//		}
		return JsonMessage.getString(200, "", javaCopyItemList);
	}
}
