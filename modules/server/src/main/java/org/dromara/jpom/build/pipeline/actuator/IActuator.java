package org.dromara.jpom.build.pipeline.actuator;

import org.dromara.jpom.build.pipeline.config.IStage;

import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public interface IActuator<T extends IStage> extends AutoCloseable {

    T stage();

    List<IActuator<?>> beforeChain();

    /**
     * 开始执行流程
     *
     * @throws Exception 异常
     */
    void run() throws Exception;

    void info(CharSequence template, Object... params);
}
