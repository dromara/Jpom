/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.common.interceptor;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ClientIpUtil;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.model.data.SystemIpConfigModel;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ip 访问限制拦截器
 *
 * @author bwcx_jzy
 * @since 2021/4/18
 */
@Configuration
@Slf4j
public class IpInterceptor implements HandlerMethodInterceptor {

    private static final int IP_ACCESS_CODE = 999;

    @Resource
    private SystemParametersServer systemParametersServer;


    /**
     * 当前请求 IP 是否允许访问（不写响应，供 WebSocket 握手等非 MVC 场景复用）
     *
     * @param request 请求
     * @return true 允许
     */
    public boolean checkAccess(HttpServletRequest request) {
        return this.checkAccessResult(request) == AccessResult.ALLOW;
    }

    /**
     * 当前请求的访问判定结果。
     * <p>
     * 与 {@link #checkAccess(HttpServletRequest)} 的区别：把「读不到 IP 配置（服务端故障）」与
     * 「IP 不在白名单（配置生效）」区分开。两者都拒绝访问，但响应必须不同——以前一律回
     * {@code 999 Prohibition of access}，前端会跳到 /prohibit-access，把「数据库/系统参数读取异常」
     * 伪装成「IP 被限制」，用户按 IP 白名单排查永远查不出问题（2026-11-21 重启计划群反馈：
     * 构建卡住后重新构建全是 999）。
     *
     * @param request 请求
     * @return 判定结果
     */
    public AccessResult checkAccessResult(HttpServletRequest request) {
        String clientIp = ClientIpUtil.resolve(request);
        if (this.isLoopback(clientIp)) {
            // 本地回环请求始终放行（保留原有行为）
            return AccessResult.ALLOW;
        }
        SystemIpConfigModel config;
        try {
            config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
        } catch (Exception e) {
            // 配置读取异常：无法确认访问限制，按 fail-closed 拒绝，但必须回「配置读取失败」而不是 IP 拒绝
            log.error(I18nMessageUtil.get("i18n.ip_authorization_interception_exception.8130"), e);
            return AccessResult.CONFIG_ERROR;
        }
        if (config == null) {
            return AccessResult.ALLOW;
        }
        // 判断不允许访问
        String prohibited = config.getProhibited();
        String allowed = config.getAllowed();
        try {
            if (!this.isIpv4(clientIp)) {
                // 黑白名单配置目前只支持 IPv4：非 IPv4（IPv6/非法转发头）无法匹配。
                // 为避免“伪造 IPv6/非法 X-Forwarded-For 即整体绕过白名单”，存在白名单限制时直接拒绝；
                // 仅配置黑名单（allowed 为空）时保持原放行语义，避免锁死 IPv6 部署。
                if (StrUtil.isEmpty(allowed) || this.isAllowAll(allowed)) {
                    return AccessResult.ALLOW;
                }
                log.warn("ip access denied, non-ipv4 client ip: {}", clientIp);
                return AccessResult.DENY;
            }
            if (StrUtil.isNotEmpty(prohibited) && this.checkIp(prohibited, clientIp, false)) {
                return AccessResult.DENY;
            }
            if (StrUtil.isEmpty(allowed) || this.checkIp(allowed, clientIp, true)) {
                return AccessResult.ALLOW;
            }
        } catch (Exception e) {
            // 配置解析异常：无法确认访问限制，按 fail-closed 拒绝，但同样回「配置读取失败」而不是 IP 拒绝
            log.error(I18nMessageUtil.get("i18n.ip_authorization_interception_exception.8130"), e);
            return AccessResult.CONFIG_ERROR;
        }
        return AccessResult.DENY;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        AccessResult accessResult = this.checkAccessResult(request);
        if (accessResult == AccessResult.ALLOW) {
            return true;
        }
        if (accessResult == AccessResult.CONFIG_ERROR) {
            // 服务端故障（读不到 IP 配置）不能伪装成 IP 拒绝，否则用户会一直去查白名单
            this.writeConfigError(response);
        } else {
            this.writeProhibition(response);
        }
        return false;
    }

