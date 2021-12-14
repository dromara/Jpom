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

import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.build.BuildUtil;
import io.jpom.common.RemoteVersion;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.NodeService;
import io.jpom.system.ConfigBean;
import io.jpom.util.CronUtils;

/**
 * @author bwcx_jzy
 * @date 2019/7/14
 */
@PreLoadClass
public class CheckMonitor {

	@PreLoadMethod
	private static void init() {
		MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
		boolean status = monitorService.checkCronStatus();
		if (status) {
			Console.log("已经开启监听调度：监控");
		}
		//
		NodeService nodeService = SpringUtil.getBean(NodeService.class);
		status = nodeService.checkCronStatus();
		if (status) {
			Console.log("已经开启监听调度：节点信息采集");
		}
		// 缓存检测调度
		CronUtils.upsert("cache_manger_schedule", "0 0/10 * * * ?", () -> {
			BuildUtil.reloadCacheSize();
			ConfigBean.getInstance().dataSize();
		});
		ThreadUtil.execute(() -> {
			BuildUtil.reloadCacheSize();
			ConfigBean.getInstance().dataSize();
			// 加载构建定时器
			BuildInfoService buildInfoService = SpringUtil.getBean(BuildInfoService.class);
			buildInfoService.startCron();
			//
			RemoteVersion.loadRemoteInfo();
		});
		// 开启版本检测调度
		CronUtils.upsert("system_monitor", "0 0 0,12 * * ?", () -> {
			try {
				BackupInfoService backupInfoService = SpringUtil.getBean(BackupInfoService.class);
				backupInfoService.checkAutoBackup();
				//
				RemoteVersion.loadRemoteInfo();
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("系统调度执行出现错误", e);
			}
		});
	}
}
