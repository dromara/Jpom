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
package io.jpom.system.init;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.service.manage.ProjectInfoService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自动启动项目
 *
 * @author bwcx_jzy
 * @since 2021/12/10
 */
@PreLoadClass
@Slf4j
public class AutoStartProject {

	@PreLoadMethod
	private static void start() {
		ProjectInfoService projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
		List<NodeProjectInfoModel> list = projectInfoService.list();
		if (CollUtil.isEmpty(list)) {
			return;
		}
		list = list.stream().filter(nodeProjectInfoModel -> nodeProjectInfoModel.getAutoStart() != null && nodeProjectInfoModel.getAutoStart()).collect(Collectors.toList());
		List<NodeProjectInfoModel> finalList = list;
		ThreadUtil.execute(() -> {
			AbstractProjectCommander instance = AbstractProjectCommander.getInstance();
			for (NodeProjectInfoModel nodeProjectInfoModel : finalList) {
				try {
					if (!instance.isRun(nodeProjectInfoModel, null)) {
						instance.start(nodeProjectInfoModel, null);
					}
					List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = nodeProjectInfoModel.getJavaCopyItemList();
					if (javaCopyItemList != null) {
						for (NodeProjectInfoModel.JavaCopyItem javaCopyItem : javaCopyItemList) {
							if (!instance.isRun(nodeProjectInfoModel, javaCopyItem)) {
								instance.start(nodeProjectInfoModel, javaCopyItem);
							}
						}
					}
				} catch (Exception e) {
					log.warn("自动启动项目失败：{} {}", nodeProjectInfoModel.getId(), e.getMessage());
				}
			}
		});
	}
}
