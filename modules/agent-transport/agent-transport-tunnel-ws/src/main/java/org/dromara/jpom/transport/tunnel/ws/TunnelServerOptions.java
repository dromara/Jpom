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

import org.dromara.jpom.transport.tunnel.TunnelConst;

/**
 * 服务端隧道参数（来自服务端配置）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public class TunnelServerOptions {

    /**
     * 心跳间隔（秒）
     */
    private int heartbeatSeconds = TunnelConst.DEFAULT_HEARTBEAT_SECONDS;
    /**
     * 连续未收到 PONG 的次数上限
     */
    private int maxMissedPong = TunnelConst.DEFAULT_MAX_MISSED_PONG;
    /**
     * 插件端并发处理上限（下发给插件端）
     */
    private int maxConcurrency = TunnelConst.DEFAULT_MAX_CONCURRENCY;
    /**
     * 握手超时（秒）：建连后多久没收到 HELLO 就断开
     */
    private int helloTimeoutSeconds = 30;
    /**
     * 流控窗口（字节）
     */
    private long windowBytes = TunnelConst.DEFAULT_WINDOW_BYTES;

    public int getHeartbeatSeconds() {
        return Math.max(heartbeatSeconds, 1);
    }

    public void setHeartbeatSeconds(int heartbeatSeconds) {
        this.heartbeatSeconds = heartbeatSeconds;
    }

    public int getMaxMissedPong() {
        return Math.max(maxMissedPong, 1);
    }

    public void setMaxMissedPong(int maxMissedPong) {
        this.maxMissedPong = maxMissedPong;
    }

    public int getMaxConcurrency() {
        return Math.max(maxConcurrency, 1);
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public int getHelloTimeoutSeconds() {
        return Math.max(helloTimeoutSeconds, 1);
    }

    public void setHelloTimeoutSeconds(int helloTimeoutSeconds) {
        this.helloTimeoutSeconds = helloTimeoutSeconds;
    }

    public long getWindowBytes() {
        return windowBytes <= 0 ? TunnelConst.DEFAULT_WINDOW_BYTES : windowBytes;
    }

    public void setWindowBytes(long windowBytes) {
        this.windowBytes = windowBytes;
    }
}
