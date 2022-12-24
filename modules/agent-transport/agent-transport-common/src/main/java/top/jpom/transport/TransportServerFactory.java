package top.jpom.transport;

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
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    public static TransportServer get() {
        return Singleton.get(TransportServer.class.getName(), TransportServerFactory::doCreate);
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
