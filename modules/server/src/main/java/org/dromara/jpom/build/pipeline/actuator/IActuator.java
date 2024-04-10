package org.dromara.jpom.build.pipeline.actuator;

import java.io.IOException;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public interface IActuator {

    List<IActuator> beforeChain();

    void run() throws IOException;

    void info(CharSequence template, Object... params);
}
