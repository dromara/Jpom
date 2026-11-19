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

import java.util.function.Consumer;

/**
 * 隧道上的一条双向流（对应正向模式的 {@code websocket} 原语）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelStream {

    /**
     * 流标识
     *
     * @return sid
     */
    String sid();

    /**
     * 注册对端数据回调
     *
     * @param consumer 回调
     */
    void onData(Consumer<byte[]> consumer);

    /**
     * 注册对端关闭回调
     *
     * @param consumer 回调（关闭码）
     */
    void onClose(Consumer<Integer> consumer);

    /**
     * 写入数据
     *
     * @param payload 数据
     */
    void write(byte[] payload);

    /**
     * 关闭流
     *
     * @param code   关闭码
     * @param reason 说明
     */
    void close(int code, String reason);

    /**
     * 是否已关闭
     *
     * @return true 已关闭
     */
    boolean isClosed();
}
