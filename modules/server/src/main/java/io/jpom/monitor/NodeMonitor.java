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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.model.stat.NodeStatModel;
import io.jpom.service.dblog.DbSystemMonitorLogService;
import io.jpom.service.node.NodeService;
import io.jpom.service.stat.NodeStatService;
import io.jpom.system.AuthorizeException;
import io.jpom.system.ServerExtConfigBean;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 节点监控
 *
 * @author bwcx_jzy
 * @date 2019/9/17
 */
public class NodeMonitor {

	private static DbSystemMonitorLogService dbSystemMonitorLogService;
	private static NodeStatService nodeStatService;
	private static NodeService nodeService;

	/**
	 * 开启调度
	 */
	public static void start() {
		final NodeMonitor monitor = new NodeMonitor();
		int heartSecond = ServerExtConfigBean.getInstance().getNodeHeartSecond();
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "Jpom Node Monitor"));
		scheduler.scheduleAtFixedRate(monitor::execute, 10, heartSecond, TimeUnit.SECONDS);
	}

	private void init() {
		if (dbSystemMonitorLogService == null) {
			dbSystemMonitorLogService = SpringUtil.getBean(DbSystemMonitorLogService.class);
		}
		if (nodeStatService == null) {
			nodeStatService = SpringUtil.getBean(NodeStatService.class);
		}
		if (nodeService == null) {
			nodeService = SpringUtil.getBean(NodeService.class);
		}
	}


	private void execute() {
		try {
			this.init();
			NodeService nodeService = SpringUtil.getBean(NodeService.class);
			List<NodeModel> nodeModels = nodeService.listDeDuplicationByUrl();
			//
			this.checkList(nodeModels);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("节点心跳检测异常", e);
		}
	}

	private List<NodeModel> getListByUrl(String url) {
		NodeModel nodeModel = new NodeModel();
		nodeModel.setUrl(url);
		return nodeService.listByBean(nodeModel);
	}

	private void checkList(List<NodeModel> nodeModels) {
		if (CollUtil.isEmpty(nodeModels)) {
			return;
		}
		nodeModels.forEach(nodeModel -> {
			//
			nodeModel.setName(nodeModel.getUrl());
			List<NodeModel> modelList = this.getListByUrl(nodeModel.getUrl());
			boolean match = modelList.stream().allMatch(NodeModel::isOpenStatus);
			if (!match) {
				// 节点都关闭
				this.save(modelList, 4, "节点禁用中");
				return;
			}
			nodeModel.setOpenStatus(1);
			nodeModel.setTimeOut(5);
			//
			ThreadUtil.execute(() -> {
				try {
					BaseServerController.resetInfo(UserModel.EMPTY);
					JSONObject nodeTopInfo = this.getNodeTopInfo(nodeModel);
					//
					long timeMillis = SystemClock.now();
					JsonMessage<Object> jsonMessage = NodeForward.requestBySys(nodeModel, NodeUrl.Status, "nodeId", nodeModel.getId());
					int networkTime = (int) (System.currentTimeMillis() - timeMillis);
					JSONObject jsonObject;
					if (jsonMessage.getCode() == 200) {
						jsonObject = jsonMessage.getData(JSONObject.class);
					} else {
						// 状态码错
						jsonObject = new JSONObject();
						jsonObject.put("status", 3);
						jsonObject.put("failureMsg", jsonMessage.toString());
					}
					jsonObject.put("networkTime", networkTime);
					if (nodeTopInfo != null) {
						nodeTopInfo.put("networkTime", networkTime);
					}
					this.save(modelList, nodeTopInfo, jsonObject);
				} catch (AuthorizeException agentException) {
					this.save(modelList, 2, agentException.getMessage());
				} catch (Exception e) {
					this.save(modelList, 1, e.getMessage());
					DefaultSystemLog.getLog().error("获取节点监控信息失败", e);
				} finally {
					BaseServerController.removeEmpty();
				}
			});
		});
	}

	private void saveSystemMonitor(List<NodeModel> modelList, JSONObject systemMonitor) {
		if (systemMonitor != null) {
			List<SystemMonitorLog> monitorLogs = modelList.stream().map(nodeModel -> {
				SystemMonitorLog log = new SystemMonitorLog();
				log.setOccupyMemory(systemMonitor.getDouble("memory"));
				log.setOccupyMemoryUsed(systemMonitor.getDouble("memoryUsed"));
				log.setOccupyDisk(systemMonitor.getDouble("disk"));
				log.setOccupyCpu(systemMonitor.getDouble("cpu"));
				log.setMonitorTime(systemMonitor.getLongValue("time"));
				log.setNetworkTime(systemMonitor.getIntValue("networkTime"));
				log.setNodeId(nodeModel.getId());
				return log;
			}).collect(Collectors.toList());
			//
			dbSystemMonitorLogService.insert(monitorLogs);
		}
	}

	/**
	 * 更新状态 和错误信息
	 *
	 * @param modelList 节点
	 * @param satus     状态
	 * @param msg       错误消息
	 */
	private void save(List<NodeModel> modelList, int satus, String msg) {
		for (NodeModel nodeModel : modelList) {
			NodeStatModel nodeStatModel = this.create(nodeModel);
			nodeStatModel.setFailureMsg(msg);
			nodeStatModel.setStatus(satus);
			nodeStatService.upsert(nodeStatModel);
		}
	}

	/**
	 * 报错结果
	 *
	 * @param modelList     节点
	 * @param systemMonitor 系统监控
	 * @param statusData    状态数据
	 */
	private void save(List<NodeModel> modelList, JSONObject systemMonitor, JSONObject statusData) {
		this.saveSystemMonitor(modelList, systemMonitor);
		//
		for (NodeModel nodeModel : modelList) {
			NodeStatModel nodeStatModel = this.create(nodeModel);
			if (nodeModel.isOpenStatus()) {
				if (systemMonitor != null) {
					nodeStatModel.setOccupyMemory(ObjectUtil.defaultIfNull(systemMonitor.getDouble("memory"), -1D));
					nodeStatModel.setOccupyMemoryUsed(ObjectUtil.defaultIfNull(systemMonitor.getDouble("memoryUsed"), -1D));
					nodeStatModel.setOccupyDisk(ObjectUtil.defaultIfNull(systemMonitor.getDouble("disk"), -1D));
					nodeStatModel.setOccupyCpu(ObjectUtil.defaultIfNull(systemMonitor.getDouble("cpu"), -1D));
				}
				//
				nodeStatModel.setNetworkTime(statusData.getIntValue("networkTime"));
				nodeStatModel.setJpomVersion(statusData.getString("jpomVersion"));
				nodeStatModel.setOsName(statusData.getString("osName"));
				nodeStatModel.setUpTimeStr(statusData.getString("runTime"));
				nodeStatModel.setFailureMsg(StrUtil.emptyToDefault(statusData.getString("failureMsg"), StrUtil.EMPTY));
				//
				Integer statusInteger = statusData.getInteger("status");
				if (statusInteger != null) {
					nodeStatModel.setStatus(statusInteger);
				} else {
					nodeStatModel.setStatus(0);
				}
			} else {
				nodeStatModel.setStatus(4);
				nodeStatModel.setFailureMsg("节点禁用中");
			}
			nodeStatService.upsert(nodeStatModel);
		}
	}

	private NodeStatModel create(NodeModel model) {
		NodeStatModel nodeStatModel = new NodeStatModel();
		nodeStatModel.setId(model.getId());
		nodeStatModel.setWorkspaceId(model.getWorkspaceId());
		nodeStatModel.setName(model.getName());
		nodeStatModel.setUrl(model.getUrl());
		//
		nodeStatModel.setOccupyMemory(-1D);
		nodeStatModel.setOccupyMemoryUsed(-1D);
		nodeStatModel.setOccupyDisk(-1D);
		nodeStatModel.setOccupyCpu(-1D);
		nodeStatModel.setNetworkTime(-1);
		nodeStatModel.setUpTimeStr(StrUtil.EMPTY);
		return nodeStatModel;
	}

	/**
	 * 获取节点监控信息
	 *
	 * @param reqNode 真实节点
	 */
	private JSONObject getNodeTopInfo(NodeModel reqNode) {
		JsonMessage<JSONObject> message = NodeForward.request(reqNode, null, NodeUrl.GetDirectTop);
		return message.getData();
	}
}
