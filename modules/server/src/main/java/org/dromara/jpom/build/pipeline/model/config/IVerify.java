package org.dromara.jpom.build.pipeline.model.config;

/**
 * @author bwcx_jzy
 * @since 2024/4/8
 */
public interface IVerify<T> {

    /**
     * 验证
     *
     * @param prefix 消息前缀
     * @return 返回自身
     */
    T verify(String prefix);
}
