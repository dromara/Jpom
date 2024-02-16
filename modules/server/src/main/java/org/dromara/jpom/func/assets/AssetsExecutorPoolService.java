package org.dromara.jpom.func.assets;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.configuration.AssetsConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class AssetsExecutorPoolService implements InitializingBean {
    /**
     * 监控线程池
     */
    private static ThreadPoolExecutor threadPoolExecutor;

    private final AssetsConfig assetsConfig;

    public AssetsExecutorPoolService(AssetsConfig assetsConfig) {
        this.assetsConfig = assetsConfig;
    }

    public void execute(Runnable command) {
        threadPoolExecutor.execute(command);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (threadPoolExecutor != null) {
            return;
        }
        ExecutorBuilder executorBuilder = ExecutorBuilder.create();
        int poolSize = assetsConfig.getMonitorPoolSize();
        if (poolSize > 0) {
            // 获取 CPU 核心数
            poolSize = RuntimeUtil.getProcessorCount();
        }
        executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
        executorBuilder.useArrayBlockingQueue(Math.max(assetsConfig.getMonitorPoolWaitQueue(), 1));
        executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                log.warn("资产监控线程池拒绝了任务：{}", r.getClass());
            }
        });
        threadPoolExecutor = executorBuilder.build();
        JpomApplication.register("assets-monitor", threadPoolExecutor);
    }
}
