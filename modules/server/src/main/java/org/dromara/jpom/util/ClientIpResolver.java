/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.util;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ClientIpUtil;
import org.dromara.jpom.configuration.WebConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端 IP 解析器（server 模块）。
 * <p>
 * 解析规则（Tomcat RemoteIpValve 语义）：
 * <ol>
 *     <li>只有 {@code remoteAddr}（容器提供、客户端无法伪造）命中可信代理列表时才信任转发头；</li>
 *     <li>可信代理场景下，{@code X-Forwarded-For} 从右向左取第一个“不是可信代理”的合法 IP；</li>
 *     <li>没有可用 {@code X-Forwarded-For} 时回退合法的 {@code X-Real-IP}，再回退 {@code remoteAddr}；</li>
 *     <li>非法/空值跳过，不会被当作客户端 IP。</li>
 * </ol>
 * 支持 IPv4/IPv6 与 CIDR，支持 {@code 1.2.3.4:port}、{@code [IPv6]:port}、IPv6 方括号归一化。
 *
 * @author jpom
 * @since 2026/11/2
 */
@Component
@Slf4j
public class ClientIpResolver implements ClientIpUtil.Resolver, InitializingBean {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private static final int IPV4_BYTES = 4;
    private static final int IPV6_BYTES = 16;

    private final WebConfig webConfig;

    /**
     * 可信代理列表（解析后的 CIDR）
     */
    private volatile List<IpRange> trustedProxies = Collections.emptyList();

