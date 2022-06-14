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

import ch.qos.logback.core.util.FileSize;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.common.commander.AbstractProjectCommander;
import io.jpom.cron.CronUtils;
import io.jpom.model.data.NodeProjectInfoModel;
import io.jpom.service.manage.ProjectInfoService;
import io.jpom.system.AgentExtConfigBean;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * 自动备份控制台日志，防止日志文件过大
 *
 * @author jiangzeyin
 * @since 2019/3/17
 */
@PreLoadClass
@Slf4j
public class AutoBackLog {

	private static final String ID = "auto_back_log";
	private static ProjectInfoService projectInfoService;

	private static FileSize MAX_SIZE;

	@PreLoadMethod
	private static void startAutoBackLog() {
		if (projectInfoService == null) {
			projectInfoService = SpringUtil.getBean(ProjectInfoService.class);
		}
		// 获取cron 表达式
		String cron = StrUtil.emptyToDefault(AgentExtConfigBean.getInstance().autoBackConsoleCron, "none");
		if ("none".equalsIgnoreCase(cron.trim())) {
			//log.info("没有配置自动备份控制台日志表达式");
			//return;
			cron = "0 0/10 * * * ?";
		}
		String size = StrUtil.emptyToDefault(AgentExtConfigBean.getInstance().autoBackSize, "50MB");
		MAX_SIZE = FileSize.valueOf(size.trim());
		//
		CronUtils.upsert(ID, cron, () -> {
			try {
				List<NodeProjectInfoModel> list = projectInfoService.list();
				if (list == null) {
					return;
				}
				list.forEach(projectInfoModel -> {
					checkProject(projectInfoModel, null);
					//
					List<NodeProjectInfoModel.JavaCopyItem> javaCopyItemList = projectInfoModel.getJavaCopyItemList();
					if (javaCopyItemList == null) {
						return;
					}
					javaCopyItemList.forEach(javaCopyItem -> checkProject(projectInfoModel, javaCopyItem));
				});
			} catch (Exception e) {
				log.error("定时备份日志失败", e);
			}
		});
	}

	private static void checkProject(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel.JavaCopyItem javaCopyItem) {
		File file = javaCopyItem == null ? new File(nodeProjectInfoModel.getLog()) : nodeProjectInfoModel.getLog(javaCopyItem);
		if (!file.exists()) {
			return;
		}
		long len = file.length();
		if (len > MAX_SIZE.getSize()) {
			try {
				AbstractProjectCommander.getInstance().backLog(nodeProjectInfoModel, javaCopyItem);
			} catch (Exception ignored) {
			}
		}
		// 清理过期的文件
		File logFile = javaCopyItem == null ? nodeProjectInfoModel.getLogBack() : nodeProjectInfoModel.getLogBack(javaCopyItem);
		DateTime nowTime = DateTime.now();
		List<File> files = FileUtil.loopFiles(logFile, pathname -> {
			DateTime dateTime = DateUtil.date(pathname.lastModified());
			long days = DateUtil.betweenDay(dateTime, nowTime, false);
			long saveDays = AgentExtConfigBean.getInstance().getLogSaveDays();
			return days > saveDays;
		});
		files.forEach(FileUtil::del);
	}
}
