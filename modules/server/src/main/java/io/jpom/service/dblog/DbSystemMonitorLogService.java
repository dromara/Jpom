package io.jpom.service.dblog;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.PageResult;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.UserModel;
import io.jpom.model.log.SystemMonitorLog;
import io.jpom.service.node.NodeService;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DbSystemMonitorLogService extends BaseDbLogService<SystemMonitorLog> {
    @Resource
    private NodeService nodeService;

    public DbSystemMonitorLogService() {
        super(SystemMonitorLog.TABLE_NAME, SystemMonitorLog.class);
        setKey("id");
    }

    /**
     * 初始化系统监控sockte 链接
     *
     * @param userInfo
     */
    public void init(UserModel userInfo) {
        String userName = UserModel.getOptUserName(userInfo);
        userName = URLUtil.encode(userName);
        List<NodeModel> list = nodeService.list();
        for (NodeModel nodeModel : list) {
            try {
                String url = nodeModel.getUrl();
                String id = nodeModel.getId();
                String socketUrl = StrUtil.format("ws://{}/console/system/{}?nodeId={}", url, userName, id);
                WebSocketClient webSocketClient = new WebSocketClient(new URI(socketUrl)) {
                    @Override
                    public void onOpen(ServerHandshake handshakedata) {

                    }

                    @Override
                    public void onMessage(String message) {
                        try {
                            JSONObject object = JSONObject.parseObject(message);
                            String url = this.getURI().toString();
                            HashMap<String, String> map = HttpUtil.decodeParamMap(url, CharsetUtil.UTF_8);
                            String nodeId = map.get("nodeId");
                            boolean top = object.getBooleanValue("top");
                            if (!top) {
                                return;
                            }
                            SystemMonitorLog systemMonitorLog = new SystemMonitorLog();
                            systemMonitorLog.setNodeId(nodeId);
                            long id = object.getLongValue("id");
                            systemMonitorLog.setId(id);
                            String time = object.getString("time");
                            systemMonitorLog.setMonitorTime(time);
                            systemMonitorLog.setOccupyCpu(object.getDoubleValue("cpu"));
                            systemMonitorLog.setOccupyDisk(object.getDoubleValue("disk"));
                            systemMonitorLog.setOccupyMemory(object.getDoubleValue("memory"));
                            insert(systemMonitorLog);
                        } catch (Exception e) {
                            DefaultSystemLog.ERROR().error("socket添加系统监控记录", e);
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {

                    }

                    @Override
                    public void onError(Exception ex) {

                    }
                };
                webSocketClient.connectBlocking();
                webSocketClient.send("{\"op\": \"top\", \"projectInfo\":{}}");
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("连接系统监控socket异常", e);
            }
        }
    }

    public JSONObject getTopMonitor(String type) {
        PageResult<SystemMonitorLog> systemMonitorLogs = getMonitorData(type);
        JSONArray scales = new JSONArray();
        JSONArray series = new JSONArray();
        JSONObject object = new JSONObject();
        if (systemMonitorLogs.getTotal() <= 0) {
//            int minSize = 30;
//            if ("day".equals(type)) {
//                minSize = 24;
//            }
//            DateTime date = DateUtil.date();
//            for (int i = 0; i < minSize; i++) {
//                String time = DateUtil.formatTime(date);
//                scales.add(time);
//                if ("day".equals(type)) {
//                    date = DateUtil.offset(date, DateField.HOUR, 1);
//                } else {
//                    date = DateUtil.offset(date, DateField.MINUTE, 1);
//                }
//            }
//            JSONObject item = new JSONObject();
//            item.put("cpu", 0);
//            item.put("disk", 0);
//            item.put("memory", 0);
//            series.add(item);
            return null;
        } else {
            for (SystemMonitorLog systemMonitorLog : systemMonitorLogs) {
                scales.add(systemMonitorLog.getMonitorTime());
                JSONObject item = new JSONObject();
                item.put("cpu", systemMonitorLog.getOccupyCpu());
                item.put("disk", systemMonitorLog.getOccupyDisk());
                item.put("memory", systemMonitorLog.getOccupyMemory());
                series.add(item);
            }
        }
        object.put("scales", scales);
        object.put("series", series);
        return object;
    }

    public PageResult<SystemMonitorLog> getMonitorData(String type) {
        long startTime;
        long endTime = DateUtil.date().getTime();
        if ("day".equals(type)) {
            startTime = endTime - TimeUnit.HOURS.toMillis(24);
        } else {
            startTime = endTime - TimeUnit.MINUTES.toMillis(30);
        }
        Entity entity = new Entity(SystemMonitorLog.TABLE_NAME);
        entity.set(" id", ">= " + startTime);
        entity.set("id", "<= " + endTime);
        return listPage(entity, null);
    }
}
