package org.dromara.jpom.build.pipeline.actuator;

import org.dromara.jpom.build.pipeline.model.config.IStage;

import java.io.IOException;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public interface IActuator<T extends IStage> extends AutoCloseable {

    T stage();

    List<IActuator<?>> beforeChain();

    void run() throws IOException;

    void info(CharSequence template, Object... params);
}
