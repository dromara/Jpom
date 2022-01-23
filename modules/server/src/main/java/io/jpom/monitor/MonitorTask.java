///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 Code Technology Studio
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
//package io.jpom.monitor;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.thread.ThreadUtil;
//import cn.hutool.cron.CronUtil;
//import cn.hutool.cron.task.Task;
//import cn.jiangzeyin.common.spring.SpringUtil;
//import io.jpom.model.Cycle;
//import io.jpom.model.data.MonitorModel;
//import io.jpom.service.monitor.MonitorService;
//import io.jpom.cron.CronUtils;
//
//import java.util.List;
//
///**
// * 监听调度
// *
// * @author bwcx_jzy
// * @date 2019/7/12
// **/
//public class MonitorTask implements Task {
//
//	private static final String CRON_ID = "Monitor";
//
//
//	/**
//	 * 开启调度
//	 */
//	public static void start() {
//		CronUtils.add(CRON_ID, Cycle.one.getCronPattern().toString(), MonitorTask::new);
//	}
//
//	public static void stop() {
//		CronUtil.remove(CRON_ID);
//	}
//
//	@Override
//	public void execute() {
//		long time = System.currentTimeMillis();
//		MonitorService monitorService = SpringUtil.getBean(MonitorService.class);
//		//
//		List<MonitorModel> monitorModels = monitorService.listRunByCycle(Cycle.one);
//		//
//		if (Cycle.five.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
//			monitorModels.addAll(monitorService.listRunByCycle(Cycle.five));
//		}
//		//
//		if (Cycle.ten.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
//			monitorModels.addAll(monitorService.listRunByCycle(Cycle.ten));
//		}
//		//
//		if (Cycle.thirty.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
//			monitorModels.addAll(monitorService.listRunByCycle(Cycle.thirty));
//		}
//		//
//		this.checkList(monitorModels);
//	}
//
//	private void checkList(List<MonitorModel> monitorModels) {
//		if (monitorModels == null || monitorModels.isEmpty()) {
//			return;
//		}
//		monitorModels.forEach(monitorModel -> {
//			List<MonitorModel.NodeProject> nodeProjects = monitorModel.projects();
//			List<String> notifyUser = monitorModel.notifyUser();
//			if (CollUtil.isEmpty(nodeProjects) || CollUtil.isEmpty(notifyUser)) {
//				return;
//			}
//			//
//			ThreadUtil.execute(new MonitorItem(monitorModel));
//		});
//	}
//
//}
