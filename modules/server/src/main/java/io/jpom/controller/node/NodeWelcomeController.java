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
package io.jpom.controller.node;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.JsonMessage;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.permission.SystemPermission;
import io.jpom.service.dblog.DbSystemMonitorLogService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 欢迎页
 *
 * @author Administrator
 */
@RestController
@RequestMapping(value = "/node")
public class NodeWelcomeController extends BaseServerController {

	private final DbSystemMonitorLogService dbSystemMonitorLogService;

	public NodeWelcomeController(DbSystemMonitorLogService dbSystemMonitorLogService) {
		this.dbSystemMonitorLogService = dbSystemMonitorLogService;
	}

	@PostMapping(value = "node_monitor_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public String nodeMonitorJson() {
		List<SystemMonitorLog> list = this.getList();
		Assert.notEmpty(list, "没有查询到任何数据");
		return JsonMessage.getString(200, "ok", list);
	}

	private List<SystemMonitorLog> getList() {
		NodeModel node = getNode();
		String startDateStr = getParameter("time[0]");
		String endDateStr = getParameter("time[1]");
		if (StrUtil.hasEmpty(startDateStr, endDateStr)) {
			SystemMonitorLog systemMonitorLog = new SystemMonitorLog();
			systemMonitorLog.setNodeId(node.getId());
			return dbSystemMonitorLogService.queryList(systemMonitorLog, 500, new Order("monitorTime", Direction.DESC));
		}
		//  处理时间
		DateTime startDate = DateUtil.parse(startDateStr);
		long startTime = startDate.getTime();
		DateTime endDate = DateUtil.parse(endDateStr);
		if (startDate.equals(endDate)) {
			// 时间相等
			endDate = DateUtil.endOfDay(endDate);
		}
		long endTime = endDate.getTime();

		// 开启了节点信息采集
		Page pageObj = new Page(1, 5000);
		pageObj.addOrder(new Order("monitorTime", Direction.DESC));
		Entity entity = Entity.create();
		entity.set("nodeId", node.getId());
		entity.set(" MONITORTIME", ">= " + startTime);
		entity.set("MONITORTIME", "<= " + endTime);
		return dbSystemMonitorLogService.listPageOnlyResult(entity, pageObj);
	}

//	private JSONObject getData() {
//		List<SystemMonitorLog> list = getList();
//		Assert.notEmpty(list, "没有查询到任何数据");
//		List<JSONObject> series = new ArrayList<>();
//		List<String> scale = new ArrayList<>();
//		for (int i = list.size() - 1; i >= 0; i--) {
//			SystemMonitorLog systemMonitorLog = list.get(i);
//			scale.add(new DateTime(systemMonitorLog.getMonitorTime()).toString(DatePattern.NORM_DATETIME_PATTERN));
//			JSONObject jsonObject = new JSONObject();
//			jsonObject.put("cpu", systemMonitorLog.getOccupyCpu());
//			jsonObject.put("memory", systemMonitorLog.getOccupyMemory());
//			jsonObject.put("memoryUsed", systemMonitorLog.getOccupyMemoryUsed());
//			jsonObject.put("disk", systemMonitorLog.getOccupyDisk());
//			series.add(jsonObject);
//		}
//
//		JSONObject object = new JSONObject();
//		object.put("scales", scale);
//		object.put("series", series);
//		return object;
//	}
//
//	@PostMapping(value = "getTop", produces = MediaType.APPLICATION_JSON_VALUE)
//	public String getTop() {
//		JSONObject object = getData();
//		return JsonMessage.getString(200, "ok", object);
//	}

//	@RequestMapping(value = "exportTop")
//	public void exportTop(String time) throws UnsupportedEncodingException {
//		List<SystemMonitorLog> result = getList();
//		if (CollUtil.isEmpty(result)) {
//			//            NodeForward.requestDownload(node, getRequest(), getResponse(), NodeUrl.exportTop);
//		} else {
//			NodeModel node = getNode();
//			StringBuilder buf = new StringBuilder();
//			buf.append("监控时间").append(",占用cpu").append(",占用内存").append(",占用磁盘").append("\r\n");
//			for (SystemMonitorLog log : result) {
//				long monitorTime = log.getMonitorTime();
//				buf.append(DateUtil.date(monitorTime)).append(StrUtil.COMMA)
//						.append(log.getOccupyCpu()).append("%").append(StrUtil.COMMA)
//						.append(log.getOccupyMemory()).append("%").append(StrUtil.COMMA)
//						.append(log.getOccupyDisk()).append("%").append("\r\n");
//			}
//			String fileName = URLEncoder.encode("Jpom系统监控-" + node.getId(), "UTF-8");
//			HttpServletResponse response = getResponse();
//			response.setHeader("Content-Disposition", "attachment;filename=" + new String(fileName.getBytes(StandardCharsets.UTF_8), "GBK") + ".csv");
//			response.setContentType("text/csv;charset=utf-8");
//			ServletUtil.write(getResponse(), buf.toString(), CharsetUtil.UTF_8);
//		}
//	}

	@RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getProcessList() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.ProcessList).toString();
	}

	@RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@SystemPermission
	public String kill() {
		return NodeForward.request(getNode(), getRequest(), NodeUrl.Kill).toString();
	}
}
