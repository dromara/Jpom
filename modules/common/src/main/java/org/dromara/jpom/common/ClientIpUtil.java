/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析入口（静态持有器）。
 * <p>
 * server 模块启动时由 {@code ClientIpResolver} 注册 {@link Resolver} 实现，统一决定是否信任
 * {@code X-Forwarded-For} / {@code X-Real-IP} 等可伪造的转发头；agent 等没有注册实现的场景
 * 保持原有 {@link ServletUtil#getClientIP(HttpServletRequest)} 行为，避免破坏兼容性。
 *
 * @author jpom
 * @since 2026/11/2
 */
@Slf4j
public final class ClientIpUtil {

    private static volatile Resolver resolver;

    private ClientIpUtil() {
    }

    /**
     * 客户端 IP 解析器
     */
    public interface Resolver {
        /**
         * 解析客户端 IP
         *
         * @param request 请求
         * @return 客户端 IP
         */
        String resolve(HttpServletRequest request);
    }

    /**
     * 注册解析器（server 启动时调用）
     *
     * @param resolver 解析器，允许为 null（恢复默认 Hutool 行为）
     */
    public static void setResolver(Resolver resolver) {
        ClientIpUtil.resolver = resolver;
    }

    public static Resolver getResolver() {
        return resolver;
    }

    /**
     * 解析客户端 IP
     *
     * @param request 请求
     * @return 客户端 IP
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Resolver current = resolver;
        if (current == null) {
            // 未注册解析器（例如 agent），保持原有 Hutool 行为
            return ServletUtil.getClientIP(request);
        }
        try {
            String ip = current.resolve(request);
            if (StrUtil.isNotBlank(ip)) {
                return ip;
            }
        } catch (Exception e) {
            // 解析异常时只回退到容器提供、客户端无法伪造的 remoteAddr
            log.warn("parse client ip failed, fallback to remoteAddr", e);
            return request.getRemoteAddr();
        }
        String remoteAddr = request.getRemoteAddr();
        if (StrUtil.isNotBlank(remoteAddr)) {
            return remoteAddr;
        }
        return ServletUtil.getClientIP(request);
    }
}
