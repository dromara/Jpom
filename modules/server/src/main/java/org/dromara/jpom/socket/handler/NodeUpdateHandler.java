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
import org.dromara.jpom.common.update.AgentEngine;
import org.dromara.jpom.common.update.AgentPackageInfo;
import org.dromara.jpom.common.update.AgentPackageResolver;
import org.dromara.jpom.common.update.PackageCheckResult;
import org.dromara.jpom.configuration.NodeConfig;
import org.dromara.jpom.configuration.SystemConfig;
import org.dromara.jpom.func.assets.model.MachineNodeModel;
import org.dromara.jpom.func.assets.server.MachineNodeServer;
import org.dromara.jpom.model.AgentFileModel;
import org.dromara.jpom.model.UploadFileModel;
import org.dromara.jpom.model.WebSocketMessageModel;
import org.dromara.jpom.permission.ClassFeature;
import org.dromara.jpom.permission.Feature;
import org.dromara.jpom.permission.MethodFeature;
import org.dromara.jpom.permission.SystemPermission;
import org.dromara.jpom.service.system.AgentPackageService;
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
 * <p>在线升级时会先探测节点运行引擎（Agent /info manifest.engine）：Java 节点继续使用
 * 系统参数 AgentFileModel.ID 中上传的 Java 升级包；Go 节点使用 Go 升级容器
 * （agent-go-&lt;version&gt;-upgrade.jar，来源为上传包或下载中心自动下载），
 * 上传链路复用 /system/upload-jar-sharding* 与 /node_update。</p>
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
    private final AgentPackageService agentPackageService;
    private final SystemConfig systemConfig;

    public NodeUpdateHandler(MachineNodeServer machineNodeServer,
                             SystemParametersServer systemParametersServer,
                             NodeConfig nodeConfig,
                             AgentPackageService agentPackageService,
                             SystemConfig systemConfig) {
        super(null);
        this.machineNodeServer = machineNodeServer;
        this.systemParametersServer = systemParametersServer;
        this.nodeConfig = nodeConfig;
        this.agentPackageService = agentPackageService;
        this.systemConfig = systemConfig;
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
            // Java 节点升级包保持原有来源（AgentFileModel.ID），Go 节点使用独立的 Go 升级包
            AgentFileModel javaPackage = agentPackageService.get(AgentEngine.JAVA);
            AgentFileModel goPackage = agentPackageService.get(AgentEngine.GO);
            // 兼容旧数据：早期手工上传的 Go 容器可能仍保存在 AgentFileModel.ID 中
            if (goPackage == null && javaPackage != null
                && AgentPackageResolver.isGoContainer(FileUtil.file(javaPackage.getSavePath()))) {
                goPackage = javaPackage;
                javaPackage = null;
            }
            if (javaPackage == null && goPackage == null) {
                this.onError(session, I18nMessageUtil.get("i18n.agent_jar_not_exist.28ac"));
                return;
            }
            final AgentFileModel javaPackageFinal = javaPackage;
            final AgentFileModel goPackageFinal = goPackage;
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.getString(i);
                MachineNodeModel node = machineNodeServer.getByKey(id);
                if (node == null) {
                    this.onError(session, I18nMessageUtil.get("i18n.no_node_specified.fa3d") + id);
                    continue;
                }
                I18nThreadUtil.execute(() -> this.updateNodeItem(id, node, session, javaPackageFinal, goPackageFinal, http));
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
            log.error("{}{}", I18nMessageUtil.get("i18n.reconnect_plugin_failure_after_upgrade.73e3"), id, e);
            this.sendMsg(restartMessage.setData(I18nMessageUtil.get("i18n.reconnect_plugin_failure.cc6c")), session);
        }
        return false;
    }

    private void updateNodeItem(String id, MachineNodeModel node, WebSocketSession session, AgentFileModel javaPackage, AgentFileModel goPackage, boolean http) {
        try {
            IProxyWebSocket client = clientMap.get(node.getId());
            if (client == null) {
                this.onError(session, I18nMessageUtil.get("i18n.plugin_not_initialized.3ea9"), id);
                return;
            }
            if (!client.isConnected()) {
                this.onError(session, I18nMessageUtil.get("i18n.node_connection_lost.b6c7"), id);
                return;
            }
            // 探测节点运行引擎与当前版本（engine=go 时使用 Go 升级容器）
            AgentEngine engine;
            String nodeVersion;
            try {
                JsonMessage<Object> infoMessage = NodeForward.request(node, StrUtil.EMPTY, NodeUrl.Info, "nodeId", id);
                if (!infoMessage.success()) {
                    this.onError(session, I18nMessageUtil.get("i18n.node_connection_failure.896d") + infoMessage.getMsg(), id);
                    return;
                }
                JSONObject infoData = JSONObject.from(infoMessage.getData());
                engine = AgentPackageResolver.resolveEngine(infoData);
                nodeVersion = AgentPackageResolver.resolveVersion(infoData);
            } catch (Exception e) {
                log.error(I18nMessageUtil.get("i18n.node_connection_failed.8497"), id, e.getMessage());
                this.onError(session, I18nMessageUtil.get("i18n.node_connection_failure.896d") + e.getMessage(), id);
                return;
            }
            AgentFileModel packageModel;
            try {
                packageModel = this.selectPackageForNode(engine, nodeVersion, javaPackage, goPackage);
            } catch (PackageSelectionException e) {
                this.onError(session, e.getMessage(), id);
                return;
            }
            boolean result = http ? this.updateNodeItemHttp(node, session, packageModel) : this.updateNodeItemWebSocket(client, id, session, packageModel);
            if (result) {
                //
                WebSocketMessageModel command = new WebSocketMessageModel("getVersion", node.getId());
                client.send(command.toString());
            }
        } catch (Exception e) {
            log.error("{}{}", I18nMessageUtil.get("i18n.upgrade_failure_with_colon.59f1"), id, e);
            this.onError(session, I18nMessageUtil.get("i18n.node_upgrade_failed.4493") + e.getMessage(), id);
        }
    }

    /**
     * 为节点选择升级包
     *
     * <p>Java 节点：保持原有行为，只用 AgentFileModel.ID 中上传的 Java 包（仅额外拦截引擎不匹配）；
     * Go 节点：优先使用本地 Go 升级容器，本地包不存在/不适用时从下载中心自动下载官方 Go 容器。</p>
     *
     * @param engine      节点引擎
     * @param nodeVersion 节点当前版本
     * @param javaPackage Java 升级包
     * @param goPackage   Go 升级包
     * @return 选中的升级包
     */
    private AgentFileModel selectPackageForNode(AgentEngine engine, String nodeVersion, AgentFileModel javaPackage, AgentFileModel goPackage) {
        if (engine == AgentEngine.JAVA) {
            String error = this.verifyPackage(javaPackage, engine, nodeVersion);
            if (error != null) {
                throw new PackageSelectionException(error);
            }
            return javaPackage;
        }
        // Go 节点：本地 Go 升级包优先（版本/引擎规则与 Java 一致）
        String localError = null;
        if (goPackage != null) {
            localError = this.verifyPackage(goPackage, engine, nodeVersion);
            if (localError == null) {
                return goPackage;
            }
        }
        // 本地包不存在或版本不适用时，尝试下载中心官方 Go 升级容器（方案 A 一键升级）
        try {
            AgentFileModel remotePackage = agentPackageService.downloadGoPackage();
            String remoteError = this.verifyPackage(remotePackage, engine, nodeVersion);
            if (remoteError != null) {
                throw new PackageSelectionException(remoteError);
            }
            return remotePackage;
        } catch (PackageSelectionException e) {
            throw e;
        } catch (Exception e) {
            if (localError != null) {
                throw new PackageSelectionException(localError);
            }
            // 不展示节点版本（避免误导），错误细节来自下载中心下载失败原因
            String message = StrUtil.format(I18nMessageUtil.get("i18n.go_agent_package_not_found.e47b"), "<version>");
            throw new PackageSelectionException(message + "：" + e.getMessage());
        }
    }

    /**
     * 校验升级包（先按旧逻辑 checkJpomJar，再做引擎/版本兼容校验）
     *
     * @param packageModel 升级包
     * @param engine       节点引擎
     * @param nodeVersion  节点当前版本
     * @return null 表示通过，否则为错误文案
     */
    private String verifyPackage(AgentFileModel packageModel, AgentEngine engine, String nodeVersion) {
        if (packageModel == null || StrUtil.isBlank(packageModel.getSavePath()) || !FileUtil.exist(packageModel.getSavePath())) {
            return I18nMessageUtil.get("i18n.agent_jar_not_exist.28ac");
        }
        File file = FileUtil.file(packageModel.getSavePath());
        AgentPackageInfo packageInfo;
        try {
            packageInfo = AgentPackageResolver.inspectPackage(file);
        } catch (IOException e) {
            return I18nMessageUtil.get("i18n.agent_jar_damaged.74a8") + e.getMessage();
        }
        if (engine == AgentEngine.GO) {
            // Go 节点：升级包必须是 Go 升级容器，按容器结构校验（不依赖旧 Server 的 Main-Class 加载检查）
            if (!packageInfo.isGoContainer()) {
                return this.buildCheckMessage(PackageCheckResult.ENGINE_MISMATCH, packageInfo, engine, nodeVersion);
            }
            String containerError = AgentPackageResolver.validateGoContainer(file);
            if (containerError != null) {
                return StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"), containerError);
            }
        } else {
            // Java 节点保持原有行为；额外拦截误上传的 Go 容器（避免 Java Agent 安装成坏包）
            if (packageInfo.isGoContainer()) {
                return this.buildCheckMessage(PackageCheckResult.ENGINE_MISMATCH, packageInfo, engine, nodeVersion);
            }
            JsonMessage<Tuple> error = JpomManifest.checkJpomJar(file.getAbsolutePath(), Type.Agent, false);
            if (!error.success()) {
                return I18nMessageUtil.get("i18n.agent_jar_damaged.74a8") + error.getMsg();
            }
        }
        PackageCheckResult result = AgentPackageResolver.verify(packageInfo, engine, nodeVersion, systemConfig.isAllowedDowngrade());
        return this.buildCheckMessage(result, packageInfo, engine, nodeVersion);
    }

    private String buildCheckMessage(PackageCheckResult result, AgentPackageInfo packageInfo, AgentEngine engine, String nodeVersion) {
        switch (result) {
            case OK:
                return null;
            case ENGINE_MISMATCH:
                return StrUtil.format(I18nMessageUtil.get("i18n.agent_package_engine_mismatch.6325"),
                    engine.getDisplay(), packageInfo.getEngine().getDisplay());
            case SAME_VERSION:
                return I18nMessageUtil.get("i18n.new_package_same_as_running_package.e25a");
            case MIN_VERSION:
                return StrUtil.format(I18nMessageUtil.get("i18n.incompatible_program_versions.5291"),
                    nodeVersion, packageInfo.getMinVersion());
            case DOWNGRADE:
                return I18nMessageUtil.get("i18n.online_upgrade_cannot_downgrade.d419");
            case INVALID_PACKAGE:
            default:
                return StrUtil.format(I18nMessageUtil.get("i18n.invalid_go_agent_upgrade_container.0755"),
                    packageInfo.getFileName());
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
        AgentFileModel agentFileModel = agentPackageService.get(AgentEngine.JAVA);
        if (agentFileModel == null) {
            // 没有 Java 包时展示 Go 升级包版本（混合集群/纯 Go 集群的界面提示）
            agentFileModel = agentPackageService.get(AgentEngine.GO);
        }
        return agentFileModel == null ? null : JSONObject.toJSONString(agentFileModel);
    }

    /**
     * 升级包选择失败（错误文案已经国际化）
     */
    private static class PackageSelectionException extends RuntimeException {
        PackageSelectionException(String message) {
            super(message);
        }
    }
}
