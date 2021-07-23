package io.jpom.system.init;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import cn.jiangzeyin.common.spring.SpringUtil;
import io.jpom.build.BuildUtil;
import io.jpom.service.monitor.MonitorService;
import io.jpom.service.node.NodeService;
import io.jpom.util.CronUtils;

import java.io.File;

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
		//
		CronUtil.schedule("cache_manger_schedule", "0 0/10 * * * ?", BuildUtil::reloadCacheSize);
		ThreadUtil.execute(BuildUtil::reloadCacheSize);
		CronUtils.start();
	}
}
