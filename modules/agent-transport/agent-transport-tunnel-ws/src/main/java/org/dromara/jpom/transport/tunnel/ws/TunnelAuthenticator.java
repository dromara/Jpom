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
 * 隧道握手鉴权：由业务层实现（校验连接密钥、机器节点是否存在、是否允许自动注册）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public interface TunnelAuthenticator {

    /**
     * 校验 HELLO 帧
     *
     * @param hello         握手帧
     * @param remoteAddress 客户端地址（审计与白名单用）
     * @return 鉴权结果
     */
    TunnelAuthResult authenticate(TunnelFrame hello, String remoteAddress);
}
