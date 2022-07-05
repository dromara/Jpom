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

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import io.jpom.model.data.NodeModel;
import io.jpom.service.system.SystemParametersServer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 全局代理配置
 *
 * @author bwcx_jzy
 * @since 2022/7/4
 */
@Slf4j
@Configuration
public class ProxySelectorConfig extends ProxySelector implements InitializingBean {

    public static final String KEY = "global_proxy";

    private final SystemParametersServer systemParametersServer;
    private volatile List<ProxyConfigItem> proxyConfigItems;
    private static ProxySelector defaultProxySelector;

    public ProxySelectorConfig(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    @Override
    public List<Proxy> select(URI uri) {
        String url = uri.toString();
        return Optional.ofNullable(proxyConfigItems)
            .flatMap(proxyConfigItems -> proxyConfigItems.stream()
                .filter(proxyConfigItem -> {
                    if (StrUtil.equals(proxyConfigItem.getPattern(), "*")) {
                        return true;
                    }
                    if (ReUtil.isMatch(proxyConfigItem.getPattern(), url)) {
                        // 满足正则条件
                        return true;
                    }
                    return StrUtil.containsIgnoreCase(url, proxyConfigItem.getPattern());
                })
                .map(proxyConfigItem -> NodeModel.crateProxy(proxyConfigItem.getProxyType(), proxyConfigItem.getProxyAddress()))
                .filter(Objects::nonNull)
                .findFirst()
                .map(Collections::singletonList)
            ).orElseGet(() -> {
                // revert to the default behaviour
                return defaultProxySelector == null ? Collections.singletonList(Proxy.NO_PROXY) : defaultProxySelector.select(uri);
            });
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (uri == null || sa == null || ioe == null) {
            throw new IllegalArgumentException(
                "Arguments can't be null.");
        }
    }

    /**
     * 刷新
     */
    public void refresh() {
        JSONArray array = systemParametersServer.getConfigDefNewInstance(ProxySelectorConfig.KEY, JSONArray.class);
        proxyConfigItems = array.toJavaList(ProxyConfigItem.class)
            .stream()
            .filter(proxyConfigItem -> StrUtil.isAllNotEmpty(proxyConfigItem.pattern, proxyConfigItem.proxyAddress, proxyConfigItem.proxyType))
            .collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        defaultProxySelector = ProxySelector.getDefault();
        //
        ProxySelector.setDefault(this);
        this.refresh();
    }


    /**
     * @author bwcx_jzy
     * @since 2022/7/4
     */
    @Data
    public static class ProxyConfigItem {

        private String pattern;

        /**
         * @see Proxy.Type
         */
        private String proxyType;

        /**
         * 127.0.0.1:8888
         */
        private String proxyAddress;
    }

}
