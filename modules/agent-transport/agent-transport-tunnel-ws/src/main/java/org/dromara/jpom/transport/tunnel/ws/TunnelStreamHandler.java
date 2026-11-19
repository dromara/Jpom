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
 * 插件端处理服务端发起的双向流（通常连接到本机 {@code /socket} 端点）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelStreamHandler {

    /**
     * 打开一条双向流
     *
     * @param open   打开帧（path/query/headers）
     * @param stream 双向流
     * @throws Exception 打开失败，异常会转成 {@code OPEN_ACK ok=false}
     */
    void open(TunnelFrame open, TunnelStream stream) throws Exception;
}
