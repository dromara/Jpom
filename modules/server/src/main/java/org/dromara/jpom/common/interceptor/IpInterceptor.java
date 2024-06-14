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


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        String clientIp = ServletUtil.getClientIP(request);
        if (StrUtil.equals(NetUtil.LOCAL_IP, clientIp) || !Validator.isIpv4(clientIp)) {
            // 本地 或者 非 ipv4 直接放开
            return true;
        }
        SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
        if (config == null) {
            return true;
        }
        // 判断不允许访问
        String prohibited = config.getProhibited();
        try {
            if (StrUtil.isNotEmpty(prohibited) && this.checkIp(prohibited, clientIp, false)) {
                ServletUtil.write(response, JsonMessage.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
                return false;
            }
            String allowed = config.getAllowed();
            if (StrUtil.isEmpty(allowed) || this.checkIp(allowed, clientIp, true)) {
                return true;
            }
        } catch (Exception e) {
            log.warn(I18nMessageUtil.get("i18n.ip_authorization_interception_exception.8130"), e);
            return true;
        }
        ServletUtil.write(response, JsonMessage.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
        return false;
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
