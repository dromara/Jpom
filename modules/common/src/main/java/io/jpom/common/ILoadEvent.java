package io.jpom.common;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

/**
 * jpom 加载事件
 * <p>
 * 保证在容器的 bean 加载完成之后
 *
 * @author bwcx_jzy
 * @since 2022/12/25
 */
public interface ILoadEvent extends Ordered {

    /**
     * 初始化成功后执行
     *
     * @param applicationContext 应用上下文
     * @throws Exception 异常
     */
    void afterPropertiesSet(ApplicationContext applicationContext) throws Exception;

    /**
     * 排序只
     *
     * @return 0 是默认
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
