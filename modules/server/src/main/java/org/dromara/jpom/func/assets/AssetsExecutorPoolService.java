/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom.func.assets;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.common.i18n.I18nMessageUtil;
import org.dromara.jpom.configuration.AssetsConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class AssetsExecutorPoolService {
    /**
     * 监控线程池
     */
    private volatile ThreadPoolExecutor threadPoolExecutor;

    private final AssetsConfig assetsConfig;

    public AssetsExecutorPoolService(AssetsConfig assetsConfig) {
        this.assetsConfig = assetsConfig;
    }

    public void execute(Runnable command) {
        this.createPool();
        threadPoolExecutor.execute(command);
    }

    private void createPool() {
        if (threadPoolExecutor == null) {
            synchronized (AssetsExecutorPoolService.class) {
                if (threadPoolExecutor == null) {
                    ExecutorBuilder executorBuilder = ExecutorBuilder.create();
                    int poolSize = assetsConfig.getMonitorPoolSize();
                    if (poolSize <= 0) {
                        // 获取 CPU 核心数
                        poolSize = RuntimeUtil.getProcessorCount();
                    }
                    executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
                    executorBuilder.useArrayBlockingQueue(Math.max(assetsConfig.getMonitorPoolWaitQueue(), 1));
                    executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            log.warn(I18nMessageUtil.get("i18n.asset_monitoring_thread_pool_rejected_task.222e"), r.getClass());
                        }
                    });
                    threadPoolExecutor = executorBuilder.build();
                    JpomApplication.register("assets-monitor", threadPoolExecutor);
                }
            }
        }
    }
}
