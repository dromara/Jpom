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
package io.jpom.monitor;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.pattern.CronPattern;
import cn.hutool.cron.task.Task;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.Cycle;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.dblog.DbSystemMonitorLogService;
import io.jpom.service.node.NodeService;
import io.jpom.cron.CronUtils;

import java.util.List;

/**
 * 节点监控
 *
 * @author bwcx_jzy
 * @date 2019/9/17
 */
public class NodeMonitor implements Task {

	private static final String CRON_ID = "NodeMonitor";

	private static DbSystemMonitorLogService dbSystemMonitorLogService;

	/**
	 * 开启调度
	 */
	public static void start() {
		Task task = CronUtil.getScheduler().getTask(CRON_ID);
		if (task == null) {
			CronPattern cronPattern = Cycle.seconds30.getCronPattern();
			CronUtils.upsert(CRON_ID, cronPattern.toString(), new NodeMonitor());
		}
		dbSystemMonitorLogService = SpringUtil.getBean(DbSystemMonitorLogService.class);
	}

	public static void stop() {
		CronUtil.remove(CRON_ID);
	}

	@Override
	public void execute() {
		long time = System.currentTimeMillis();
		NodeService nodeService = SpringUtil.getBean(NodeService.class);
		//
		List<NodeModel> nodeModels = nodeService.listByCycle(Cycle.seconds30);
		//
		if (Cycle.one.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
			nodeModels.addAll(nodeService.listByCycle(Cycle.one));
		}
		//
		if (Cycle.five.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
			nodeModels.addAll(nodeService.listByCycle(Cycle.five));
		}
		//
		if (Cycle.ten.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
			nodeModels.addAll(nodeService.listByCycle(Cycle.ten));
		}
		//
		if (Cycle.thirty.getCronPattern().match(time, CronUtil.getScheduler().isMatchSecond())) {
			nodeModels.addAll(nodeService.listByCycle(Cycle.thirty));
		}
		//
		this.checkList(nodeModels);
	}

	private void checkList(List<NodeModel> nodeModels) {
		if (nodeModels == null || nodeModels.isEmpty()) {
			return;
		}
		nodeModels.forEach(nodeModel -> ThreadUtil.execute(() -> {
			try {
				getNodeInfo(nodeModel);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("获取节点监控信息失败：{}", e.getMessage());
			}
		}));
	}

	private void getNodeInfo(NodeModel nodeModel) {
		JsonMessage<JSONObject> message = NodeForward.request(nodeModel, null, NodeUrl.GetDirectTop);
		JSONObject jsonObject = message.getData();
		if (jsonObject == null) {
			return;
		}
		double disk = jsonObject.getDoubleValue("disk");
		if (disk <= 0) {
			return;
		}
		//
		SystemMonitorLog log = new SystemMonitorLog();
		log.setOccupyMemory(jsonObject.getDoubleValue("memory"));
		log.setOccupyMemoryUsed(jsonObject.getDoubleValue("memoryUsed"));
		log.setOccupyDisk(disk);
		log.setOccupyCpu(jsonObject.getDoubleValue("cpu"));
		log.setMonitorTime(jsonObject.getLongValue("time"));
		log.setNodeId(nodeModel.getId());
		dbSystemMonitorLogService.insert(log);
	}
}
