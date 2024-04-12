package org.dromara.jpom.build.pipeline.actuator;

import cn.hutool.core.util.IdUtil;
import org.dromara.jpom.build.pipeline.config.BaseStage;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author bwcx_jzy
 * @since 2024/4/10
 */
public abstract class BaseActuator<T extends BaseStage> implements IActuator<T>, AutoCloseable {
    /**
     * 流程对象
     */
    protected T stage;
    /**
     * 流程信息
     */
    protected StageActuatorBaseInfo info;
    /**
     * 是否为调试模式
     */
    protected boolean debug;
    /**
     * 数据目录
     */
    protected File dataDir;
    /**
     * 执行器 id
     */
    protected final String actuatorId;


    public BaseActuator() {
        this.actuatorId = IdUtil.fastSimpleUUID();
    }

    @Override
    public T stage() {
        return stage;
    }

    @Override
    public List<IActuator<?>> beforeChain() {
        return Collections.emptyList();
    }

    @Override
    public void info(CharSequence template, Object... params) {

    }

    @Override
    public void close() throws Exception {

    }
}
