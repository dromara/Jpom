/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.net.Ipv4Util;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.pattern.CronPatternUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.keepbx.jpom.IJsonMessage;
import cn.keepbx.jpom.model.JsonMessage;
import lombok.Lombok;
import org.dromara.jpom.common.validator.ValidatorItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工具类
 *
 * @author bwcx_jzy
 * @since 2023/3/10
 */
@RestController
@RequestMapping(value = "/tools")
public class ToolsController {

    @GetMapping(value = "cron", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<Long>> cron(@ValidatorItem String cron, @ValidatorItem int count, String date, boolean isMatchSecond) {
        Date startDate = null;
        Date endDate = null;
        if (StrUtil.isNotEmpty(date)) {
            List<String> split = StrUtil.splitTrim(date, "~");
            try {
                startDate = DateUtil.parse(split.get(0));
                startDate = DateUtil.beginOfDay(startDate);
                endDate = DateUtil.parse(split.get(1));
                endDate = DateUtil.endOfDay(endDate);
            } catch (Exception e) {
                return new JsonMessage<>(405, "日期格式错误:" + e.getMessage());
            }
        }
        try {
            List<Date> dateList;
            if (startDate != null) {
                dateList = CronPatternUtil.matchedDates(cron, startDate, endDate, count, isMatchSecond);
            } else {
                dateList = CronPatternUtil.matchedDates(cron, DateTime.now(), count, isMatchSecond);
            }
            return JsonMessage.success("", dateList.stream().map(Date::getTime).collect(Collectors.toList()));
        } catch (Exception e) {
            return new JsonMessage<>(405, "cron 表达式不正确," + e.getMessage());
        }
    }

    @GetMapping(value = "ip-list", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<List<JSONObject>> ipList() {
        Collection<NetworkInterface> networkInterfaces = NetUtil.getNetworkInterfaces();
        List<JSONObject> collect = networkInterfaces.stream()
            .sorted((o1, o2) -> 0)
            .map(networkInterface -> {
                boolean virtual = networkInterface.isVirtual();
                String name = networkInterface.getName();
                String displayName = networkInterface.getDisplayName();
                JSONObject jsonObject = new JSONObject();
                jsonObject.set("name", name);
                jsonObject.set("displayName", displayName);
                jsonObject.set("virtual", virtual);
                try {
                    jsonObject.set("loopback", networkInterface.isLoopback());
                } catch (SocketException e) {
                    throw Lombok.sneakyThrow(e);
                }
                final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                JSONArray ips = new JSONArray();
                while (inetAddresses.hasMoreElements()) {
                    final InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress != null && !inetAddress.isLinkLocalAddress()) {
                        String hostAddress = inetAddress.getHostAddress();
                        // 处理 Mac  ip 地址
                        hostAddress = StrUtil.subBefore(hostAddress, "%", true);
                        JSONObject parseIp = parseIp(hostAddress);
                        parseIp.set("ip", hostAddress);
                        ips.add(parseIp);
                    }
                }
                if (CollUtil.isEmpty(ips)) {
                    return null;
                }
                jsonObject.set("ips", ips);
                return jsonObject;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return JsonMessage.success("", collect);
    }

    @GetMapping(value = "net-ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> ping(@ValidatorItem String host, @ValidatorItem int timeout) {
        boolean ping = NetUtil.ping(host, (int) TimeUnit.SECONDS.toMillis(Math.max(1, timeout)));
        //
        JSONObject jsonObject = this.parseIp(host);
        jsonObject.set("ping", ping);
        return JsonMessage.success("", jsonObject);
    }

    @GetMapping(value = "net-telnet", produces = MediaType.APPLICATION_JSON_VALUE)
    public IJsonMessage<JSONObject> telnet(@ValidatorItem String host, int port, @ValidatorItem int timeout) {
        InetSocketAddress address = NetUtil.createAddress(host, port);
        boolean open = NetUtil.isOpen(address, (int) TimeUnit.SECONDS.toMillis(Math.max(1, timeout)));
        //
        JSONObject jsonObject = this.parseIp(host);
        jsonObject.set("open", open);
        return JsonMessage.success("", jsonObject);
    }

    /**
     * 解析 host 的 IP 地址类型
     *
     * @param host 主机地址
     * @return json
     */
    private JSONObject parseIp(String host, String... appendLabels) {
        boolean ipv4 = Validator.isIpv4(host);
        boolean ipv6 = Validator.isIpv6(host);
        JSONObject jsonObject = new JSONObject();
        //
        JSONArray labels = new JSONArray();
        for (String appendLabel : appendLabels) {
            labels.put(appendLabel);
        }
        if (ipv4) {
            labels.put("IPV4");
            //
            String type = detectionType(host);
            if (type != null) {
                labels.add(type);
            }
        }
        if (ipv6) {
            labels.put("IPV6");
        }
        if (!ipv4 && !ipv6) {
            String ipByHost = NetUtil.getIpByHost(host);
            if (!StrUtil.equals(ipByHost, host)) {
                jsonObject.set("originalIP", ipByHost);
            }
            labels.put("DOMAIN");
        }

        jsonObject.set("labels", labels);
        return jsonObject;
    }


    private static String detectionType(String ipAddress) {
        if (Ipv4Util.LOCAL_IP.equals(ipAddress)) {
            return "LOCAL";
        }
        long ipNum = Ipv4Util.ipv4ToLong(ipAddress);

        long aBegin = Ipv4Util.ipv4ToLong("10.0.0.0");
        long aEnd = Ipv4Util.ipv4ToLong("10.255.255.255");
        if (isInclude(ipNum, aBegin, aEnd)) {
            return "A";
        }

        long bBegin = Ipv4Util.ipv4ToLong("172.16.0.0");
        long bEnd = Ipv4Util.ipv4ToLong("172.31.255.255");
        if (isInclude(ipNum, bBegin, bEnd)) {
            return "B";
        }

        long cBegin = Ipv4Util.ipv4ToLong("192.168.0.0");
        long cEnd = Ipv4Util.ipv4ToLong("192.168.255.255");
        if (isInclude(ipNum, cBegin, cEnd)) {
            return "C";
        }

        long pBegin = Ipv4Util.ipv4ToLong("20.0.0.0");
        long pEnd = Ipv4Util.ipv4ToLong("223.255.255.255");
        if (isInclude(ipNum, pBegin, pEnd)) {
            return "PUBLIC";
        }
        return null;
    }

    private static boolean isInclude(long userIp, long begin, long end) {
        return (userIp >= begin) && (userIp <= end);
    }
}
