/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel.ws;

import java.util.Collections;
import java.util.Map;

/**
 * 插件端隧道参数。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelClientOptions {

    /**
     * 服务端隧道地址，支持 {@code ws://} 与 {@code wss://}（可带 {@code /agent/tunnel} 路径）
     */
    private String serverUrl;
    /**
     * 机器节点标识
     */
    private String machineId;
    /**
     * 连接密钥
     */
    private String key;
    /**
     * 插件端版本
     */
    private String agentVersion;
    /**
     * 插件端系统
     */
    private String os;
    /**
     * 插件端架构
     */
    private String arch;
    /**
     * 本机授权账号（HELLO 上报，供服务端自动注册机器资产时补齐通讯账号）
     */
    private String authUser;
    /**
     * 本机授权摘要 sha1(name@pwd)（HELLO 上报，不含明文密码）
     */
    private String authToken;
    /**
     * 本机 HTTP 基础地址（如 {@code http://127.0.0.1:2123}）
     */
    private String localHttpBase;
    /**
     * 本机 WebSocket 基础地址（如 {@code ws://127.0.0.1:2123}）
     */
    private String localWsBase;
    /**
     * 回环请求附加请求头（如插件端授权头、传输加密头）
     */
    private Map<String, String> localHeaders = Collections.emptyMap();
    /**
     * 回环 WS 附加查询参数（如插件端授权参数）
     */
    private Map<String, String> localQuery = Collections.emptyMap();
    /**
     * 并发处理上限
     */
    private int maxConcurrency = 16;
    /**
     * 握手/回环超时（秒）
     */
    private int timeoutSeconds = 30;

    private TunnelRequestHandler requestHandler;
    private TunnelStreamHandler streamHandler;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAuthUser() {
        return authUser;
    }

    public void setAuthUser(String authUser) {
        this.authUser = authUser;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getLocalHttpBase() {
        return localHttpBase;
    }

    public void setLocalHttpBase(String localHttpBase) {
        this.localHttpBase = localHttpBase;
    }

    public String getLocalWsBase() {
        return localWsBase;
    }

    public void setLocalWsBase(String localWsBase) {
        this.localWsBase = localWsBase;
    }

    public Map<String, String> getLocalHeaders() {
        return localHeaders == null ? Collections.emptyMap() : localHeaders;
    }

    public void setLocalHeaders(Map<String, String> localHeaders) {
        this.localHeaders = localHeaders;
    }

    public Map<String, String> getLocalQuery() {
        return localQuery == null ? Collections.emptyMap() : localQuery;
    }

    public void setLocalQuery(Map<String, String> localQuery) {
        this.localQuery = localQuery;
    }

    public int getMaxConcurrency() {
        return Math.max(maxConcurrency, 1);
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public int getTimeoutSeconds() {
        return Math.max(timeoutSeconds, 1);
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public TunnelRequestHandler getRequestHandler() {
        return requestHandler;
    }

    public void setRequestHandler(TunnelRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    public TunnelStreamHandler getStreamHandler() {
        return streamHandler;
    }

    public void setStreamHandler(TunnelStreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }
}
