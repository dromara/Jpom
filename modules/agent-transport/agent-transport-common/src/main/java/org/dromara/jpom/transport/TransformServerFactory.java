/*
 * Copyright (c) 2019 Code Technology Studio
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
 * @since 2022/12/24
 */
@Slf4j
public class TransformServerFactory {

    /**
     * 获得单例的 TransformServer
     *
     * @return 单例的 TransformServer
     */
    public static TransformServer get() {
        return Singleton.get(TransformServer.class.getName(), TransformServerFactory::doCreate);
    }

    /**
     * 根据用户引入的 Transform 客户端引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code TransformServer}
     */
    public static TransformServer of() {
        final TransformServer transportServer = doCreate();
        log.debug("Use [{}] Agent Transport As Default.", transportServer.getClass().getSimpleName());
        return transportServer;
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static TransformServer doCreate() {
        final TransformServer engine = ServiceLoaderUtil.loadFirstAvailable(TransformServer.class);
        if (null != engine) {
            return engine;
        }

        throw new RuntimeException("No jpom agent transform jar found ! Please add one of it to your project !");
    }
}
