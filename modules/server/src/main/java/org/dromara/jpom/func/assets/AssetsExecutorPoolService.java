/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.dromara.jpom.func.assets;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
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
                            log.warn("资产监控线程池拒绝了任务：{}", r.getClass());
                        }
                    });
                    threadPoolExecutor = executorBuilder.build();
                    JpomApplication.register("assets-monitor", threadPoolExecutor);
                }
            }
        }
    }
}
