/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.tunnel;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 反向隧道服务端配置（{@code jpom.agent.tunnel.*}）。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Data
@ConfigurationProperties("jpom.agent.tunnel")
public class TunnelConfig {

    /**
     * 连接密钥：为空表示不启用反向隧道（服务端不注册端点）
     */
    private String key;

    /**
     * 未注册节点是否允许自动注册（默认 false：直接拒绝）
     */
    private boolean allowAutoRegister;

    /**
     * 心跳间隔（秒）
     */
    private int heartbeatSeconds = TunnelConst.DEFAULT_HEARTBEAT_SECONDS;

    /**
     * 连续未收到 PONG 的次数上限
     */
    private int maxMissedPong = TunnelConst.DEFAULT_MAX_MISSED_PONG;

    /**
     * 插件端并发处理上限
     */
    private int maxConcurrency = TunnelConst.DEFAULT_MAX_CONCURRENCY;

    /**
     * 握手超时（秒）
     */
    private int helloTimeoutSeconds = 30;

    /**
     * 插件端主动推送的数据允许的最大年龄（秒）
     * <p>
     * 超过该年龄仍未收到新的推送，视为节点不再上报，节点状态按无法连接处理。
     */
    private int pushMaxAgeSeconds = TunnelConst.DEFAULT_PUSH_MAX_AGE_SECONDS;

    /**
     * 自动注册的机器资产默认分配到哪个工作空间（工作空间 id）。
     * <p>
     * 为空（默认）表示只建机器资产、不分配，保持历史行为；配置后握手自动注册时会同时生成节点，
     * 让「插件端配好 key 即可用」，不需要人工到机器资产页再点「分配」。
     */
    private String autoRegisterWorkspace;

    /**
     * 是否启用
     *
     * @return true 已配置连接密钥
     */
    public boolean enabled() {
        return StrUtil.isNotEmpty(key);
    }
}
