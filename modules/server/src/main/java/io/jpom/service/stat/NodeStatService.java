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
package io.jpom.service.stat;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.jiangzeyin.common.JsonMessage;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.BaseServerController;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.cron.IAsyncLoad;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.model.stat.NodeStatModel;
import io.jpom.service.dblog.DbSystemMonitorLogService;
import io.jpom.service.h2db.BaseWorkspaceService;
import io.jpom.service.node.NodeService;
import io.jpom.system.AgentException;
import io.jpom.system.AuthorizeException;
import io.jpom.system.ServerExtConfigBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 节点统计 service
 *
 * @author bwcx_jzy
 * @since 2022/1/22
 */
@Service
@Slf4j
public class NodeStatService extends BaseWorkspaceService<NodeStatModel> implements IAsyncLoad, Runnable {

    private final ServerExtConfigBean serverExtConfigBean;
    private final DbSystemMonitorLogService dbSystemMonitorLogService;
    private final NodeService nodeService;

    public NodeStatService(ServerExtConfigBean serverExtConfigBean,
                           DbSystemMonitorLogService dbSystemMonitorLogService,
                           NodeService nodeService) {
        this.serverExtConfigBean = serverExtConfigBean;
        this.dbSystemMonitorLogService = dbSystemMonitorLogService;
        this.nodeService = nodeService;
    }

    /**
     * 根据 url 去重
     *
     * @return list
     */
    public List<String> getDeDuplicationByUrl() {
        String sql = "select url from " + super.getTableName() + "  group  by url";
        List<Entity> query = this.query(sql);
        if (query != null) {
            return query.stream().map((entity -> entity.getStr("url"))).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public void startLoad() {
        // 启动心跳检测
        int heartSecond = serverExtConfigBean.getNodeHeartSecond();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "Jpom Node Monitor"));
        scheduler.scheduleAtFixedRate(this, 10, heartSecond, TimeUnit.SECONDS);
        //  清理 错误的节点统计数据
        List<String> duplicationByUrl = nodeService.getDeDuplicationByUrl();
        List<String> statUrl = this.getDeDuplicationByUrl();
        Collection<String> subtract = CollUtil.subtract(statUrl, duplicationByUrl);
        //
        for (String s : subtract) {
            NodeStatModel statModel = new NodeStatModel();
            statModel.setUrl(s);
            this.del(this.dataBeanToEntity(statModel));
        }
    }

    @Override
    public void run() {
        List<NodeModel> nodeModels = nodeService.listDeDuplicationByUrl();
        //
        this.checkList(nodeModels);
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
                try {
                    BaseServerController.resetInfo(UserModel.EMPTY);
                    this.save(modelList, 4, "节点禁用中");
                } finally {
                    BaseServerController.removeEmpty();
                }
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
                } catch (AgentException e) {
                    this.save(modelList, 1, e.getMessage());
                } catch (Exception e) {
                    this.save(modelList, 1, e.getMessage());
                    log.error("获取节点监控信息失败", e);
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
            this.upsert(nodeStatModel);
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
                String runTime = statusData.getString("runTime");
                String runTimeLong = statusData.getString("runTimeLong");
                // 兼容数据
                nodeStatModel.setUpTimeStr(StrUtil.emptyToDefault(runTimeLong, runTime));
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
            this.upsert(nodeStatModel);
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
        nodeStatModel.setGroup(model.getGroup());
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
