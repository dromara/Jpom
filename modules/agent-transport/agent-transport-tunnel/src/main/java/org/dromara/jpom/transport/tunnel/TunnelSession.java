/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.tunnel;

/**
 * 一条已建立的隧道连接（服务端侧与插件端侧共用同一套语义）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelSession {

    /**
     * 服务端分配的会话标识
     *
     * @return 会话标识
     */
    String sessionId();

    /**
     * 绑定的机器节点标识
     *
     * @return 机器节点标识
     */
    String machineId();

    /**
     * 连接是否可用
     *
     * @return true 可用
     */
    boolean isOpen();

    /**
     * 发送控制帧
     *
     * @param frame 控制帧
     */
    void sendControl(TunnelFrame frame);

    /**
     * 发送数据帧
     *
     * @param frame 数据帧
     */
    void sendData(TunnelDataFrame frame);

    /**
     * 关闭连接
     *
     * @param code   关闭码
     * @param reason 说明
     */
    void close(int code, String reason);
}
