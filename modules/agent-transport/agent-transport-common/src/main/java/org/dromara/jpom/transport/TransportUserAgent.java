/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 传输层出站标识装配点。
 * <p>传输模块不能依赖业务层（{@code JpomUserAgent} 在业务模块），因此由业务层在启动时注册取值函数；
 * 传输层所有出站通道（hutool 全局头之外的隧道握手、回环 ws、服务端到插件端 ws）统一从这里取头。</p>
 *
 * @author jpom
 * @since 2026/11/26
 */
public final class TransportUserAgent {

    private static volatile Supplier<String> supplier = () -> null;

    private TransportUserAgent() {
    }

    /**
     * 注册 UA 取值函数（业务层启动时调用一次）
     *
     * @param userAgentSupplier UA 取值函数，传 {@code null} 表示清除
     */
    public static void set(Supplier<String> userAgentSupplier) {
        supplier = userAgentSupplier == null ? () -> null : userAgentSupplier;
    }

    /**
     * 出站请求头（UA 为空时返回空表，调用方可安全合并）
     *
     * @return 头表
     */
    public static Map<String, String> headers() {
        String value;
        try {
            value = supplier.get();
        } catch (RuntimeException e) {
            return Collections.emptyMap();
        }
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(TransportServer.USER_AGENT, value);
        return headers;
    }
}
