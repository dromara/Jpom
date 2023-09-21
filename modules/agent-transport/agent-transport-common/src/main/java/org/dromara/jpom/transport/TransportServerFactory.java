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
package org.dromara.jpom.transport;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ServiceLoaderUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bwcx_jzy
 * @since 2022/12/23
 */
@Slf4j
public class TransportServerFactory {

    private static final Map<Integer, TransportServer> MAP = new ConcurrentHashMap<>();

    /**
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    @Deprecated
    public static TransportServer get() {
        return Singleton.get(TransportServer.class.getName(), TransportServerFactory.doCreate(null));
    }

    /**
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    public static synchronized TransportServer get(INodeInfo nodeInfo) {
        if (MAP.containsKey(nodeInfo.transportMode())) {
            return MAP.get(nodeInfo.transportMode());
        }
        TransportServer transportServer = TransportServerFactory.doCreate(nodeInfo);
        return MAP.put(nodeInfo.transportMode(), transportServer);
    }

    /**
     * 根据用户引入的 Transport 客户端引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code TransportServer}
     */
    public static TransportServer of() {
        final TransportServer transportServer = doCreate(null);
        log.debug("Use [{}] Agent Transport As Default.", transportServer.getClass().getSimpleName());
        return transportServer;
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static TransportServer doCreate(INodeInfo nodeInfo) {
        /*final TransportServer engine = ServiceLoaderUtil.loadFirstAvailable(TransportServer.class);
        if (null != engine) {
            return engine;
        }*/
        List<TransportServer> list = ServiceLoaderUtil.loadList(TransportServer.class);
        if (!list.isEmpty()) {
            Optional<TransportServer> optional = list.stream().filter(it -> it.support(nodeInfo.transportMode())).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }

        throw new RuntimeException("No jpom agent transport jar found ! Please add one of it to your project !");
    }
}
