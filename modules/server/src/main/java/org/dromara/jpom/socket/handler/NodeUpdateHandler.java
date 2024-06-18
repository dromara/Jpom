/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.socket.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.map.SafeConcurrentHashMap;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.Type;
import cn.keepbx.jpom.model.JsonMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.common.i18n.I18nThreadUtil;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.model.UploadFileModel;
import org.dromara.jpom.model.WebSocketMessageModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.dromara.jpom.socket.BaseProxyHandler;
import org.dromara.jpom.socket.ConsoleCommandOp;
import org.dromara.jpom.transport.*;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 节点管理控制器
 *
 * @author lf
 */
@SystemPermission(superUser = true)
@Feature(cls = ClassFeature.UPGRADE_NODE_LIST, method = MethodFeature.EXECUTE)
@Slf4j
public class NodeUpdateHandler extends BaseProxyHandler {

    private final ConcurrentMap<String, IProxyWebSocket> clientMap = new SafeConcurrentHashMap<>();

    private static final int CHECK_COUNT = 120;

    /**
     * 初始等待时间
     */
    private static final int INIT_WAIT = 10 * 1000;

    private final SystemParametersServer systemParametersServer;
    private final MachineNodeServer machineNodeServer;
    private final NodeConfig nodeConfig;

    public NodeUpdateHandler(MachineNodeServer machineNodeServer,
                             SystemParametersServer systemParametersServer,
                             NodeConfig nodeConfig) {
        super(null);
        this.machineNodeServer = machineNodeServer;
        this.systemParametersServer = systemParametersServer;
        this.nodeConfig = nodeConfig;
        //systemParametersServer = SpringUtil.getBean(SystemParametersServer.class);
//        nodeService = SpringUtil.getBean(NodeService.class);
    }

    @Override
    protected void init(WebSocketSession session, Map<String, Object> attributes) throws Exception {
        super.init(session, attributes);

    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{};
    }

    @Override
    protected void showHelloMsg(Map<String, Object> attributes, WebSocketSession session) {

    }

    private void pullNodeList(WebSocketSession session, String ids) {
        List<String> split = StrUtil.split(ids, StrUtil.COMMA);
        List<MachineNodeModel> nodeModelList = machineNodeServer.listById(split);
        if (nodeModelList == null) {
            this.onError(session, I18nMessageUtil.get("i18n.node_info_not_found.2c8c") + ids);
            return;
        }
        for (MachineNodeModel model : nodeModelList) {
            IProxyWebSocket nodeClient = clientMap.computeIfAbsent(model.getId(), s -> {
                INodeInfo nodeInfo = NodeForward.coverNodeInfo(model);
                IUrlItem urlItem = NodeForward.parseUrlItem(model, StrUtil.EMPTY, NodeUrl.NodeUpdate, DataContentType.FORM_URLENCODED);

                IProxyWebSocket proxySession = TransportServerFactory.get().websocket(nodeInfo, urlItem);
                proxySession.onMessage(msg -> sendMsg(session, msg));
                return proxySession;
            });
            // 连接节点
            I18nThreadUtil.execute(() -> {
                try {
                    if (!nodeClient.isConnected()) {
                        nodeClient.reconnectBlocking();
                    }
                    WebSocketMessageModel command = new WebSocketMessageModel("getVersion", model.getId());
                    nodeClient.send(command.toString());
                } catch (Exception e) {
                    String closeStatusMsg = nodeClient.getCloseStatusMsg();
                    log.error(I18nMessageUtil.get("i18n.create_plugin_endpoint_connection_failure.30f8"), closeStatusMsg, e);
                    IProxyWebSocket webSocket = clientMap.remove(model.getId());
                    IoUtil.close(webSocket);
                    this.onError(session, StrUtil.format(I18nMessageUtil.get("i18n.connect_plugin_failed.e492"), closeStatusMsg, e.getMessage(), model.getId()));
                }
            });
        }
    }

