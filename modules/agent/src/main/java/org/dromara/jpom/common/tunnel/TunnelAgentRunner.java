/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.tunnel;

import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.Const;
import org.dromara.jpom.configuration.AgentAuthorize;
import org.dromara.jpom.common.JpomManifest;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.configuration.AgentConfig;
import org.dromara.jpom.service.AgentStatInfoService;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.dromara.jpom.transport.tunnel.ws.LocalHttpTunnelHandler;
import org.dromara.jpom.transport.tunnel.ws.LocalWebSocketTunnelHandler;
import org.dromara.jpom.transport.tunnel.ws.TunnelClient;
import org.dromara.jpom.transport.tunnel.ws.TunnelClientOptions;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 插件端反向隧道启动器：配置齐全时主动连接服务端，并把请求转发到本机接口。
 * <p>
 * 只做出站连接，插件端不需要对服务端开放任何端口。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Slf4j
@Component
public class TunnelAgentRunner implements ApplicationRunner, DisposableBean {

    private final TunnelAgentProperties properties;
    private final AgentConfig agentConfig;
    private final JpomApplication jpomApplication;
    private final AgentStatInfoService agentStatInfoService;
    private final AgentAuthorize agentAuthorize;

    private volatile TunnelClient tunnelClient;
    private volatile ScheduledExecutorService pushScheduler;
    private final AtomicLong pushSeq = new AtomicLong();
    private volatile boolean pushUnsupportedLogged;

    public TunnelAgentRunner(TunnelAgentProperties properties, AgentConfig agentConfig, JpomApplication jpomApplication,
                             AgentStatInfoService agentStatInfoService,
                             AgentAuthorize agentAuthorize) {
        this.properties = properties;
        this.agentConfig = agentConfig;
        this.jpomApplication = jpomApplication;
        this.agentStatInfoService = agentStatInfoService;
        this.agentAuthorize = agentAuthorize;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            log.info("未完整配置 jpom.tunnel.server-url/key/machine-id，反向隧道未启用（保持仅监听本机端口）");
            return;
        }
        int port = jpomApplication.getPort();
        // 授权头由服务端随请求/流打开参数转发（插件端不读取自身授权摘要，AgentAuthorize#getAuthorize 亦不对外暴露）
        Map<String, String> authorize = Collections.emptyMap();
        TunnelClientOptions options = new TunnelClientOptions();
        options.setServerUrl(properties.getServerUrl());
        options.setMachineId(properties.getMachineId());
        options.setKey(properties.getKey());
        options.setAgentVersion(JpomManifest.getInstance().getVersion());
        // 服务端自动注册时需要：账号 + 授权摘要（不含明文密码）
        options.setAuthUser(agentAuthorize == null ? null : agentAuthorize.getAgentName());
        options.setAuthToken(agentAuthorize == null ? null : agentAuthorize.tunnelAuthToken());
        options.setOs(SystemUtil.getOsInfo().getName());
        options.setArch(SystemUtil.getOsInfo().getArch());
        options.setLocalHttpBase("http://127.0.0.1:" + port);
        options.setLocalWsBase("ws://127.0.0.1:" + port);
        options.setLocalHeaders(authorize);
        options.setLocalQuery(authorize);
        options.setMaxConcurrency(properties.getMaxConcurrency());
        options.setTimeoutSeconds(properties.getTimeoutSeconds());
        options.setRequestHandler(new LocalHttpTunnelHandler(options.getLocalHttpBase(), authorize, properties.getTimeoutSeconds()));
        options.setStreamHandler(new LocalWebSocketTunnelHandler(options.getLocalWsBase(), authorize, properties.getTimeoutSeconds()));
        this.tunnelClient = new TunnelClient(options);
        this.tunnelClient.start();
        this.startPushLoop();
        log.info("反向隧道已启动，服务端 {} 节点 {}，主动推送={}（间隔 {} 秒）", properties.getServerUrl(),
            properties.getMachineId(), properties.isPushEnabled() ? "开启" : "关闭", Math.max(properties.getPushInterval(), 5));
    }

    /**
     * 启动节点状态主动推送循环
     * <p>
     * 只有隧道在线时才采集与发送：采集本身有固定休眠（系统监控统计），离线时不做无用功。
     * 推送失败不缓存重放，下一轮重新采集，避免服务端收到过期的历史数据。
     */
    private void startPushLoop() {
        if (!properties.isPushEnabled()) {
            return;
        }
        int interval = Math.max(properties.getPushInterval(), 5);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jpom-tunnel-push");
            thread.setDaemon(true);
            return thread;
        });
        this.pushScheduler = scheduler;
        // 立即执行首轮（未连上时内部会直接返回），并把首次状态采集提前预热：
        // oshi 首次采集可能要几十秒，预热后首帧推送才不会被拖到 1 分钟以后。
        Thread warmUp = new Thread(() -> {
            try {
                this.agentStatInfoService.statInfo();
            } catch (Throwable throwable) {
                log.debug("节点状态采集预热失败：{}", throwable.getMessage());
            }
        }, "jpom-tunnel-push-warmup");
        warmUp.setDaemon(true);
        warmUp.start();
        scheduler.scheduleWithFixedDelay(this::pushStatOnce, 0, interval, TimeUnit.SECONDS);
    }

    private void pushStatOnce() {
        TunnelClient client = this.tunnelClient;
        if (client == null || !client.isConnected()) {
            return;
        }
        if (!client.isPushSupported()) {
            // 老服务端不认识 PUSH 帧（收到会按非法帧断开连接），能力位未声明时宁可只拉取
            if (!this.pushUnsupportedLogged) {
                log.info("服务端未声明支持主动推送（老版本服务端），插件端保持拉取模式、不发送推送帧");
                this.pushUnsupportedLogged = true;
            }
            return;
        }
        this.pushUnsupportedLogged = false;
        try {
            JSONObject statInfo = this.agentStatInfoService.statInfo();
            if (client.sendPush(TunnelConst.TOPIC_NODE_STAT, this.pushSeq.incrementAndGet(), statInfo.toJSONString())) {
                log.debug("节点状态已推送，seq={}", this.pushSeq.get());
            }
        } catch (Throwable throwable) {
            log.warn("节点状态推送失败", throwable);
        }
    }

    /**
     * 隧道是否在线（供状态接口使用）
     *
     * @return true 在线
     */
    public boolean online() {
        TunnelClient client = this.tunnelClient;
        return client != null && client.isConnected();
    }

    @Override
    public void destroy() {
        ScheduledExecutorService scheduler = this.pushScheduler;
        if (scheduler != null) {
            scheduler.shutdownNow();
            this.pushScheduler = null;
        }
        TunnelClient client = this.tunnelClient;
        if (client != null) {
            client.stop();
        }
    }
}