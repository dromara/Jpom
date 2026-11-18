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

/**
 * 隧道上下线事件（服务端用于刷新节点在线状态）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelConnectionListener {

    /**
     * 隧道上线（HELLO 通过）
     *
     * @param channel 通道
     */
    void online(TunnelChannel channel);

    /**
     * 隧道下线
     *
     * @param machineId 机器节点标识
     * @param code      关闭码
     * @param reason    原因
     */
    void offline(String machineId, int code, String reason);
}
