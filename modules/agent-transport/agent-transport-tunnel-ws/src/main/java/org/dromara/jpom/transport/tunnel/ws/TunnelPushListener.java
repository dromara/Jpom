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

import org.dromara.jpom.transport.tunnel.TunnelFrame;

/**
 * 插件端主动推送（{@code PUSH} 帧）的服务端监听器。
 * <p>
 * 与请求/响应不同，推送是单向的：服务端只接收、不回应，因此不占用 sid，也不参与流控窗口。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelPushListener {

    /**
     * 收到一条推送
     *
     * @param channel 来源通道（{@link TunnelChannel#machineId()} 为推送节点标识）
     * @param frame   推送帧（{@code topic} / {@code seq} / {@code body}）
     */
    void onPush(TunnelChannel channel, TunnelFrame frame);
}
