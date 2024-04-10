package org.dromara.jpom.build.pipeline.actuator;

import org.dromara.jpom.build.pipeline.model.config.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public abstract class BaseActuator implements IActuator, AutoCloseable {

    private final Map<String, Repository> repositoryMap;
    private final int[] position = new int[]{0, 1};
    private final String description = "";

    protected BaseActuator(Map<String, Repository> repositoryMap) {
        this.repositoryMap = repositoryMap;
    }

    @Override
    public List<IActuator> beforeChain() {
        return Collections.emptyList();
    }

    @Override
    public void info(CharSequence template, Object... params) {

    }

    @Override
    public void close() throws Exception {

    }
}