    public ClientIpResolver(WebConfig webConfig) {
        this.webConfig = webConfig;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> configProxies = webConfig.getTrustedProxies();
        List<IpRange> ranges = new ArrayList<>();
        if (configProxies != null) {
            for (String item : configProxies) {
                if (StrUtil.isBlank(item)) {
                    continue;
                }
                IpRange range = IpRange.parse(item);
                if (range == null) {
                    log.warn("jpom.web.trusted-proxies 配置项非法，已忽略：{}", item);
                    continue;
                }
                ranges.add(range);
            }
        }
        this.trustedProxies = Collections.unmodifiableList(ranges);
        ClientIpUtil.setResolver(this);
        log.info("client ip resolver initialized, trusted proxies: {}", configProxies);
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String remoteAddr = normalizeIp(request.getRemoteAddr());
        if (remoteAddr == null) {
            // remoteAddr 不是合法 IP 时不信任任何转发头
            return request.getRemoteAddr();
        }
        if (!this.isTrustedProxy(remoteAddr)) {
            // 非可信代理直连：忽略客户端可伪造的 X-Forwarded-For / X-Real-IP
            return remoteAddr;
        }
        // 可信代理：X-Forwarded-For 从右向左取第一个“不可信且合法”的 IP
        String forwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StrUtil.isNotBlank(forwardedFor)) {
            String[] items = StrUtil.splitToArray(forwardedFor, ',');
            for (int i = items.length - 1; i >= 0; i--) {
                String ip = normalizeIp(items[i]);
                if (ip == null) {
                    continue;
                }
                if (!this.isTrustedProxy(ip)) {
                    return ip;
                }
            }
        }
        // X-Real-IP 只可能由（可信）代理写入
        String realIp = normalizeIp(request.getHeader(HEADER_X_REAL_IP));
        if (realIp != null) {
            return realIp;
        }
        return remoteAddr;
    }

    /**
     * 判断 IP 是否是可信代理
     *
     * @param ip 已归一化的 IP
     * @return true 可信
     */
    private boolean isTrustedProxy(String ip) {
        byte[] address = toBytes(ip);
        if (address == null) {
            return false;
        }
        for (IpRange range : this.trustedProxies) {
            if (range.matches(address)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // IP 文本归一化（不做 DNS 解析）
    // ------------------------------------------------------------------

    /**
     * 归一化 IP 文本：去空白/引号/端口/方括号，返回标准 IPv4 或 IPv6 文本（IPv4 去前导 0）。
     *
     * @param raw 原始值
     * @return 归一化后的 IP，非法返回 null
     */
    static String normalizeIp(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        // 去引号
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                value = value.substring(1, value.length() - 1).trim();
                if (value.isEmpty()) {
                    return null;
                }
            }
        }
        // [IPv6] 或 [IPv6]:port
        if (value.charAt(0) == '[') {
            int end = value.indexOf(']');
            if (end <= 1) {
                return null;
            }
            String inner = value.substring(1, end);
            String rest = value.substring(end + 1).trim();
            if (!rest.isEmpty() && (rest.charAt(0) != ':' || !isDigits(rest.substring(1)))) {
                return null;
            }
            return normalizeIpv6(inner);
        }
        int firstColon = value.indexOf(':');
        if (firstColon < 0) {
            return normalizeIpv4(value);
        }
        int lastColon = value.lastIndexOf(':');
        if (firstColon == lastColon) {
            // 单冒号：IPv4:port
            String port = value.substring(firstColon + 1);
            if (!isDigits(port)) {
                return null;
            }
            return normalizeIpv4(value.substring(0, firstColon));
        }
        // 多冒号：IPv6 字面量（不支持未加方括号的端口写法，避免歧义）
        return normalizeIpv6(value);
    }

    private static String normalizeIpv4(String value) {
        if (!Validator.isIpv4(value)) {
            return null;
        }
        String[] parts = StrUtil.splitToArray(value, '.');
        if (parts.length != IPV4_BYTES) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < IPV4_BYTES; i++) {
            String part = parts[i].trim();
            if (part.isEmpty() || part.length() > 3) {
                return null;
            }
            int number = 0;
            for (int j = 0; j < part.length(); j++) {
                char ch = part.charAt(j);
                if (ch < '0' || ch > '9') {
                    return null;
                }
                number = number * 10 + (ch - '0');
            }
            if (number > 255) {
                return null;
            }
            if (i > 0) {
                builder.append('.');
            }
            builder.append(number);
        }
        return builder.toString();
    }

    private static String normalizeIpv6(String value) {
        if (value.isEmpty() || value.length() > 45) {
            return null;
        }
        // 不支持 zone id（fe80::1%eth0），避免不同节点解析结果不一致
        if (value.indexOf('%') >= 0 || !Validator.isIpv6(value)) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(value);
            String hostAddress = address.getHostAddress();
            if (hostAddress.indexOf(':') >= 0) {
                return hostAddress;
            }
            // IPv4-mapped（::ffff:1.2.3.4）由 JDK 归一化成 IPv4
            return normalizeIpv4(hostAddress);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 把归一化后的 IP 文本转成字节（IPv4 4 字节 / IPv6 16 字节），不做 DNS 解析
     *
     * @param ip 归一化后的 IP
     * @return 字节，非法返回 null
     */
    static byte[] toBytes(String ip) {
        if (StrUtil.isBlank(ip)) {
            return null;
        }
        if (Validator.isIpv4(ip)) {
            String[] parts = StrUtil.splitToArray(ip, '.');
            if (parts.length != IPV4_BYTES) {
                return null;
            }
            byte[] bytes = new byte[IPV4_BYTES];
            for (int i = 0; i < IPV4_BYTES; i++) {
                int number;
                try {
                    number = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (number < 0 || number > 255) {
                    return null;
                }
                bytes[i] = (byte) number;
            }
            return bytes;
        }
        if (Validator.isIpv6(ip) && ip.indexOf('%') < 0) {
            try {
                byte[] address = InetAddress.getByName(ip).getAddress();
                return address.length == IPV6_BYTES || address.length == IPV4_BYTES ? address : null;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static boolean isDigits(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * IP/CIDR 区间
     */
    static final class IpRange {
        private final byte[] network;
        private final int prefixLength;

        private IpRange(byte[] network, int prefixLength) {
            this.network = network;
            this.prefixLength = prefixLength;
        }

        /**
         * 解析 IP 或 CIDR（支持 IPv4/IPv6），非法返回 null
         *
         * @param value 配置值，例如 127.0.0.1/32、198.51.100.0/24、::1/128
         * @return 区间
         */
        static IpRange parse(String value) {
            if (StrUtil.isBlank(value)) {
                return null;
            }
            String item = value.trim();
            int prefixLength = -1;
            String ipPart = item;
            int slashIndex = item.indexOf('/');
            if (slashIndex >= 0) {
                ipPart = item.substring(0, slashIndex).trim();
                String prefixPart = item.substring(slashIndex + 1).trim();
                if (!isDigits(prefixPart)) {
                    return null;
                }
                try {
                    prefixLength = Integer.parseInt(prefixPart);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            byte[] network = toBytes(normalizeIp(ipPart));
            if (network == null) {
                return null;
            }
            int maxPrefix = network.length * 8;
            if (prefixLength < 0) {
                prefixLength = maxPrefix;
            }
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                return null;
            }
            return new IpRange(network, prefixLength);
        }

        boolean matches(byte[] address) {
            if (address == null || address.length != this.network.length) {
                return false;
            }
            int fullBytes = this.prefixLength / 8;
            int remainderBits = this.prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != this.network[i]) {
                    return false;
                }
            }
            if (remainderBits > 0) {
                int mask = 0xFF << (8 - remainderBits);
                if ((address[fullBytes] & mask) != (this.network[fullBytes] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
