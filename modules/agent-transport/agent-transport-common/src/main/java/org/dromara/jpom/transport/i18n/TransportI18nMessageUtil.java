/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.transport.i18n;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ServiceLoaderUtil;

/**
 * @author bwcx_jzy1
 * @since 2024/6/11
 */
public class TransportI18nMessageUtil {

    /**
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    public static String get(String key) {
        return Singleton.get(II18nMessageUtil.class.getName(), TransportI18nMessageUtil::doCreate).get(key);
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static II18nMessageUtil doCreate() {
        final II18nMessageUtil engine = ServiceLoaderUtil.loadFirstAvailable(II18nMessageUtil.class);
        if (null != engine) {
            return engine;
        }

        throw new RuntimeException("No jpom IMessageUtil jar found ! Please add one of it to your project !");
    }
}
