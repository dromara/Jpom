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

import java.util.Map;

/**
 * 插件端回写响应（服务端侧 {@code REQ} 的应答通道）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelResponseSender {

    /**
     * 发送响应头
     *
     * @param status  HTTP 状态码
     * @param headers 响应头
     */
    void head(int status, Map<String, String> headers);

    /**
     * 发送响应体片段（自动按 256 KiB 分帧）
     *
     * @param payload 数据
     */
    void write(byte[] payload);

    /**
     * 结束响应（带 FIN 的数据帧 + END 帧）
     */
    void end();

    /**
     * 中止响应
     *
     * @param code 错误码
     * @param msg  说明
     */
    void abort(int code, String msg);
}
