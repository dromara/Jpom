/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom.system.init;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.PreLoadClass;
import cn.jiangzeyin.common.PreLoadMethod;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.system.ConfigBean;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自动注册server 节点
 *
 * @author bwcx_jzy
 * @since 2019/8/6
 */
@PreLoadClass
@Slf4j
public class AutoRegSeverNode {

    /**
     * 向服务端注册插件端
     */
    @PreLoadMethod
    private static void reg() {
        AgentExtConfigBean instance = AgentExtConfigBean.getInstance();
        String agentId = instance.getAgentId();
        String serverUrl = instance.getServerUrl();
        if (StrUtil.isEmpty(agentId) || StrUtil.isEmpty(serverUrl)) {
            //  如果二者缺一不注册
            return;
        }
        String oldInstallId = null;
        File file = FileUtil.file(ConfigBean.getInstance().getDataPath(), AgentConfigBean.SERVER_ID);
        JSONObject serverJson = null;
        if (file.exists()) {
            try {
                serverJson = (JSONObject) JsonFileUtil.readJson(file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                serverJson = new JSONObject();
            }
            oldInstallId = serverJson.getString("installId");
        }
        HttpRequest installRequest = instance.createServerRequest(ServerOpenApi.INSTALL_ID);
        try (HttpResponse execute = installRequest.execute()) {
            String body1 = execute.body();
            JsonMessage<?> jsonMessage = JSON.parseObject(body1, JsonMessage.class);
            if (jsonMessage.getCode() != HttpStatus.HTTP_OK) {
                log.error("获取Server 安装id失败:" + jsonMessage);
                return;
            }
            String installId = jsonMessage.dataToString();
            boolean eqInstall = StrUtil.equals(oldInstallId, installId);
            //
            URL url = URLUtil.toUrlForHttp(instance.getAgentUrl());
            String protocol = url.getProtocol();

            HttpRequest serverRequest = instance.createServerRequest(ServerOpenApi.UPDATE_NODE_INFO);
            serverRequest.form("id", agentId);
            serverRequest.form("name", "节点：" + agentId);
            serverRequest.form("openStatus", 1);
            serverRequest.form("protocol", protocol);
            serverRequest.form("url", url.getHost() + CharPool.COLON + url.getPort());
            AgentAuthorize agentAuthorize = AgentAuthorize.getInstance();
            serverRequest.form("loginName", agentAuthorize.getAgentName());
            serverRequest.form("loginPwd", agentAuthorize.getAgentPwd());
            serverRequest.form("type", eqInstall ? "update" : "add");
            try (HttpResponse httpResponse = serverRequest.execute()) {
                String body = httpResponse.body();
                log.info("自动注册Server:" + body);
                JsonMessage<?> regJsonMessage = JSON.parseObject(body, JsonMessage.class);
                if (regJsonMessage.getCode() == HttpStatus.HTTP_OK) {
                    if (serverJson == null) {
                        serverJson = new JSONObject();
                    }
                    if (!eqInstall) {
                        serverJson.put("installId", installId);
                        serverJson.put("regTime", DateTime.now().toString());
                    } else {
                        serverJson.put("updateTime", DateTime.now().toString());
                    }
                    JsonFileUtil.saveJson(file.getAbsolutePath(), serverJson);
                } else {
                    log.error("自动注册插件端失败：{}", body);
                }
            }
        }
    }

    /**
     * 自动推送插件端信息到服务端
     *
     * @param url 服务端url
     */
    public static void autoPushToServer(String url) {
        url = StrUtil.removeSuffix(url, CharPool.SINGLE_QUOTE + "");
        url = StrUtil.removePrefix(url, CharPool.SINGLE_QUOTE + "");
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(url);
        String networkName = (String) urlBuilder.getQuery().get("networkName");
        //
        LinkedHashSet<InetAddress> localAddressList = NetUtil.localAddressList(networkInterface -> StrUtil.isEmpty(networkName) || StrUtil.equals(networkName, networkInterface.getName()), address -> {
            // 非loopback地址，指127.*.*.*的地址
            return !address.isLoopbackAddress()
                // 需为IPV4地址
                && address instanceof Inet4Address;
        });
        if (StrUtil.isNotEmpty(networkName) && CollUtil.isEmpty(localAddressList)) {
            log.warn("No usable IP found by NIC name,{}", networkName);
        }
        Set<String> ips = localAddressList.stream().map(InetAddress::getHostAddress).filter(StrUtil::isNotEmpty).collect(Collectors.toSet());
        urlBuilder.addQuery("ips", CollUtil.join(ips, StrUtil.COMMA));
        AgentAuthorize agentAuthorize = AgentAuthorize.getInstance();
        urlBuilder.addQuery("loginName", agentAuthorize.getAgentName());
        urlBuilder.addQuery("loginPwd", agentAuthorize.getAgentPwd());
        int port = ConfigBean.getInstance().getPort();
        urlBuilder.addQuery("port", port + "");
        //
        String build = urlBuilder.build();
        try (HttpResponse execute = HttpUtil.createGet(build).execute()) {
            String body = execute.body();
            Console.log("push result:" + body);
        }
    }
}