    /**
     * IP 访问判定结果
     */
    public enum AccessResult {
        /**
         * 允许访问
         */
        ALLOW,
        /**
         * IP 黑名单命中 / 不在白名单：属于 IP 限制生效
         */
        DENY,
        /**
         * 服务端读不到或解析不了 IP 授权配置：属于服务端故障，不是 IP 限制
         */
        CONFIG_ERROR
    }

    private boolean isIpv4(String ip) {
        return StrUtil.isNotBlank(ip) && Validator.isIpv4(ip);
    }

    /**
     * 是否本地回环地址（兼容 IPv6 的等价写法）
     *
     * @param ip ip
     * @return true 本地回环
     */
    private boolean isLoopback(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return StrUtil.equals("127.0.0.1", ip)
            || StrUtil.equals("::1", ip)
            || StrUtil.equals("0:0:0:0:0:0:0:1", ip);
    }

    /**
     * 白名单是否配置了“全部放行”（0.0.0.0）
     *
     * @param allowed 白名单配置
     * @return true 全部放行
     */
    private boolean isAllowAll(String allowed) {
        String[] split = StrUtil.splitToArray(allowed, StrUtil.LF);
        for (String itemIp : split) {
            itemIp = itemIp.trim();
            if (itemIp.startsWith("#")) {
                continue;
            }
            if (StrUtil.equals(itemIp, "0.0.0.0")) {
                return true;
            }
        }
        return false;
    }

    private void writeProhibition(HttpServletResponse response) {
        ServletUtil.write(response, JsonMessage.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
    }

    private void writeConfigError(HttpServletResponse response) {
        ServletUtil.write(response, JsonMessage.getString(500, I18nMessageUtil.get("i18n.ip_access_config_read_failed.4b1c")), MediaType.APPLICATION_JSON_VALUE);
    }


    /**
     * 检查ip 地址是否可以访问
     *
     * @param value    配置的值
     * @param ip       被检查的 ip 地址
     * @param checkAll 是否检查开放所有、避免禁止所有 ip 访问
     * @return true 命中检查项
     */
    private boolean checkIp(String value, String ip, boolean checkAll) {
        long ipNum = NetUtil.ipv4ToLong(ip);
        String[] split = StrUtil.splitToArray(value, StrUtil.LF);
        boolean check;
        for (String itemIp : split) {
            itemIp = itemIp.trim();
            if (itemIp.startsWith("#")) {
                continue;
            }
            if (checkAll && StrUtil.equals(itemIp, "0.0.0.0")) {
                // 开放所有
                return true;
            }
            if (StrUtil.contains(itemIp, Ipv4Util.IP_MASK_SPLIT_MARK)) {
                // ip段
                String[] itemIps = StrUtil.splitToArray(itemIp, Ipv4Util.IP_MASK_SPLIT_MARK);
                int count1 = StrUtil.count(itemIps[0], StrUtil.DOT);
                int count2 = StrUtil.count(itemIps[1], StrUtil.DOT);
                if (count1 == 3 && count2 == 3) {
                    //192.168.1.0/192.168.1.200
                    long aBegin = NetUtil.ipv4ToLong(itemIps[0]);
                    long aEnd = NetUtil.ipv4ToLong(itemIps[1]);
                    check = (ipNum >= aBegin) && (ipNum <= aEnd);
                } else if (count1 == 3 && count2 == 0) {
                    //192.168.1.0/24
                    String startIp = Ipv4Util.getBeginIpStr(itemIps[0], Integer.parseInt(itemIps[1]));
                    String endIp = Ipv4Util.getEndIpStr(itemIps[0], Integer.parseInt(itemIps[1]));
                    long aBegin = NetUtil.ipv4ToLong(startIp);
                    long aEnd = NetUtil.ipv4ToLong(endIp);
                    check = (ipNum >= aBegin) && (ipNum <= aEnd);
                } else {
                    check = false;
                }

            } else {
                check = StrUtil.equals(itemIp, ip);
            }
            if (check) {
                return true;
            }
        }
        return false;
    }
}