    @Override
    public void destroy(WebSocketSession session) {
        clientMap.values().forEach(iProxyWebSocket -> {
            if (iProxyWebSocket.isConnected()) {
                try {
                    iProxyWebSocket.close();
                } catch (Exception e) {
                    log.error(I18nMessageUtil.get("i18n.close_connection_exception.c855"), e);
                }
            }
        });
        clientMap.clear();
        //
        super.destroy(session);
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
        WebSocketMessageModel model = WebSocketMessageModel.getInstance(json.toString());
        String command = model.getCommand();
        switch (command) {
            case "getAgentVersion":
                model.setData(getAgentVersion());
                break;
            case "updateNode":
                super.logOpt(this.getClass(), attributes, json);
                updateNode(model, session);
                break;
            case "heart":
                for (Map.Entry<String, IProxyWebSocket> entry : clientMap.entrySet()) {
                    String key = entry.getKey();
                    IProxyWebSocket iProxyWebSocket = entry.getValue();
                    try {
                        iProxyWebSocket.send(model.toString());
                    } catch (Exception e) {
                        log.error(I18nMessageUtil.get("i18n.heartbeat_message_forwarding_failed.89cc"), key, e.getMessage());
                    }
                }
                break;
            default: {
                if (StrUtil.startWith(command, "getNodeList:")) {
                    String ids = StrUtil.removePrefix(command, "getNodeList:");
                    if (StrUtil.isNotEmpty(ids)) {
                        pullNodeList(session, ids);
                    }
                }
            }
            break;
        }
        if (model.getData() != null) {
            return model.toString();
        }
        return null;
    }

    private void onError(WebSocketSession session, String msg) {
        this.onError(session, msg, StrUtil.EMPTY);
    }

    private void onError(WebSocketSession session, String msg, String nodeId) {
        WebSocketMessageModel error = new WebSocketMessageModel("onError", nodeId);
        error.setData(msg);
        this.sendMsg(error, session);
    }

