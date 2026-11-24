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

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ServiceLoaderUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author bwcx_jzy
 * @since 2022/12/23
 */
@Slf4j
public class TransportServerFactory {

    /**
     * 由业务层注册的传输实现（反向建连隧道用）
     * <p>
     * SPI（{@code META-INF/services}）只能加载一个全局实现，无法按节点区分正反向传输，
     * 因此允许服务端在启动时注册一个路由门面：门面内部按节点判断走隧道还是走 SPI 实现。
     */
    private static volatile TransportServer delegate;

    /**
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    public static TransportServer get() {
        TransportServer transportServer = delegate;
        if (transportServer != null) {
            return transportServer;
        }
        return Singleton.get(TransportServer.class.getName(), TransportServerFactory::doCreate);
    }

    /**
     * 注册传输实现（通常为按节点路由的门面）
     *
     * @param transportServer 传输实现，传 {@code null} 恢复 SPI 默认实现
     */
    public static void set(TransportServer transportServer) {
        delegate = transportServer;
    }

    /**
     * 根据用户引入的 Transport 客户端引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code TransportServer}
     */
    public static TransportServer of() {
        final TransportServer transportServer = doCreate();
        log.debug("Use [{}] Agent Transport As Default.", transportServer.getClass().getSimpleName());
        return transportServer;
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static TransportServer doCreate() {
        final TransportServer engine = ServiceLoaderUtil.loadFirstAvailable(TransportServer.class);
        if (null != engine) {
            return engine;
        }

        throw new RuntimeException("No jpom agent transport jar found ! Please add one of it to your project !");
    }
}
