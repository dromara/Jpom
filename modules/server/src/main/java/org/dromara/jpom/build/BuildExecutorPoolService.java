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
package org.dromara.jpom.build;

import cn.hutool.core.thread.ExecutorBuilder;
import lombok.extern.slf4j.Slf4j;
import org.dromara.jpom.JpomApplication;
import org.dromara.jpom.configuration.BuildExtConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class BuildExecutorPoolService {
    /**
     * 构建线程池
     */
    private volatile ThreadPoolExecutor threadPoolExecutor;
    private final BuildExtConfig buildExtConfig;

    public BuildExecutorPoolService(BuildExtConfig buildExtConfig) {
        this.buildExtConfig = buildExtConfig;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        this.initPool();
        return threadPoolExecutor;
    }

    public void execute(Runnable command) {
        this.initPool();
        threadPoolExecutor.execute(command);
    }

    /**
     * 创建构建线程池
     */
    private void initPool() {
        if (threadPoolExecutor == null) {
            synchronized (BuildExecutorPoolService.class) {
                if (threadPoolExecutor == null) {
                    ExecutorBuilder executorBuilder = ExecutorBuilder.create();
                    int poolSize = buildExtConfig.getPoolSize();
                    if (poolSize > 0) {
                        executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
                    }
                    executorBuilder.useArrayBlockingQueue(Math.max(buildExtConfig.getPoolWaitQueue(), 1));
                    executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            if (r instanceof BuildExecuteManage) {
                                // 取消任务
                                BuildExecuteManage buildExecuteManage = (BuildExecuteManage) r;
                                buildExecuteManage.rejectedExecution();
                            } else {
                                log.warn("构建线程池拒绝了未知任务：{}", r.getClass());
                            }
                        }
                    });
                    threadPoolExecutor = executorBuilder.build();
                    JpomApplication.register("build", threadPoolExecutor);
                }
            }
        }
    }
}
