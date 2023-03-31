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
package org.dromara.jpom.socket.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.unit.DataSize;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.common.JsonMessage;
import org.dromara.jpom.common.Type;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.common.forward.NodeUrl;
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
import org.dromara.jpom.system.ServerConfig;
import org.dromara.jpom.transport.*;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ConcurrentMap<String, IProxyWebSocket> clientMap = new ConcurrentHashMap<>();

    private static final int CHECK_COUNT = 120;

    /**
     * 初始等待时间
     */
    private static final int INIT_WAIT = 10 * 1000;

    private final SystemParametersServer systemParametersServer;
    private final MachineNodeServer machineNodeServer;
    private final ServerConfig.NodeConfig nodeConfig;

    public NodeUpdateHandler(MachineNodeServer machineNodeServer,
                             SystemParametersServer systemParametersServer,
                             ServerConfig.NodeConfig nodeConfig) {
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
            this.onError(session, "没有查询到节点信息：" + ids);
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
            ThreadUtil.execute(() -> {
                try {
                    if (!nodeClient.isConnected()) {
                        nodeClient.reconnectBlocking();
                    }
                    WebSocketMessageModel command = new WebSocketMessageModel("getVersion", model.getId());
                    nodeClient.send(command.toString());
                } catch (Exception e) {
                    String closeStatusMsg = nodeClient.getCloseStatusMsg();
                    log.error("创建插件端连接失败 {}", closeStatusMsg, e);
                    IProxyWebSocket webSocket = clientMap.remove(model.getId());
                    IoUtil.close(webSocket);
                    this.onError(session, "连接插件端失败：" + closeStatusMsg + " " + e.getMessage(), model.getId());
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
                    log.error("关闭连接异常", e);
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
                        log.error("心跳消息转发失败 {} {}", key, e.getMessage());
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
                this.onError(session, "Agent JAR包不存在");
                return;
            }
            JsonMessage<Tuple> error = JpomManifest.checkJpomJar(agentFileModel.getSavePath(), Type.Agent, false);
            if (!error.success()) {
                this.onError(session, "Agent JAR 损坏请重新上传," + error.getMsg());
                return;
            }
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.getString(i);
                MachineNodeModel node = machineNodeServer.getByKey(id);
                if (node == null) {
                    this.onError(session, "没有对应的节点：" + id);
                    continue;
                }
                ThreadUtil.execute(() -> this.updateNodeItem(id, node, session, agentFileModel, http));
            }
        } catch (Exception e) {
            log.error("升级失败", e);
            this.onError(session, "升级失败 " + e.getMessage());
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
                        this.sendMsg(callbackRestartMessage.setData("重启完成"), session);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("{} 节点连接失败 {}", id, e.getMessage());
                }
            }
            this.sendMsg(callbackRestartMessage.setData("重连失败"), session);
        } catch (Exception e) {
            log.error("升级后重连插件端失败:" + id, e);
            this.sendMsg(callbackRestartMessage.setData("重连插件端失败"), session);
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
                        this.sendMsg(restartMessage.setData("重启完成"), session);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("{} 节点连接失败 {}", id, e.getMessage());
                }
                ThreadUtil.sleep(1000L);
                ++retryCount;
            }
            this.sendMsg(restartMessage.setData("重连失败"), session);
        } catch (Exception e) {
            log.error("升级后重连插件端失败:" + id, e);
            this.sendMsg(restartMessage.setData("重连插件端失败"), session);
        }
        return false;
    }

    private void updateNodeItem(String id, MachineNodeModel node, WebSocketSession session, AgentFileModel agentFileModel, boolean http) {
        try {
            IProxyWebSocket client = clientMap.get(node.getId());
            if (client == null) {
                this.onError(session, "对应的插件端还没有被初始化", id);
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
                this.onError(session, "节点连接丢失或者还没有连接上", id);
            }
        } catch (Exception e) {
            log.error("升级失败:" + id, e);
            this.onError(session, "节点升级失败：" + e.getMessage(), id);
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
