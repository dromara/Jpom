/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.system.init;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.event.ICacheTask;
import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.common.ILoadEvent;
import org.dromara.jpom.common.forward.NodeForward;
import org.dromara.jpom.service.system.SystemParametersServer;
import org.springframework.context.ApplicationContext;
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
public class ProxySelectorConfig extends ProxySelector implements ILoadEvent, ICacheTask {

    public static final String KEY = "global_proxy";

    private final SystemParametersServer systemParametersServer;
    private volatile List<ProxyConfigItem> proxyConfigItems;
    private ProxySelector defaultProxySelector;

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
                        .map(proxyConfigItem -> NodeForward.crateProxy(proxyConfigItem.getProxyType(), proxyConfigItem.getProxyAddress()))
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
    @Override
    public void refreshCache() {
        JSONArray array = systemParametersServer.getConfigDefNewInstance(ProxySelectorConfig.KEY, JSONArray.class);
        proxyConfigItems = array.toJavaList(ProxyConfigItem.class)
                .stream()
                .filter(proxyConfigItem -> StrUtil.isAllNotEmpty(proxyConfigItem.pattern, proxyConfigItem.proxyAddress, proxyConfigItem.proxyType))
                .collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        if (ProxySelector.getDefault() != this) {
            defaultProxySelector = ProxySelector.getDefault();
            //
            ProxySelector.setDefault(this);
        }
        // 立马配置 全局代理
        this.refreshCache();
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
