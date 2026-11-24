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
import lombok.Data;
import org.dromara.jpom.transport.tunnel.TunnelConst;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 插件端反向隧道配置（{@code jpom.tunnel.*}，与 Go 插件端共用同一份配置键）。
 *
 * @author jpom
 * @since 2026/11/18
 */
@Data
@ConfigurationProperties("jpom.tunnel")
public class TunnelAgentProperties {

    /**
     * 服务端隧道地址，支持 {@code ws://} 与 {@code wss://}；可只写到主机，路径自动补 {@code /agent/tunnel}
     */
    private String serverUrl;

    /**
     * 连接密钥（必须与服务端 {@code jpom.agent.tunnel.key} 一致）
     */
    private String key;

    /**
     * 本插件端在服务端"机器节点资产"中的标识（machineId）
     */
    private String machineId;

    /**
     * 回环请求超时（秒）
     */
    private int timeoutSeconds = 30;

    /**
     * 并发处理上限
     */
    private int maxConcurrency = TunnelConst.DEFAULT_MAX_CONCURRENCY;

    /**
     * 是否主动推送节点状态（服务端据此切换到推送模式，见 {@code jpom.agent.tunnel.push-max-age-seconds}）
     */
    private boolean pushEnabled = true;

    /**
     * 主动推送间隔（秒）
     */
    private int pushInterval = TunnelConst.DEFAULT_PUSH_INTERVAL_SECONDS;

    /**
     * 是否启用（三项配置齐全才启用）
     *
     * @return true 启用
     */
    public boolean enabled() {
        return StrUtil.isNotEmpty(serverUrl) && StrUtil.isNotEmpty(key) && StrUtil.isNotEmpty(machineId);
    }
}