    /**
     * 更新节点
     *
     * @param model 参数
     */
    private void updateNode(WebSocketMessageModel model, WebSocketSession session) {
        JSONObject params = (JSONObject) model.getParams();
        JSONArray ids = params.getJSONArray("ids");
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        String protocol = params.getString("protocol");
        boolean http = StrUtil.equalsIgnoreCase(protocol, "http");
        try {
            AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class);
            //
            if (agentFileModel == null || !FileUtil.exist(agentFileModel.getSavePath())) {
                this.onError(session, I18nMessageUtil.get("i18n.agent_jar_not_exist.28ac"));
                return;
            }
            JsonMessage<Tuple> error = JpomManifest.checkJpomJar(agentFileModel.getSavePath(), Type.Agent, false);
            if (!error.success()) {
                this.onError(session, I18nMessageUtil.get("i18n.agent_jar_damaged.74a8") + error.getMsg());
                return;
            }
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.getString(i);
                MachineNodeModel node = machineNodeServer.getByKey(id);
                if (node == null) {
                    this.onError(session, I18nMessageUtil.get("i18n.no_node_specified.fa3d") + id);
                    continue;
                }
                I18nThreadUtil.execute(() -> this.updateNodeItem(id, node, session, agentFileModel, http));
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.upgrade_failure.4ae2"), e);
            this.onError(session, I18nMessageUtil.get("i18n.upgrade_failure_with_colon.59f1") + e.getMessage());
        }
    }

    private boolean updateNodeItemHttp(MachineNodeModel machineNodeModel, WebSocketSession session, AgentFileModel agentFileModel) throws IOException {
        File file = FileUtil.file(agentFileModel.getSavePath());

        JsonMessage<String> message = NodeForward.requestSharding(machineNodeModel, NodeUrl.SystemUploadJar, new JSONObject(),
            file,
            jsonObject -> NodeForward.request(machineNodeModel, NodeUrl.SystemUploadJarMerge, jsonObject),
            (total, progressSize) -> {
                UploadFileModel uploadFileModel = new UploadFileModel();
                uploadFileModel.setSize(total);
                uploadFileModel.setCompleteSize(progressSize);
                uploadFileModel.setId(machineNodeModel.getId());
                uploadFileModel.setVersion(agentFileModel.getVersion());
                // 更新进度
                WebSocketMessageModel model = new WebSocketMessageModel("updateNode", machineNodeModel.getId());
                model.setData(uploadFileModel);
                NodeUpdateHandler.this.sendMsg(model, session);
            });
        String id = machineNodeModel.getId();
        WebSocketMessageModel callbackRestartMessage = new WebSocketMessageModel("restart", id);
        callbackRestartMessage.setData(message.getMsg());
        this.sendMsg(callbackRestartMessage, session);
        if (!message.success()) {
            return false;
        }
        // 先等待一会，太快可能还没重启
        ThreadUtil.sleep(INIT_WAIT);
        int retryCount = 0;
        try {
            while (retryCount <= CHECK_COUNT) {
                ++retryCount;
                try {
                    ThreadUtil.sleep(1000L);
                    JsonMessage<Object> jsonMessage = NodeForward.request(machineNodeModel, StrUtil.EMPTY, NodeUrl.Info, "nodeId", id);
                    if (jsonMessage.success()) {
                        this.sendMsg(callbackRestartMessage.setData(I18nMessageUtil.get("i18n.restart_completed.42b8")), session);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug(I18nMessageUtil.get("i18n.node_connection_failed.8497"), id, e.getMessage());
                }
            }
            this.sendMsg(callbackRestartMessage.setData(I18nMessageUtil.get("i18n.reconnect_failure.7c01")), session);
        } catch (Exception e) {
            log.error("{}{}", I18nMessageUtil.get("i18n.reconnect_plugin_failure_after_upgrade.73e3"), id, e);
            this.sendMsg(callbackRestartMessage.setData(I18nMessageUtil.get("i18n.reconnect_plugin_failure.cc6c")), session);
        }
        return false;
    }

    private boolean updateNodeItemWebSocket(IProxyWebSocket client, String id, WebSocketSession session, AgentFileModel agentFileModel) throws IOException {
        // 发送文件信息
        WebSocketMessageModel webSocketMessageModel = new WebSocketMessageModel("upload", id);
        webSocketMessageModel.setNodeId(id);
        webSocketMessageModel.setParams(agentFileModel);
        client.send(webSocketMessageModel.toString());
        //
        try (FileInputStream fis = new FileInputStream(agentFileModel.getSavePath())) {
            // 发送文件内容
            int len;
            int fileSliceSize = nodeConfig.getUploadFileSliceSize();
            byte[] buffer = new byte[(int) DataSize.ofMegabytes(fileSliceSize).toBytes()];
            while ((len = fis.read(buffer)) > 0) {
                client.send(ByteBuffer.wrap(buffer, 0, len));
            }
        }
        WebSocketMessageModel restartMessage = new WebSocketMessageModel("restart", id);
        client.send(restartMessage.toString());
        // 先等待一会，太快可能还没重启
        ThreadUtil.sleep(INIT_WAIT);
        // 重启后尝试访问插件端，能够连接说明重启完毕
        //WebSocketMessageModel callbackRestartMessage = new WebSocketMessageModel("restart", id);
        int retryCount = 0;
        try {
            while (retryCount <= CHECK_COUNT) {
                try {
                    if (client.reconnect()) {
                        this.sendMsg(restartMessage.setData(I18nMessageUtil.get("i18n.restart_completed.42b8")), session);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug(I18nMessageUtil.get("i18n.node_connection_failed.8497"), id, e.getMessage());
                }
                ThreadUtil.sleep(1000L);
                ++retryCount;
            }
            this.sendMsg(restartMessage.setData(I18nMessageUtil.get("i18n.reconnect_failure.7c01")), session);
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.reconnect_plugin_failure_after_upgrade.73e3") + id, e);
            this.sendMsg(restartMessage.setData(I18nMessageUtil.get("i18n.reconnect_plugin_failure.cc6c")), session);
        }
        return false;
    }

    private void updateNodeItem(String id, MachineNodeModel node, WebSocketSession session, AgentFileModel agentFileModel, boolean http) {
        try {
            IProxyWebSocket client = clientMap.get(node.getId());
            if (client == null) {
                this.onError(session, I18nMessageUtil.get("i18n.plugin_not_initialized.3ea9"), id);
                return;
            }
            if (client.isConnected()) {
                boolean result = http ? this.updateNodeItemHttp(node, session, agentFileModel) : this.updateNodeItemWebSocket(client, id, session, agentFileModel);
                if (result) {
                    //
                    WebSocketMessageModel command = new WebSocketMessageModel("getVersion", node.getId());
                    client.send(command.toString());
                }
            } else {
                this.onError(session, I18nMessageUtil.get("i18n.node_connection_lost.b6c7"), id);
            }
        } catch (Exception e) {
            log.error(I18nMessageUtil.get("i18n.upgrade_failure_with_colon.59f1") + id, e);
            this.onError(session, I18nMessageUtil.get("i18n.node_upgrade_failed.4493") + e.getMessage(), id);
        }
    }

    private void sendMsg(WebSocketMessageModel model, WebSocketSession session) {
        super.sendMsg(session, model.toString());
    }

    /**
     * 获取当前系统缓存的Agent
     *
     * @return json
     */
    private String getAgentVersion() {
        AgentFileModel agentFileModel = systemParametersServer.getConfig(AgentFileModel.ID, AgentFileModel.class, agentFileModel1 -> {
            if (agentFileModel1 == null || !FileUtil.exist(agentFileModel1.getSavePath())) {
                return AgentFileModel.EMPTY;
            }
            return agentFileModel1;
        });
        return agentFileModel == null ? null : JSONObject.toJSONString(agentFileModel);
    }
}
