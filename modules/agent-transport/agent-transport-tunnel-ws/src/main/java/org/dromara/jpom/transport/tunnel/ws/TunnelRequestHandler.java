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
 * 插件端处理服务端下发的逻辑请求（通常转发到本机 HTTP 接口）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelRequestHandler {

    /**
     * 处理请求
     *
     * @param request 请求帧（method/path/headers/contentType）
     * @param body    完整请求体
     * @param sender  响应回写通道
     * @throws Exception 处理异常，异常会被转成 ABORT 帧
     */
    void handle(TunnelFrame request, byte[] body, TunnelResponseSender sender) throws Exception;
}
