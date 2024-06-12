package org.dromara.jpom.transport.i18n;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.ServiceLoaderUtil;

/**
 * @author bwcx_jzy1
 * @since 2024/6/11
 */
public class TransportMessageUtil {

    /**
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    public static String get(String key) {
        return Singleton.get(IMessageUtil.class.getName(), TransportMessageUtil::doCreate).get(key);
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static IMessageUtil doCreate() {
        final IMessageUtil engine = ServiceLoaderUtil.loadFirstAvailable(IMessageUtil.class);
        if (null != engine) {
            return engine;
        }

        throw new RuntimeException("No jpom IMessageUtil jar found ! Please add one of it to your project !");
    }
}
