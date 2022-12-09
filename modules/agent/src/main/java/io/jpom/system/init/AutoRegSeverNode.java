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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.JsonMessage;
import io.jpom.common.ServerOpenApi;
import io.jpom.system.AgentAuthorize;
import io.jpom.system.AgentConfigBean;
import io.jpom.system.AgentExtConfigBean;
import io.jpom.system.ConfigBean;
import io.jpom.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

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
@Slf4j
@Configuration
public class AutoRegSeverNode implements InitializingBean {

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
        try (HttpResponse execute = HttpUtil.createGet(build, true).execute()) {
            String body = execute.body();
            log.info("push result:" + body);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }
}
