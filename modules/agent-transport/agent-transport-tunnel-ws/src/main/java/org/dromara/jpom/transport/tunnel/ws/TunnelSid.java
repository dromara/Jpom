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

import cn.hutool.core.util.IdUtil;

/**
 * 隧道 sid 生成器。
 * <p>
 * sid 同时用于逻辑请求与双向流，长度必须小于 {@code TunnelConst.MAX_SID_LENGTH}（24）。
 *
 * @author jpom
 * @since 2026/11/18
 */
public final class TunnelSid {

    private TunnelSid() {
    }

    /**
     * 生成 sid
     *
     * @return 形如 {@code s-1a2b3c4d5e6f} 的标识
     */
    public static String next() {
        return "s-" + IdUtil.fastSimpleUUID().substring(0, 16);
    }
}
